package com.dataintensive.lab.provisioning.dto;

import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.provisioning.LabEnvironmentStatus;

public record LabProvisionResponse(
        String labId,
        EngineType engineType,
        LabEnvironmentStatus status,
        String message,
        int allocatedPort,
        int estimatedWaitSeconds
) {}
