package com.dataintensive.lab.query;

import com.dataintensive.lab.domain.EngineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.driver.types.Entity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Neo4jEngineExecutorTest {

    @Test
    @DisplayName("Deve retornar EngineType.NEO4J")
    void shouldReturnEngineTypeNeo4j() {
        Neo4jEngineExecutor executor = new Neo4jEngineExecutor(null);
        assertThat(executor.getEngineType()).isEqualTo(EngineType.NEO4J);
    }

    @Test
    @DisplayName("Deve retornar erro quando neo4jDriver for nulo")
    void shouldReturnErrorWhenDriverIsNull() {
        Neo4jEngineExecutor executor = new Neo4jEngineExecutor(null);
        long start = System.currentTimeMillis();

        QueryResult result = executor.execute("MATCH (n) RETURN n", start);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Driver do Neo4j não está disponível");
    }

    @Test
    @DisplayName("Deve executar consulta Cypher com nós e relacionamentos retornando dados sanitizados")
    void shouldExecuteCypherQueryWithEntities() {
        Driver mockDriver = mock(Driver.class);
        Session mockSession = mock(Session.class);
        Result mockResult = mock(Result.class);
        Record mockRecord = mock(Record.class);
        Value nameValue = mock(Value.class);
        Value entityValue = mock(Value.class);
        Entity mockEntity = mock(Entity.class);

        when(mockDriver.session()).thenReturn(mockSession);
        when(mockSession.executeWrite(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            TransactionContext txContext = mock(TransactionContext.class);
            when(txContext.run("MATCH (p:Person) RETURN p.name, p")).thenReturn(mockResult);
            return callback.execute(txContext);
        });

        when(mockResult.keys()).thenReturn(List.of("name", "entity"));
        when(mockResult.hasNext()).thenReturn(true, false);
        when(mockResult.next()).thenReturn(mockRecord);

        when(mockRecord.get("name")).thenReturn(nameValue);
        when(nameValue.asObject()).thenReturn("Martin");

        when(mockRecord.get("entity")).thenReturn(entityValue);
        when(entityValue.asObject()).thenReturn(mockEntity);
        when(mockEntity.asMap()).thenReturn(Map.of("name", "Martin", "role", "Author"));

        Neo4jEngineExecutor executor = new Neo4jEngineExecutor(mockDriver);
        long start = System.currentTimeMillis();

        QueryResult queryResult = executor.execute("MATCH (p:Person) RETURN p.name, p", start);

        assertThat(queryResult.success()).isTrue();
        assertThat(queryResult.columns()).containsExactly("name", "entity");
        assertThat(queryResult.rows()).hasSize(1);
        assertThat(queryResult.rows().get(0)).containsEntry("name", "Martin");
        assertThat(queryResult.rows().get(0).get("entity")).isEqualTo(Map.of("name", "Martin", "role", "Author"));
    }

    @Test
    @DisplayName("Deve executar comando de mutação sem retorno gerando contagem total de nós e relacionamentos")
    void shouldExecuteMutationQueryAndReturnUpdateCount() {
        Driver mockDriver = mock(Driver.class);
        Session mockSession = mock(Session.class);
        Result mockResult = mock(Result.class);
        ResultSummary mockSummary = mock(ResultSummary.class);
        SummaryCounters mockCounters = mock(SummaryCounters.class);

        when(mockDriver.session()).thenReturn(mockSession);
        when(mockSession.executeWrite(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            TransactionContext txContext = mock(TransactionContext.class);
            when(txContext.run("CREATE (a:User {name: 'Alice'})-[:FRIEND]->(b:User {name: 'Bob'})")).thenReturn(mockResult);
            return callback.execute(txContext);
        });

        when(mockResult.keys()).thenReturn(List.of());
        when(mockResult.hasNext()).thenReturn(false);
        when(mockResult.consume()).thenReturn(mockSummary);
        when(mockSummary.counters()).thenReturn(mockCounters);
        when(mockCounters.nodesCreated()).thenReturn(2);
        when(mockCounters.relationshipsCreated()).thenReturn(1);

        Neo4jEngineExecutor executor = new Neo4jEngineExecutor(mockDriver);
        long start = System.currentTimeMillis();

        QueryResult queryResult = executor.execute("CREATE (a:User {name: 'Alice'})-[:FRIEND]->(b:User {name: 'Bob'})", start);

        assertThat(queryResult.success()).isTrue();
        assertThat(queryResult.rowCount()).isEqualTo(3);
        assertThat(queryResult.message()).contains("Linhas afetadas: 3");
    }

    @Test
    @DisplayName("Deve fechar driver graciosamente no método close()")
    void shouldCloseDriverGracefully() {
        Driver mockDriver = mock(Driver.class);
        Neo4jEngineExecutor executor = new Neo4jEngineExecutor(mockDriver);

        executor.close();
        verify(mockDriver, times(1)).close();

        // Com exceção no close
        Driver throwingDriver = mock(Driver.class);
        doThrow(new RuntimeException("Falha ao fechar")).when(throwingDriver).close();
        Neo4jEngineExecutor throwingExecutor = new Neo4jEngineExecutor(throwingDriver);
        throwingExecutor.close();
        verify(throwingDriver, times(1)).close();

        // Com driver nulo
        Neo4jEngineExecutor nullExecutor = new Neo4jEngineExecutor(null);
        nullExecutor.close();
    }
}
