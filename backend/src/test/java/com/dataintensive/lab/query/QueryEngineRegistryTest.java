package com.dataintensive.lab.query;

import com.dataintensive.lab.domain.DomainValidationException;
import com.dataintensive.lab.domain.EngineType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryEngineRegistryTest {

    private QueryEngineExecutor postgresExecutor;
    private QueryEngineExecutor neo4jExecutor;
    private QueryEngineRegistry registry;

    @BeforeEach
    void setUp() {
        postgresExecutor = mock(QueryEngineExecutor.class);
        when(postgresExecutor.getEngineType()).thenReturn(EngineType.POSTGRES);
        when(postgresExecutor.getEngineIdentifier()).thenReturn("POSTGRES");

        neo4jExecutor = mock(QueryEngineExecutor.class);
        when(neo4jExecutor.getEngineType()).thenReturn(EngineType.NEO4J);
        when(neo4jExecutor.getEngineIdentifier()).thenReturn("NEO4J");

        registry = new QueryEngineRegistry(List.of(postgresExecutor, neo4jExecutor));
    }

    @Test
    @DisplayName("Deve registrar e recuperar executor por EngineType")
    void shouldRegisterAndRetrieveExecutorByEngineType() {
        QueryEngineExecutor resolved = registry.getExecutor(EngineType.POSTGRES);
        assertThat(resolved).isSameAs(postgresExecutor);

        QueryEngineExecutor resolvedNeo4j = registry.getExecutor(EngineType.NEO4J);
        assertThat(resolvedNeo4j).isSameAs(neo4jExecutor);
    }

    @Test
    @DisplayName("Deve resolver executor por identificador String case-insensitive")
    void shouldRetrieveExecutorByStringIdentifier() {
        assertThat(registry.getExecutor("postgres")).isSameAs(postgresExecutor);
        assertThat(registry.getExecutor("POSTGRES")).isSameAs(postgresExecutor);
        assertThat(registry.getExecutor("neo4j")).isSameAs(neo4jExecutor);
        assertThat(registry.getExecutor("NEO4J")).isSameAs(neo4jExecutor);
    }

    @Test
    @DisplayName("Deve lançar DomainValidationException para EngineType nulo ou não registrado")
    void shouldThrowDomainValidationExceptionForNullOrUnregisteredEngineType() {
        assertThrows(DomainValidationException.class, () -> registry.getExecutor((EngineType) null));
    }

    @Test
    @DisplayName("Deve lançar DomainValidationException para identificador String desconhecido ou vazio")
    void shouldThrowDomainValidationExceptionForUnknownOrEmptyString() {
        assertThrows(DomainValidationException.class, () -> registry.getExecutor(""));
        assertThrows(DomainValidationException.class, () -> registry.getExecutor("   "));
        assertThrows(DomainValidationException.class, () -> registry.getExecutor((String) null));
        assertThrows(DomainValidationException.class, () -> registry.getExecutor("redis"));
    }

    @Test
    @DisplayName("Deve listar todos os motores de banco registrados")
    void shouldListAllRegisteredEngines() {
        List<EngineType> engines = registry.getSupportedEngines();
        assertThat(engines).containsExactlyInAnyOrder(EngineType.POSTGRES, EngineType.NEO4J);
    }
}
