package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.query.QueryEngineExecutor;
import com.dataintensive.lab.query.QueryEngineRegistry;
import com.dataintensive.lab.query.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseCatalogInspectorTest {

    private QueryEngineRegistry mockEngineRegistry;
    private QueryEngineExecutor mockPostgresExecutor;
    private QueryEngineExecutor mockNeo4jExecutor;
    private DatabaseCatalogInspector inspector;

    @BeforeEach
    void setUp() {
        mockEngineRegistry = mock(QueryEngineRegistry.class);
        mockPostgresExecutor = mock(QueryEngineExecutor.class);
        mockNeo4jExecutor = mock(QueryEngineExecutor.class);

        when(mockEngineRegistry.getExecutor(EngineType.POSTGRES)).thenReturn(mockPostgresExecutor);
        when(mockEngineRegistry.getExecutor(EngineType.NEO4J)).thenReturn(mockNeo4jExecutor);

        inspector = new DefaultDatabaseCatalogInspector(mockEngineRegistry);
    }

    @Test
    @DisplayName("tableExists deve retornar true quando information_schema confirmar existência da tabela")
    void shouldReturnTrueWhenTableExistsInCatalog() {
        when(mockPostgresExecutor.execute(contains("information_schema.tables"), anyLong()))
                .thenReturn(QueryResult.ok(List.of("cnt"), List.of(Map.of("cnt", 1L)), 5));

        boolean exists = inspector.tableExists(EngineType.POSTGRES, "usuarios");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("tableExists deve retornar false quando tabela não existir ou contagem for zero")
    void shouldReturnFalseWhenTableDoesNotExist() {
        when(mockPostgresExecutor.execute(contains("information_schema.tables"), anyLong()))
                .thenReturn(QueryResult.ok(List.of("cnt"), List.of(Map.of("cnt", 0L)), 5));

        boolean exists = inspector.tableExists(EngineType.POSTGRES, "tabela_inexistente");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("columnExists deve validar existência e tipo esperado da coluna (ex: JSONB)")
    void shouldValidateColumnExistenceAndType() {
        when(mockPostgresExecutor.execute(contains("information_schema.columns"), anyLong()))
                .thenReturn(QueryResult.ok(List.of("udt_name"), List.of(Map.of("udt_name", "jsonb")), 5));

        boolean hasJsonbColumn = inspector.columnExists(EngineType.POSTGRES, "usuarios_documento", "perfil", "jsonb");

        assertThat(hasJsonbColumn).isTrue();
    }

    @Test
    @DisplayName("foreignKeyExists deve retornar true quando constraint referencial for detectada")
    void shouldReturnTrueWhenForeignKeyExists() {
        when(mockPostgresExecutor.execute(contains("table_constraints"), anyLong()))
                .thenReturn(QueryResult.ok(List.of("cnt"), List.of(Map.of("cnt", 1L)), 5));

        boolean fkExists = inspector.foreignKeyExists(EngineType.POSTGRES, "experiencias_profissionais", "usuarios");

        assertThat(fkExists).isTrue();
    }

    @Test
    @DisplayName("getRowCount deve retornar número real de linhas da tabela")
    void shouldReturnActualRowCount() {
        when(mockPostgresExecutor.execute(contains("SELECT COUNT(*)"), anyLong()))
                .thenReturn(QueryResult.ok(List.of("cnt"), List.of(Map.of("cnt", 3L)), 5));

        long count = inspector.getRowCount(EngineType.POSTGRES, "usuarios");

        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("viewExists deve verificar se view padrão ou materializada existe")
    void shouldVerifyViewExistence() {
        when(mockPostgresExecutor.execute(contains("pg_matviews"), anyLong()))
                .thenReturn(QueryResult.ok(List.of("cnt"), List.of(Map.of("cnt", 1L)), 5));

        boolean viewExists = inspector.viewExists(EngineType.POSTGRES, "pedidos_resumo_leitura");

        assertThat(viewExists).isTrue();
    }

    @Test
    @DisplayName("countNeo4jNodes deve consultar quantidade de nós por rótulo no Neo4j")
    void shouldCountNeo4jNodesByLabel() {
        when(mockNeo4jExecutor.execute(contains("MATCH (n:Person)"), anyLong()))
                .thenReturn(QueryResult.ok(List.of("cnt"), List.of(Map.of("cnt", 4L)), 5));

        long nodeCount = inspector.countNeo4jNodes("Person");

        assertThat(nodeCount).isEqualTo(4L);
    }

    @Test
    @DisplayName("countNeo4jRelationships deve consultar quantidade de arestas por tipo no Neo4j")
    void shouldCountNeo4jRelationshipsByType() {
        when(mockNeo4jExecutor.execute(contains("MATCH ()-[r:WITHIN]->()"), anyLong()))
                .thenReturn(QueryResult.ok(List.of("cnt"), List.of(Map.of("cnt", 2L)), 5));

        long relCount = inspector.countNeo4jRelationships("WITHIN");

        assertThat(relCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("executeVerificationQuery deve executar query via executor correspondente")
    void shouldExecuteVerificationQuerySuccessfully() {
        when(mockPostgresExecutor.execute(eq("SELECT 1"), anyLong()))
                .thenReturn(QueryResult.ok(List.of("val"), List.of(Map.of("val", 1)), 2));

        QueryResult result = inspector.executeVerificationQuery(EngineType.POSTGRES, "SELECT 1");

        assertThat(result.success()).isTrue();
        assertThat(result.rows()).hasSize(1);
    }

    @Test
    @DisplayName("Deve ser resiliente e retornar false/zero quando o motor lançar erro ou consulta falhar")
    void shouldHandleExceptionsGracefullyWithoutThrowing() {
        when(mockPostgresExecutor.execute(anyString(), anyLong()))
                .thenThrow(new RuntimeException("Connection refused"));
        when(mockNeo4jExecutor.execute(anyString(), anyLong()))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThat(inspector.tableExists(EngineType.POSTGRES, "usuarios")).isFalse();
        assertThat(inspector.getRowCount(EngineType.POSTGRES, "usuarios")).isEqualTo(0L);
        assertThat(inspector.countNeo4jNodes("Person")).isEqualTo(0L);
        assertThat(inspector.executeVerificationQuery(EngineType.POSTGRES, "SELECT 1").success()).isFalse();
    }
}
