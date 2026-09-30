package com.dataintensive.lab.provisioning;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@EnableScheduling
public class LabInactivityMonitor {

    private final LabProvisioningService provisioningService;

    public LabInactivityMonitor(LabProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @Scheduled(fixedDelay = 60000)
    public void monitorInactivity() {
        provisioningService.cleanupInactiveSessions(Duration.ofMinutes(15));
    }
}
