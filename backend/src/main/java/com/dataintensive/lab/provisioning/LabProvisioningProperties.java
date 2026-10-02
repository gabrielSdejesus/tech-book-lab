package com.dataintensive.lab.provisioning;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lab.provisioning")
public class LabProvisioningProperties {

    /**
     * Tempo de inatividade (em minutos) antes de desprovisionar o contêiner do laboratório.
     * Padrão: 15 minutos.
     */
    private int inactivityTimeoutMinutes = 15;

    /**
     * Intervalo ideal de polling/heartbeat (em segundos) sugerido para o cliente frontend.
     * Padrão: 30 segundos.
     */
    private int heartbeatIntervalSeconds = 30;

    /**
     * Tempo de tolerância / debounce (em segundos) antes de executar efetivamente o teardown do contêiner.
     * Padrão: 5 segundos.
     */
    private int teardownGracePeriodSeconds = 5;

    public LabProvisioningProperties() {}

    public LabProvisioningProperties(int inactivityTimeoutMinutes, int heartbeatIntervalSeconds) {
        this(inactivityTimeoutMinutes, heartbeatIntervalSeconds, 5);
    }

    public LabProvisioningProperties(int inactivityTimeoutMinutes, int heartbeatIntervalSeconds, int teardownGracePeriodSeconds) {
        this.inactivityTimeoutMinutes = inactivityTimeoutMinutes;
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        this.teardownGracePeriodSeconds = teardownGracePeriodSeconds;
    }

    public int getInactivityTimeoutMinutes() {
        return inactivityTimeoutMinutes;
    }

    public void setInactivityTimeoutMinutes(int inactivityTimeoutMinutes) {
        this.inactivityTimeoutMinutes = inactivityTimeoutMinutes;
    }

    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public int getTeardownGracePeriodSeconds() {
        return teardownGracePeriodSeconds;
    }

    public void setTeardownGracePeriodSeconds(int teardownGracePeriodSeconds) {
        this.teardownGracePeriodSeconds = teardownGracePeriodSeconds;
    }
}

