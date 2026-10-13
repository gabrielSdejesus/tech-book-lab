package com.dataintensive.lab.simulation;

import java.util.Map;

public record StressTestRequest(
        String userStateJson,
        Map<String, Object> workloadOverrides
) {
    public StressTestRequest {
        workloadOverrides = workloadOverrides != null ? Map.copyOf(workloadOverrides) : Map.of();
    }
}
