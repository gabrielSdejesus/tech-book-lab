package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.provisioning.dto.EngineHealthStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InfraStatusServiceTest {

    @Test
    @DisplayName("Deve reportar UP para porta com socket aberto e DOWN para porta fechada")
    void shouldReportUpForOpenSocketAndDownForClosedSocket() throws IOException {
        int openPort;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            openPort = serverSocket.getLocalPort();
            int closedPort;
            try (ServerSocket tempSocket = new ServerSocket(0)) {
                closedPort = tempSocket.getLocalPort();
            } // tempSocket fechado imediatamente

            LabEngineProperties properties = new LabEngineProperties();
            Map<String, LabEngineProperties.EngineConfig> customConfigs = new LinkedHashMap<>();
            customConfigs.put("postgres", new LabEngineProperties.EngineConfig("postgres", openPort, "tbl-lab-postgres"));
            customConfigs.put("neo4j", new LabEngineProperties.EngineConfig("neo4j", closedPort, "tbl-lab-neo4j"));
            properties.setConfigs(customConfigs);

            InfraStatusService service = new InfraStatusService(properties);

            Map<String, EngineHealthStatus> statuses = service.getInfraStatus();

            assertThat(statuses).containsKeys("postgres", "neo4j");

            EngineHealthStatus postgresStatus = statuses.get("postgres");
            assertThat(postgresStatus.healthy()).isTrue();
            assertThat(postgresStatus.status()).isEqualTo("UP");
            assertThat(postgresStatus.port()).isEqualTo(openPort);
            assertThat(postgresStatus.serviceName()).isEqualTo("postgres");

            EngineHealthStatus neo4jStatus = statuses.get("neo4j");
            assertThat(neo4jStatus.healthy()).isFalse();
            assertThat(neo4jStatus.status()).isEqualTo("DOWN");
            assertThat(neo4jStatus.port()).isEqualTo(closedPort);
            assertThat(neo4jStatus.serviceName()).isEqualTo("neo4j");
        }
    }
}
