package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;

import java.time.Instant;

public record LabSession(
        SessionId sessionId,
        String labId,
        String challengeId,
        EngineType engineType,
        LabEnvironmentStatus status,
        Instant lastHeartbeatAt,
        int allocatedPort,
        String errorMessage
) {
    public LabSession(
            SessionId sessionId,
            String labId,
            EngineType engineType,
            LabEnvironmentStatus status,
            Instant lastHeartbeatAt,
            int allocatedPort,
            String errorMessage
    ) {
        this(sessionId, labId, null, engineType, status, lastHeartbeatAt, allocatedPort, errorMessage);
    }

    public LabSession withStatus(LabEnvironmentStatus newStatus) {
        return new LabSession(sessionId, labId, challengeId, engineType, newStatus, lastHeartbeatAt, allocatedPort, errorMessage);
    }

    public LabSession withHeartbeat(Instant newHeartbeat) {
        return new LabSession(sessionId, labId, challengeId, engineType, status, newHeartbeat, allocatedPort, errorMessage);
    }

    public LabSession withPort(int port) {
        return new LabSession(sessionId, labId, challengeId, engineType, status, lastHeartbeatAt, port, errorMessage);
    }

    public LabSession withError(String error) {
        return new LabSession(sessionId, labId, challengeId, engineType, LabEnvironmentStatus.ERROR, lastHeartbeatAt, allocatedPort, error);
    }
}
