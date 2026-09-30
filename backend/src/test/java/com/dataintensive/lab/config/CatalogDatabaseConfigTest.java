package com.dataintensive.lab.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
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
    @DisplayName("Deve validar que o application.yml declara todas as variáveis de ambiente com defaults usando ':'")
    void shouldDeclareEnvironmentVariablesWithDefaultsInApplicationYml() throws IOException {
        Path ymlPath = Path.of("src", "main", "resources", "application.yml");
        if (!Files.exists(ymlPath)) {
            ymlPath = Path.of("backend", "src", "main", "resources", "application.yml");
        }
        String content = Files.readString(ymlPath);

        // Valida declaração com sintaxe ${VAR:default}
        assertThat(content).contains("${CATALOG_DB_URL:jdbc:sqlite:data/tbl_catalog.db}");
        assertThat(content).contains("${CATALOG_DB_DRIVER:org.sqlite.JDBC}");
        assertThat(content).contains("${CATALOG_DB_POOL_NAME:TblCatalogHikariPool}");
        assertThat(content).contains("${CATALOG_DB_MAX_POOL_SIZE:10}");
        assertThat(content).contains("${CATALOG_DB_MIN_IDLE:2}");
        assertThat(content).contains("${CATALOG_DB_CONNECTION_TIMEOUT:10000}");
        assertThat(content).contains("${CATALOG_DB_IDLE_TIMEOUT:60000}");
        assertThat(content).contains("${CATALOG_DB_MAX_LIFETIME:1800000}");
        assertThat(content).contains("${CATALOG_DB_JOURNAL_MODE:WAL}");
        assertThat(content).contains("${CATALOG_DB_SYNCHRONOUS:NORMAL}");
        assertThat(content).contains("${CATALOG_DB_BUSY_TIMEOUT:5000}");
        assertThat(content).contains("${CATALOG_DB_FOREIGN_KEYS:true}");
    }

    @Test
    @DisplayName("Deve configurar os parâmetros otimizados do pool HikariCP a partir dos defaults")
    void shouldConfigureHikariPoolParameters() {
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("test-pool.db").toAbsolutePath();
        HikariConfig config = SqliteTestDataSourceFactory.createConfig(dbUrl);

        assertThat(config.getPoolName()).isEqualTo(SqliteTestDataSourceFactory.DEFAULT_POOL_NAME);
        assertThat(config.getMaximumPoolSize()).isEqualTo(SqliteTestDataSourceFactory.DEFAULT_MAX_POOL_SIZE);
        assertThat(config.getMinimumIdle()).isEqualTo(SqliteTestDataSourceFactory.DEFAULT_MIN_IDLE);
        assertThat(config.getConnectionTimeout()).isEqualTo(SqliteTestDataSourceFactory.DEFAULT_CONNECTION_TIMEOUT);
        assertThat(config.getIdleTimeout()).isEqualTo(SqliteTestDataSourceFactory.DEFAULT_IDLE_TIMEOUT);
        assertThat(config.getMaxLifetime()).isEqualTo(SqliteTestDataSourceFactory.DEFAULT_MAX_LIFETIME);
    }

    @Test
    @DisplayName("Deve ativar os pragmas essenciais no SQLite (WAL, NORMAL, busy_timeout, foreign_keys)")
    void shouldConfigureSqlitePragmasOnDataSourceConnection() throws SQLException {
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("test-pragmas.db").toAbsolutePath();
        try (HikariDataSource dataSource = SqliteTestDataSourceFactory.createDataSource(dbUrl);
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
        try (HikariDataSource dataSource = SqliteTestDataSourceFactory.createDataSource(dbUrl);
             Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("CREATE TABLE parent (id INTEGER PRIMARY KEY);");
            statement.execute("CREATE TABLE child (id INTEGER PRIMARY KEY, parent_id INTEGER REFERENCES parent(id));");

            assertThatThrownBy(() -> statement.execute("INSERT INTO child (id, parent_id) VALUES (1, 999);"))
                    .isInstanceOf(SQLException.class);
        }
    }
}
