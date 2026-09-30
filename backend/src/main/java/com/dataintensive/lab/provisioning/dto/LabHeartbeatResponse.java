package com.dataintensive.lab.provisioning.dto;

public record LabHeartbeatResponse(
        String status,
        String labId,
        long ttlRemainingSeconds,
        long lastHeartbeatAt
) {}
