package com.dataintensive.lab.simulation;

import java.util.List;

public record ArchitectureModel(
        List<ArchitectureNode> nodes,
        List<ArchitectureEdge> edges,
        DataModelStrategy dataModel,
        String partitioningKey,
        int replicationFactor,
        List<String> indexes,
        String cachingStrategy
) {
    public ArchitectureModel {
        nodes = nodes != null ? List.copyOf(nodes) : List.of();
        edges = edges != null ? List.copyOf(edges) : List.of();
        indexes = indexes != null ? List.copyOf(indexes) : List.of();
        dataModel = dataModel != null ? dataModel : DataModelStrategy.NORMALIZED_3NF;
        partitioningKey = partitioningKey != null && !partitioningKey.isBlank() ? partitioningKey : "NONE";
        replicationFactor = replicationFactor > 0 ? replicationFactor : 1;
        cachingStrategy = cachingStrategy != null && !cachingStrategy.isBlank() ? cachingStrategy : "NONE";
    }
}
