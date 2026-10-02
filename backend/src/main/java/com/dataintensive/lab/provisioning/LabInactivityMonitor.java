package com.dataintensive.lab.provisioning;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@EnableScheduling
public class LabInactivityMonitor {

    private final LabProvisioningService provisioningService;
    private final LabProvisioningProperties properties;

    public LabInactivityMonitor(LabProvisioningService provisioningService, LabProvisioningProperties properties) {
        this.provisioningService = provisioningService;
        this.properties = properties;
    }

    @Scheduled(fixedDelay = 15000)
    public void monitorInactivity() {
        provisioningService.cleanupInactiveSessions(Duration.ofMinutes(properties.getInactivityTimeoutMinutes()));
    }
}
