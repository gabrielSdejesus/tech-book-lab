package com.dataintensive.lab.simulation;

import java.util.List;

public record StressSimulationResult(
        double estimatedLatencyP50Ms,
        double estimatedLatencyP99Ms,
        double estimatedThroughputOps,
        double lockContentionIndex,
        boolean slaMet,
        List<String> violations,
        List<String> recommendations
) {
    public StressSimulationResult {
        violations = violations != null ? List.copyOf(violations) : List.of();
        recommendations = recommendations != null ? List.copyOf(recommendations) : List.of();
    }
}
