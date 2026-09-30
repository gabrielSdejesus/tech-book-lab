package com.dataintensive.lab.query;

import com.dataintensive.lab.catalog.CatalogRepository;
import com.dataintensive.lab.catalog.CatalogService;
import com.dataintensive.lab.domain.EngineType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
}
