package com.dataintensive.lab.provisioning.dto;

import com.dataintensive.lab.provisioning.LabEnvironmentStatus;

public record LabTeardownResponse(
        String labId,
        LabEnvironmentStatus status,
        String message
) {}
