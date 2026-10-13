package com.dataintensive.lab.simulation;

import java.util.Map;

public record ArchitectureNode(
        String id,
        ArchitectureNodeType type,
        String label,
        Map<String, Object> properties
) {
    public ArchitectureNode {
        properties = properties != null ? Map.copyOf(properties) : Map.of();
    }
}
