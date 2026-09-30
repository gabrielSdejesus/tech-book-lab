package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;

import java.time.Instant;

public record LabSession(
        SessionId sessionId,
        String labId,
        EngineType engineType,
        LabEnvironmentStatus status,
        Instant lastHeartbeatAt,
        int allocatedPort,
        String errorMessage
) {
    public LabSession withStatus(LabEnvironmentStatus newStatus) {
        return new LabSession(sessionId, labId, engineType, newStatus, lastHeartbeatAt, allocatedPort, errorMessage);
    }

    public LabSession withHeartbeat(Instant newHeartbeat) {
        return new LabSession(sessionId, labId, engineType, status, newHeartbeat, allocatedPort, errorMessage);
    }

    public LabSession withPort(int port) {
        return new LabSession(sessionId, labId, engineType, status, lastHeartbeatAt, port, errorMessage);
    }

    public LabSession withError(String error) {
        return new LabSession(sessionId, labId, engineType, LabEnvironmentStatus.ERROR, lastHeartbeatAt, allocatedPort, error);
    }
}
