package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;

import java.time.Instant;

public record LabSession(
        SessionId sessionId,
        String labId,
        String challengeId,
        String containerName,
        EngineType engineType,
        LabEnvironmentStatus status,
        Instant lastHeartbeatAt,
        int allocatedPort,
        String errorMessage
) {
    public LabSession(
            SessionId sessionId,
            String labId,
            String challengeId,
            EngineType engineType,
            LabEnvironmentStatus status,
            Instant lastHeartbeatAt,
            int allocatedPort,
            String errorMessage
    ) {
        this(sessionId, labId, challengeId, null, engineType, status, lastHeartbeatAt, allocatedPort, errorMessage);
    }

    public LabSession(
            SessionId sessionId,
            String labId,
            EngineType engineType,
            LabEnvironmentStatus status,
            Instant lastHeartbeatAt,
            int allocatedPort,
            String errorMessage
    ) {
        this(sessionId, labId, null, null, engineType, status, lastHeartbeatAt, allocatedPort, errorMessage);
    }

    public LabSession withStatus(LabEnvironmentStatus newStatus) {
        return new LabSession(sessionId, labId, challengeId, containerName, engineType, newStatus, lastHeartbeatAt, allocatedPort, errorMessage);
    }

    public LabSession withHeartbeat(Instant newHeartbeat) {
        return new LabSession(sessionId, labId, challengeId, containerName, engineType, status, newHeartbeat, allocatedPort, errorMessage);
    }
}
