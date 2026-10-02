package com.dataintensive.lab.query;

import com.dataintensive.lab.catalog.CatalogRepository;
import com.dataintensive.lab.catalog.CatalogService;
import com.dataintensive.lab.domain.EngineType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class QueryExecutionServiceTest {

    private QueryExecutionService queryExecutionService;
    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        CatalogRepository catalogRepository = mock(CatalogRepository.class);
        catalogService = new CatalogService(catalogRepository);
        queryExecutionService = new QueryExecutionService(
                "jdbc:postgresql://localhost:5432/tbl_lab",
                "postgres",
                "postgrespassword",
                "bolt://localhost:7687",
                "neo4j",
                "tblpassword",
                catalogService
        );
    }

    @Test
    @DisplayName("Deve rejeitar consulta vazia ou composta apenas por espaços")
    void shouldRejectEmptyQuery() {
        QueryRequest request = new QueryRequest("   ", EngineType.POSTGRES, "ddia-cap-03-lab-01");

        QueryResult result = queryExecutionService.execute(request);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).containsIgnoringCase("consulta fornecida está vazia");
    }

    @Test
    @DisplayName("Deve rejeitar consulta com valor nulo")
    void shouldRejectNullQuery() {
        QueryRequest request = new QueryRequest(null, EngineType.POSTGRES, "ddia-cap-03-lab-01");

        QueryResult result = queryExecutionService.execute(request);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).containsIgnoringCase("consulta fornecida está vazia");
    }

    @Test
    @DisplayName("Deve lançar DomainValidationException ao executar consulta em laboratório inexistente")
    void shouldThrowDomainValidationExceptionWhenLabDoesNotExist() {
        QueryRequest request = new QueryRequest("SELECT 1;", EngineType.POSTGRES, "lab-fantasma");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.dataintensive.lab.domain.DomainValidationException.class,
                () -> queryExecutionService.execute(request)
        );
    }

    @Test
    @DisplayName("Deve lançar DomainValidationException ao tentar resetar laboratório inexistente")
    void shouldThrowDomainValidationExceptionWhenResettingUnknownLab() {
        org.junit.jupiter.api.Assertions.assertThrows(
                com.dataintensive.lab.domain.DomainValidationException.class,
                () -> queryExecutionService.resetLab("lab-fantasma")
        );
    }

    @Test
    @DisplayName("Deve executar reset de schema no Postgres e scripts do laboratório")
    void shouldExecutePostgresSchemaResetAndLabResetSqlWhenResettingPostgresDatabase() {
        QueryEngineExecutor pgExecutor = mock(QueryEngineExecutor.class);
        when(pgExecutor.getEngineType()).thenReturn(EngineType.POSTGRES);
        when(pgExecutor.execute(anyString(), anyLong())).thenReturn(QueryResult.ok(java.util.List.of(), java.util.List.of(), 10));

        QueryEngineRegistry registry = new QueryEngineRegistry(java.util.List.of(pgExecutor));
        QueryExecutionService service = new QueryExecutionService(registry, catalogService);

        com.dataintensive.lab.domain.Lab testLab = new com.dataintensive.lab.domain.Lab(
                "lab-1", 1, "slug", "Title", "Summary", java.util.List.of(), EngineType.POSTGRES, "tbl_lab", "DROP TABLE IF EXISTS custom_tbl CASCADE;", java.util.List.of()
        );

        service.resetDatabase(EngineType.POSTGRES, testLab);

        verify(pgExecutor).execute(contains("DROP SCHEMA IF EXISTS public CASCADE"), anyLong());
        verify(pgExecutor).execute(eq("DROP TABLE IF EXISTS custom_tbl CASCADE;"), anyLong());
    }

    @Test
    @DisplayName("Deve executar DETACH DELETE no Neo4j ao resetar banco do motor Neo4j")
    void shouldExecuteCypherDetachDeleteWhenResettingNeo4jDatabase() {
        QueryEngineExecutor neoExecutor = mock(QueryEngineExecutor.class);
        when(neoExecutor.getEngineType()).thenReturn(EngineType.NEO4J);
        when(neoExecutor.execute(anyString(), anyLong())).thenReturn(QueryResult.ok(java.util.List.of(), java.util.List.of(), 10));

        QueryEngineRegistry registry = new QueryEngineRegistry(java.util.List.of(neoExecutor));
        QueryExecutionService service = new QueryExecutionService(registry, catalogService);

        com.dataintensive.lab.domain.Lab testLab = new com.dataintensive.lab.domain.Lab(
                "lab-2", 2, "slug2", "Title2", "Summary2", java.util.List.of(), EngineType.NEO4J, "neo4j", null, java.util.List.of()
        );

        service.resetDatabase(EngineType.NEO4J, testLab);

        verify(neoExecutor).execute(eq("MATCH (n) DETACH DELETE n;"), anyLong());
    }
}
