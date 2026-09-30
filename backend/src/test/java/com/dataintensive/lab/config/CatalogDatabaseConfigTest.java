package com.dataintensive.lab.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogDatabaseConfigTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve configurar os parâmetros otimizados do pool HikariCP para SQLite")
    void shouldConfigureHikariPoolParameters() {
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("test-pool.db").toAbsolutePath();
        HikariConfig config = CatalogDatabaseConfig.createSqliteHikariConfig(dbUrl);

        assertThat(config.getPoolName()).isEqualTo(CatalogDatabaseConfig.DEFAULT_POOL_NAME);
        assertThat(config.getMaximumPoolSize()).isEqualTo(CatalogDatabaseConfig.DEFAULT_MAX_POOL_SIZE);
        assertThat(config.getMinimumIdle()).isEqualTo(CatalogDatabaseConfig.DEFAULT_MIN_IDLE);
        assertThat(config.getConnectionTimeout()).isEqualTo(CatalogDatabaseConfig.DEFAULT_CONNECTION_TIMEOUT_MS);
        assertThat(config.getIdleTimeout()).isEqualTo(CatalogDatabaseConfig.DEFAULT_IDLE_TIMEOUT_MS);
        assertThat(config.getMaxLifetime()).isEqualTo(CatalogDatabaseConfig.DEFAULT_MAX_LIFETIME_MS);
    }

    @Test
    @DisplayName("Deve ativar os pragmas essenciais no SQLite (WAL, NORMAL, busy_timeout, foreign_keys)")
    void shouldConfigureSqlitePragmasOnDataSourceConnection() throws SQLException {
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("test-pragmas.db").toAbsolutePath();
        try (HikariDataSource dataSource = CatalogDatabaseConfig.createSqliteDataSource(dbUrl);
             Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            try (ResultSet rs = statement.executeQuery("PRAGMA journal_mode;")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualToIgnoringCase("wal");
            }

            try (ResultSet rs = statement.executeQuery("PRAGMA synchronous;")) {
                assertThat(rs.next()).isTrue();
                // 1 corresponde ao modo NORMAL no SQLite
                assertThat(rs.getInt(1)).isEqualTo(1);
            }

            try (ResultSet rs = statement.executeQuery("PRAGMA busy_timeout;")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(5000);
            }

            try (ResultSet rs = statement.executeQuery("PRAGMA foreign_keys;")) {
                assertThat(rs.next()).isTrue();
                // 1 corresponde a ON
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    @DisplayName("Deve respeitar a integridade referencial quando foreign_keys estiver ativado")
    void shouldEnforceForeignKeysWhenInsertingInvalidData() throws SQLException {
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("test-fk.db").toAbsolutePath();
        try (HikariDataSource dataSource = CatalogDatabaseConfig.createSqliteDataSource(dbUrl);
             Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("CREATE TABLE parent (id INTEGER PRIMARY KEY);");
            statement.execute("CREATE TABLE child (id INTEGER PRIMARY KEY, parent_id INTEGER REFERENCES parent(id));");

            assertThatThrownBy(() -> statement.execute("INSERT INTO child (id, parent_id) VALUES (1, 999);"))
                    .isInstanceOf(SQLException.class);
        }
    }
}
