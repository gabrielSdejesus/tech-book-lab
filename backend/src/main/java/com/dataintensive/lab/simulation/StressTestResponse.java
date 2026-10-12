package com.dataintensive.lab.simulation;

import java.util.List;

public record StressTestResponse(
        String scenarioId,
        double estimatedLatencyP50Ms,
        double estimatedLatencyP99Ms,
        double estimatedThroughputOps,
        double lockContentionIndex,
        boolean slaMet,
        double targetSlaP99Ms,
        List<String> violations,
        List<String> recommendations
) {
    public StressTestResponse {
        violations = violations != null ? List.copyOf(violations) : List.of();
        recommendations = recommendations != null ? List.copyOf(recommendations) : List.of();
    }
}
