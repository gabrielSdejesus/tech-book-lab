package com.dataintensive.lab.provisioning.dto;

import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.provisioning.LabEnvironmentStatus;

public record LabStatusResponse(
        String labId,
        String challengeId,
        String containerName,
        EngineType engineType,
        LabEnvironmentStatus status,
        int allocatedPort,
        long uptimeSeconds,
        long lastHeartbeatAt,
        String errorMessage
) {
    public LabStatusResponse(
            String labId,
            String challengeId,
            EngineType engineType,
            LabEnvironmentStatus status,
            int allocatedPort,
            long uptimeSeconds,
            long lastHeartbeatAt,
            String errorMessage
    ) {
        this(labId, challengeId, null, engineType, status, allocatedPort, uptimeSeconds, lastHeartbeatAt, errorMessage);
    }

    public LabStatusResponse(
            String labId,
            EngineType engineType,
            LabEnvironmentStatus status,
            int allocatedPort,
            long uptimeSeconds,
            long lastHeartbeatAt,
            String errorMessage
    ) {
        this(labId, null, null, engineType, status, allocatedPort, uptimeSeconds, lastHeartbeatAt, errorMessage);
    }
}
