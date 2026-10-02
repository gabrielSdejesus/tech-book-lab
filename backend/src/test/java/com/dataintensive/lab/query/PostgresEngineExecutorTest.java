package com.dataintensive.lab.query;

import com.dataintensive.lab.domain.EngineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresEngineExecutorTest {

    private static final String H2_URL = "jdbc:h2:mem:postgres_executor_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    private static final String H2_USER = "sa";
    private static final String H2_PASS = "";

    @Test
    @DisplayName("Deve retornar EngineType.POSTGRES")
    void shouldReturnEngineTypePostgres() {
        PostgresEngineExecutor executor = new PostgresEngineExecutor(H2_URL, H2_USER, H2_PASS);
        assertThat(executor.getEngineType()).isEqualTo(EngineType.POSTGRES);
    }

    @Test
    @DisplayName("Deve executar comandos DDL e DML retornando contagem de linhas afetadas")
    void shouldExecuteDdlAndDmlCommands() {
        PostgresEngineExecutor executor = new PostgresEngineExecutor(H2_URL, H2_USER, H2_PASS);
        long start = System.currentTimeMillis();

        // DDL: Create table
        QueryResult createResult = executor.execute("CREATE TABLE IF NOT EXISTS sample_items (id INT, label VARCHAR(50));", start);
        assertThat(createResult.success()).isTrue();
        assertThat(createResult.rowCount()).isEqualTo(0);

        // DML: Insert
        QueryResult insertResult = executor.execute("INSERT INTO sample_items VALUES (1, 'Item 1'), (2, 'Item 2');", start);
        assertThat(insertResult.success()).isTrue();
        assertThat(insertResult.rowCount()).isEqualTo(2);
        assertThat(insertResult.message()).contains("Linhas afetadas: 2");
    }

    @Test
    @DisplayName("Deve executar consulta SELECT retornando colunas e linhas formatadas")
    void shouldExecuteSelectQuery() {
        PostgresEngineExecutor executor = new PostgresEngineExecutor(H2_URL, H2_USER, H2_PASS);
        long start = System.currentTimeMillis();

        executor.execute("CREATE TABLE IF NOT EXISTS test_products (id INT, name VARCHAR(50));", start);
        executor.execute("DELETE FROM test_products;", start);
        executor.execute("INSERT INTO test_products VALUES (10, 'Kafka Book');", start);

        QueryResult selectResult = executor.execute("SELECT id, name FROM test_products ORDER BY id ASC;", start);

        assertThat(selectResult.success()).isTrue();
        assertThat(selectResult.columns()).hasSize(2);
        assertThat(selectResult.rows()).hasSize(1);
        assertThat(selectResult.rows().get(0)).containsValues(10, "Kafka Book");
    }

    @Test
    @DisplayName("Deve propagar RuntimeException com causa SQLException diante de SQL inválido")
    void shouldThrowRuntimeExceptionOnSqlError() {
        PostgresEngineExecutor executor = new PostgresEngineExecutor(H2_URL, H2_USER, H2_PASS);
        long start = System.currentTimeMillis();

        assertThatThrownBy(() -> executor.execute("SELECT * FROM non_existent_table_xyz;", start))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("Deve propagar RuntimeException com causa SQLException diante de conexão inválida")
    void shouldThrowRuntimeExceptionOnConnectionError() {
        PostgresEngineExecutor executor = new PostgresEngineExecutor("jdbc:invalid:url", "invalid", "invalid");
        long start = System.currentTimeMillis();

        assertThatThrownBy(() -> executor.execute("SELECT 1;", start))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(SQLException.class);
    }
}
