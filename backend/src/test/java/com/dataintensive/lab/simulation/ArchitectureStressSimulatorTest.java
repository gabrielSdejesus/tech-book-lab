package com.dataintensive.lab.simulation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureStressSimulatorTest {

    private ArchitectureStressSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new ArchitectureStressSimulator();
    }

    @Test
    @DisplayName("Deve penalizar latência e violar SLA em modelo 3NF com múltiplos JOINs sob leitura pesada sem cache")
    void shouldPenalizeLatencyAndFailSlaForNormalizedModelWithManyJoinsUnderReadHeavyLoad() {
        var nodeDb = new ArchitectureNode("db-1", ArchitectureNodeType.SQL_DATABASE, "PostgreSQL Master", Map.of("joinsCount", 4));
        var nodeApi = new ArchitectureNode("api-1", ArchitectureNodeType.API_GATEWAY, "Gateway API", Map.of());
        var edge = new ArchitectureEdge("api-1", "db-1", "READ");

        var model = new ArchitectureModel(
                List.of(nodeDb, nodeApi),
                List.of(edge),
                DataModelStrategy.NORMALIZED_3NF,
                "NONE",
                1,
                List.of("BTREE"),
                "NONE"
        );

        var workload = new StressWorkload(
                12000L, // 12k ops/s
                0.95,   // 95% leitura
                50_000_000_000L, // 50 GB
                30.0,   // SLA alvo p99 = 30ms
                50
        );

        StressSimulationResult result = simulator.simulate(model, workload);

        assertThat(result.slaMet()).isFalse();
        assertThat(result.estimatedLatencyP99Ms()).isGreaterThan(30.0);
        assertThat(result.violations()).anyMatch(v -> v.toLowerCase().contains("join") || v.toLowerCase().contains("latência"));
        assertThat(result.recommendations()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve reduzir latência p50/p99 e atender SLA ao introduzir Cache Redis em modelo com alta taxa de leitura")
    void shouldReduceLatencyAndMeetSlaWhenCacheRedisIsPresentForReadHeavyLoad() {
        var nodeDb = new ArchitectureNode("db-1", ArchitectureNodeType.SQL_DATABASE, "PostgreSQL Master", Map.of("joinsCount", 3));
        var nodeCache = new ArchitectureNode("cache-1", ArchitectureNodeType.CACHE_REDIS, "Redis Cache", Map.of());
        var nodeApi = new ArchitectureNode("api-1", ArchitectureNodeType.API_GATEWAY, "Gateway API", Map.of());

        var model = new ArchitectureModel(
                List.of(nodeDb, nodeCache, nodeApi),
                List.of(new ArchitectureEdge("api-1", "cache-1", "READ"), new ArchitectureEdge("cache-1", "db-1", "READ")),
                DataModelStrategy.NORMALIZED_3NF,
                "NONE",
                2,
                List.of("BTREE"),
                "CACHE_ASIDE"
        );

        var workload = new StressWorkload(
                12000L,
                0.95,
                50_000_000_000L,
                30.0,
                50
        );

        StressSimulationResult result = simulator.simulate(model, workload);

        assertThat(result.slaMet()).isTrue();
        assertThat(result.estimatedLatencyP50Ms()).isLessThan(5.0);
        assertThat(result.estimatedLatencyP99Ms()).isLessThanOrEqualTo(30.0);
        assertThat(result.violations()).isEmpty();
    }

    @Test
    @DisplayName("Deve disparar índice de contenção de lock e violar SLA em modelo desnormalizado sob escrita pesada concorrente sem particionamento")
    void shouldTriggerLockContentionAndFailSlaForDenormalizedModelUnderWriteHeavyLoadWithoutSharding() {
        var nodeDoc = new ArchitectureNode("doc-1", ArchitectureNodeType.DOCUMENT_STORE, "MongoDB / DocumentDB", Map.of());
        var nodeApi = new ArchitectureNode("api-1", ArchitectureNodeType.API_GATEWAY, "Gateway API", Map.of());

        var model = new ArchitectureModel(
                List.of(nodeDoc, nodeApi),
                List.of(new ArchitectureEdge("api-1", "doc-1", "WRITE")),
                DataModelStrategy.DENORMALIZED_DOCUMENT,
                "NONE", // sem partição
                1,
                List.of("BTREE"),
                "NONE"
        );

        var workload = new StressWorkload(
                8000L, // 8k ops/s
                0.20,  // 80% escrita
                10_000_000_000L,
                40.0,
                100
        );

        StressSimulationResult result = simulator.simulate(model, workload);

        assertThat(result.slaMet()).isFalse();
        assertThat(result.lockContentionIndex()).isGreaterThan(0.30);
        assertThat(result.violations()).anyMatch(v -> v.toLowerCase().contains("contenção") || v.toLowerCase().contains("concorrência") || v.toLowerCase().contains("lock"));
    }

    @Test
    @DisplayName("Deve distribuir carga e aumentar throughput ao aplicar particionamento adequado com réplicas de leitura")
    void shouldScaleThroughputWhenProperShardingAndReadReplicasAreConfigured() {
        var nodeDb = new ArchitectureNode("db-1", ArchitectureNodeType.SHARD_CLUSTER, "Sharded Cluster", Map.of("shardsCount", 4));
        var nodeReplica = new ArchitectureNode("replica-1", ArchitectureNodeType.READ_REPLICA, "Read Replica Pool", Map.of());

        var model = new ArchitectureModel(
                List.of(nodeDb, nodeReplica),
                List.of(new ArchitectureEdge("db-1", "replica-1", "ASYNC_REPLICATION")),
                DataModelStrategy.DENORMALIZED_DOCUMENT,
                "user_id_hash", // Partição de alta cardinalidade
                3,
                List.of("BTREE"),
                "NONE"
        );

        var workload = new StressWorkload(
                20000L,
                0.80,
                100_000_000_000L,
                50.0,
                150
        );

        StressSimulationResult result = simulator.simulate(model, workload);

        assertThat(result.estimatedThroughputOps()).isGreaterThanOrEqualTo(20000L);
        assertThat(result.lockContentionIndex()).isLessThan(0.15);
        assertThat(result.slaMet()).isTrue();
    }

    @Test
    @DisplayName("Deve acusar violação de sequential scan quando a estratégia de indexação for NONE em dataset volumoso")
    void shouldFlagSequentialScanViolationWhenNoIndexIsConfiguredOnLargeDataset() {
        var nodeDb = new ArchitectureNode("db-1", ArchitectureNodeType.SQL_DATABASE, "PostgreSQL Single", Map.of());

        var model = new ArchitectureModel(
                List.of(nodeDb),
                List.of(),
                DataModelStrategy.NORMALIZED_3NF,
                "NONE",
                1,
                List.of("NONE"), // Sem índices
                "NONE"
        );

        var workload = new StressWorkload(
                1000L,
                0.90,
                20_000_000_000L, // 20 GB
                20.0,
                20
        );

        StressSimulationResult result = simulator.simulate(model, workload);

        assertThat(result.violations()).anyMatch(v -> v.toLowerCase().contains("índice") || v.toLowerCase().contains("varredura"));
    }

    @Test
    @DisplayName("Deve realizar parse resiliente de JSON de estado do usuário e constraints")
    void shouldParseUserStateJsonAndConstraintsResiliently() {
        String userStateJson = """
                {
                  "nodes": [
                    { "id": "n1", "type": "SQL_DATABASE", "label": "DB Principal", "properties": { "joinsCount": 2 } },
                    { "id": "n2", "type": "CACHE_REDIS", "label": "Cache L1", "properties": {} }
                  ],
                  "edges": [
                    { "from": "n1", "to": "n2", "flowType": "READ" }
                  ],
                  "dataModel": "NORMALIZED_3NF",
                  "partitioningKey": "tenant_id",
                  "replicationFactor": 2,
                  "indexes": ["BTREE"],
                  "cachingStrategy": "CACHE_ASIDE"
                }
                """;

        String constraintsJson = """
                {
                  "targetOpsPerSec": 15000,
                  "readRatio": 0.9,
                  "datasetSizeBytes": 10737418240,
                  "targetSlaP99Ms": 25.0,
                  "concurrentClients": 40
                }
                """;

        ArchitectureModel parsedModel = simulator.parseModel(userStateJson);
        StressWorkload parsedWorkload = simulator.parseWorkload(constraintsJson, Map.of("targetOpsPerSec", 18000));

        assertThat(parsedModel.nodes()).hasSize(2);
        assertThat(parsedModel.dataModel()).isEqualTo(DataModelStrategy.NORMALIZED_3NF);
        assertThat(parsedModel.cachingStrategy()).isEqualTo("CACHE_ASIDE");

        // O override deve ter precedência
        assertThat(parsedWorkload.targetOpsPerSec()).isEqualTo(18000L);
        assertThat(parsedWorkload.readRatio()).isEqualTo(0.9);
        assertThat(parsedWorkload.targetSlaP99Ms()).isEqualTo(25.0);
    }
}
