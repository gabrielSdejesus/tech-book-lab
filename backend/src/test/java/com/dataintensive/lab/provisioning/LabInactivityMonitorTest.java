package com.dataintensive.lab.provisioning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LabInactivityMonitorTest {

    @Mock
    private LabProvisioningService provisioningService;

    private LabProvisioningProperties properties;
    private LabInactivityMonitor monitor;

    @BeforeEach
    void setUp() {
        properties = new LabProvisioningProperties();
        properties.setInactivityTimeoutMinutes(15);
        monitor = new LabInactivityMonitor(provisioningService, properties);
    }

    @Test
    @DisplayName("Deve disparar limpeza de sessões inativas respeitando o TTL configurado de 15 minutos")
    void shouldTriggerCleanupWithConfiguredTtl() {
        monitor.monitorInactivity();

        verify(provisioningService).cleanupInactiveSessions(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("Deve respeitar TTL customizado injetado via propriedades externas")
    void shouldRespectCustomTtlFromProperties() {
        properties.setInactivityTimeoutMinutes(20);

        monitor.monitorInactivity();

        verify(provisioningService).cleanupInactiveSessions(Duration.ofMinutes(20));
    }
}
