package com.dataintensive.lab.simulation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ArchitectureStressSimulator {

    private final ObjectMapper objectMapper;

    public ArchitectureStressSimulator() {
        this.objectMapper = new ObjectMapper();
    }

    public ArchitectureStressSimulator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public StressSimulationResult simulate(ArchitectureModel model, StressWorkload workload) {
        List<String> violations = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        boolean hasCache = model.nodes().stream().anyMatch(n -> n.type() == ArchitectureNodeType.CACHE_REDIS)
                || "CACHE_ASIDE".equalsIgnoreCase(model.cachingStrategy());

        boolean hasShards = model.nodes().stream().anyMatch(n -> n.type() == ArchitectureNodeType.SHARD_CLUSTER)
                || (!"NONE".equalsIgnoreCase(model.partitioningKey()) && !model.partitioningKey().isBlank());

        int shardsCount = 1;
        for (ArchitectureNode node : model.nodes()) {
            if (node.type() == ArchitectureNodeType.SHARD_CLUSTER && node.properties() != null && node.properties().containsKey("shardsCount")) {
                Object val = node.properties().get("shardsCount");
                if (val instanceof Number num) {
                    shardsCount = Math.max(1, num.intValue());
                }
            }
        }
        if (hasShards && shardsCount == 1) {
            shardsCount = 2;
        }

        int replicasCount = Math.max(1, model.replicationFactor());
        for (ArchitectureNode node : model.nodes()) {
            if (node.type() == ArchitectureNodeType.READ_REPLICA) {
                replicasCount = Math.max(replicasCount, 2);
            }
        }

        int joinsCount = 1;
        for (ArchitectureNode node : model.nodes()) {
            if (node.properties() != null && node.properties().containsKey("joinsCount")) {
                Object val = node.properties().get("joinsCount");
                if (val instanceof Number num) {
                    joinsCount = Math.max(joinsCount, num.intValue());
                }
            }
        }

        boolean hasIndexes = !model.indexes().isEmpty() && !model.indexes().stream().allMatch(i -> "NONE".equalsIgnoreCase(i) || i.isBlank());

        // 1. Verificação de Índices
        double indexPenalty = 0.0;
        if (!hasIndexes && workload.datasetSizeBytes() > 5_000_000_000L) {
            indexPenalty = 25.0;
            violations.add("Ausência de índices em volume de dados superior a 5GB: risco crítico de Sequential Scan e saturação de I/O.");
            recommendations.add("Adicione índices secundários B-Tree nas chaves de busca mais frequentes para evitar varredura sequencial da tabela.");
        }

        // 2. Latência de Leitura no Banco
        double dbReadLatency = 2.5 + indexPenalty;
        if (model.dataModel() == DataModelStrategy.NORMALIZED_3NF) {
            if (joinsCount > 1) {
                dbReadLatency += (joinsCount - 1) * 6.5;
            }
            double readOps = workload.targetOpsPerSec() * workload.readRatio();
            if (joinsCount >= 3 && !hasCache && readOps > 5000) {
                violations.add("Excesso de JOINs normalizados (" + joinsCount + ") sob carga massiva de leitura sem camada de cache: degradação severa de latência p99.");
                recommendations.add("Considere introduzir uma camada de Cache-Aside (ex: Redis) ou desnormalizar agregados para consultas frequentes (DDIA Cap. 3).");
            }
        } else if (model.dataModel() == DataModelStrategy.DENORMALIZED_DOCUMENT) {
            dbReadLatency = 2.0 + (indexPenalty * 0.5);
        }

        double effectiveReadLatency;
        if (hasCache) {
            double cacheHitRatio = 0.85;
            double cacheLatency = 0.8;
            effectiveReadLatency = (cacheHitRatio * cacheLatency) + ((1.0 - cacheHitRatio) * dbReadLatency);
        } else {
            effectiveReadLatency = dbReadLatency;
        }

        // 3. Latência de Escrita e Contenção de Lock
        double writeOps = workload.targetOpsPerSec() * (1.0 - workload.readRatio());
        double lockContentionIndex = 0.0;

        if (model.dataModel() == DataModelStrategy.DENORMALIZED_DOCUMENT) {
            if (!hasShards && writeOps > 1000) {
                lockContentionIndex = Math.min(1.0, (writeOps / 1000.0) * 0.07);
                if (lockContentionIndex > 0.25) {
                    violations.add("Alta contenção de lock e concorrência detectada em documento compartilhado sob taxa pesada de escrita (" + (int) writeOps + " ops/s) sem particionamento.");
                    recommendations.add("Implemente particionamento por hash de chave de alta cardinalidade para paralelizar escritas e eliminar gargalos de contenção.");
                }
            } else if (hasShards) {
                lockContentionIndex = Math.min(1.0, (writeOps / (1000.0 * shardsCount)) * 0.03);
            }
        } else {
            if (!hasShards && writeOps > 3000) {
                lockContentionIndex = Math.min(1.0, (writeOps / 3000.0) * 0.05);
            } else if (hasShards) {
                lockContentionIndex = Math.min(1.0, (writeOps / (3000.0 * shardsCount)) * 0.02);
            }
        }

        double writeLatency = 4.0 + (lockContentionIndex * 25.0);

        // 4. Latência Combinada p50 e p99
        double p50 = (workload.readRatio() * effectiveReadLatency) + ((1.0 - workload.readRatio()) * writeLatency);
        double concurrencyLoadFactor = 1.0 + (workload.targetOpsPerSec() / 15000.0) * 0.6 + (lockContentionIndex * 2.5);
        double p99 = p50 * concurrencyLoadFactor * 1.5;

        // 5. Throughput
        double baseThroughputPerNode = (1000.0 / Math.max(0.5, p50)) * workload.concurrentClients() * 0.40;
        double scaleMultiplier = shardsCount * (1.0 + Math.min(3, replicasCount - 1) * 0.6);
        double estimatedThroughput = baseThroughputPerNode * scaleMultiplier;

        boolean slaMet = (p99 <= workload.targetSlaP99Ms()) && violations.isEmpty();

        if (p99 > workload.targetSlaP99Ms() && violations.isEmpty()) {
            violations.add("Latência p99 estimada (" + Math.round(p99 * 10.0) / 10.0 + " ms) ultrapassa o SLA contratado de " + workload.targetSlaP99Ms() + " ms.");
            recommendations.add("Otimize componentes da arquitetura adicionando réplicas de leitura, particionamento ou cache em memória.");
        }

        return new StressSimulationResult(
                Math.round(p50 * 10.0) / 10.0,
                Math.round(p99 * 10.0) / 10.0,
                Math.round(estimatedThroughput * 10.0) / 10.0,
                Math.round(lockContentionIndex * 100.0) / 100.0,
                slaMet,
                violations,
                recommendations
        );
    }

    public ArchitectureModel parseModel(String userStateJson) {
        if (userStateJson == null || userStateJson.isBlank()) {
            return new ArchitectureModel(List.of(), List.of(), DataModelStrategy.NORMALIZED_3NF, "NONE", 1, List.of("BTREE"), "NONE");
        }
        try {
            JsonNode root = objectMapper.readTree(userStateJson);
            List<ArchitectureNode> nodes = new ArrayList<>();
            if (root.has("nodes") && root.get("nodes").isArray()) {
                for (JsonNode n : root.get("nodes")) {
                    String id = n.path("id").asText("n" + nodes.size());
                    String typeStr = n.path("type").asText("SQL_DATABASE");
                    ArchitectureNodeType type;
                    try {
                        type = ArchitectureNodeType.valueOf(typeStr.trim().toUpperCase());
                    } catch (Exception e) {
                        type = ArchitectureNodeType.SQL_DATABASE;
                    }
                    String label = n.path("label").asText(id);
                    Map<String, Object> props = Map.of();
                    if (n.has("properties") && n.get("properties").isObject()) {
                        props = objectMapper.convertValue(n.get("properties"), new TypeReference<Map<String, Object>>() {});
                    }
                    nodes.add(new ArchitectureNode(id, type, label, props));
                }
            }

            List<ArchitectureEdge> edges = new ArrayList<>();
            if (root.has("edges") && root.get("edges").isArray()) {
                for (JsonNode e : root.get("edges")) {
                    String from = e.path("from").asText("");
                    String to = e.path("to").asText("");
                    String flowType = e.path("flowType").asText("SYNC");
                    edges.add(new ArchitectureEdge(from, to, flowType));
                }
            }

            String dataModelStr = root.path("dataModel").asText("NORMALIZED_3NF");
            DataModelStrategy dataModel;
            try {
                dataModel = DataModelStrategy.valueOf(dataModelStr.trim().toUpperCase());
            } catch (Exception e) {
                dataModel = DataModelStrategy.NORMALIZED_3NF;
            }

            String partitioningKey = root.path("partitioningKey").asText("NONE");
            int replicationFactor = root.path("replicationFactor").asInt(1);

            List<String> indexes = new ArrayList<>();
            if (root.has("indexes") && root.get("indexes").isArray()) {
                for (JsonNode idx : root.get("indexes")) {
                    indexes.add(idx.asText());
                }
            } else {
                indexes.add("BTREE");
            }

            String cachingStrategy = root.path("cachingStrategy").asText("NONE");

            return new ArchitectureModel(nodes, edges, dataModel, partitioningKey, replicationFactor, indexes, cachingStrategy);
        } catch (Exception e) {
            return new ArchitectureModel(List.of(), List.of(), DataModelStrategy.NORMALIZED_3NF, "NONE", 1, List.of("BTREE"), "NONE");
        }
    }

    public StressWorkload parseWorkload(String constraintsJson, Map<String, Object> overrides) {
        long targetOps = 10000L;
        double readRatio = 0.90;
        long datasetSize = 10_000_000_000L;
        double targetSla = 50.0;
        int concurrentClients = 50;

        if (constraintsJson != null && !constraintsJson.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(constraintsJson);
                if (root.has("targetOpsPerSec")) targetOps = root.get("targetOpsPerSec").asLong(targetOps);
                if (root.has("readRatio")) readRatio = root.get("readRatio").asDouble(readRatio);
                if (root.has("datasetSizeBytes")) datasetSize = root.get("datasetSizeBytes").asLong(datasetSize);
                if (root.has("targetSlaP99Ms")) targetSla = root.get("targetSlaP99Ms").asDouble(targetSla);
                if (root.has("concurrentClients")) concurrentClients = root.get("concurrentClients").asInt(concurrentClients);
            } catch (Exception ignored) {
            }
        }

        if (overrides != null) {
            if (overrides.containsKey("targetOpsPerSec") && overrides.get("targetOpsPerSec") instanceof Number n) {
                targetOps = n.longValue();
            }
            if (overrides.containsKey("readRatio") && overrides.get("readRatio") instanceof Number n) {
                readRatio = n.doubleValue();
            }
            if (overrides.containsKey("datasetSizeBytes") && overrides.get("datasetSizeBytes") instanceof Number n) {
                datasetSize = n.longValue();
            }
            if (overrides.containsKey("targetSlaP99Ms") && overrides.get("targetSlaP99Ms") instanceof Number n) {
                targetSla = n.doubleValue();
            }
            if (overrides.containsKey("concurrentClients") && overrides.get("concurrentClients") instanceof Number n) {
                concurrentClients = n.intValue();
            }
        }

        return new StressWorkload(targetOps, readRatio, datasetSize, targetSla, concurrentClients);
    }
}
