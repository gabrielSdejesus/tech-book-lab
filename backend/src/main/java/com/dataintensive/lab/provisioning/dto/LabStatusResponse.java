package com.dataintensive.lab.provisioning.dto;

import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.provisioning.LabEnvironmentStatus;

public record LabStatusResponse(
        String labId,
        EngineType engineType,
        LabEnvironmentStatus status,
        int allocatedPort,
        long uptimeSeconds,
        long lastHeartbeatAt,
        String errorMessage
) {}
