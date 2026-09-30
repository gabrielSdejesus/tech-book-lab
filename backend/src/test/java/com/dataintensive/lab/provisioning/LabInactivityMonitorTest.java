package com.dataintensive.lab.provisioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LabInactivityMonitorTest {

    @Mock
    private LabProvisioningService provisioningService;

    @InjectMocks
    private LabInactivityMonitor monitor;

    @Test
    @DisplayName("Deve disparar limpeza de sessões inativas com TTL rigoroso de 1 minuto")
    void shouldTriggerCleanupWithOneMinuteTtl() {
        monitor.monitorInactivity();

        verify(provisioningService).cleanupInactiveSessions(Duration.ofMinutes(1));
    }
}
