package com.dataintensive.lab.simulation;

public record StressWorkload(
        long targetOpsPerSec,
        double readRatio,
        long datasetSizeBytes,
        double targetSlaP99Ms,
        int concurrentClients
) {
    public StressWorkload {
        targetOpsPerSec = Math.max(1, targetOpsPerSec);
        readRatio = Math.max(0.0, Math.min(1.0, readRatio));
        datasetSizeBytes = Math.max(0L, datasetSizeBytes);
        targetSlaP99Ms = Math.max(1.0, targetSlaP99Ms);
        concurrentClients = Math.max(1, concurrentClients);
    }
}
