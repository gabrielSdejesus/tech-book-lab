package com.dataintensive.lab.provisioning;

import java.time.Instant;

public record LabHeartbeatResult(
        String status,
        String labId,
        long ttlRemainingSeconds,
        Instant lastHeartbeatAt
) {}
