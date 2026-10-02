package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DockerComposeLabManagerTest {

    @Test
    @DisplayName("Deve resolver nome padrão do contêiner para POSTGRES sem hash de usuário")
    void shouldResolveStandardContainerNameForPostgres() {
        String containerName = DockerComposeLabManager.resolveContainerName(EngineType.POSTGRES);
        assertThat(containerName).isEqualTo("tbl-lab-postgres");
    }

    @Test
    @DisplayName("Deve resolver nome padrão do contêiner para NEO4J sem hash de usuário")
    void shouldResolveStandardContainerNameForNeo4j() {
        String containerName = DockerComposeLabManager.resolveContainerName(EngineType.NEO4J);
        assertThat(containerName).isEqualTo("tbl-lab-neo4j");
    }

    @Test
    @DisplayName("Deve resolver nome padronizado ignorando sessionId e labId em sobrecarga de compatibilidade")
    void shouldIgnoreSessionIdInOverload() {
        SessionId sessionId = SessionId.of("e5a8383c-6c25-4a6f-8e11-601b59836f68");
        String containerName = DockerComposeLabManager.resolveContainerName(sessionId, "ddia-cap-03-lab-01", EngineType.POSTGRES);
        assertThat(containerName).isEqualTo("tbl-lab-postgres");
    }

    @Test
    @DisplayName("Deve retornar fallback seguro quando motor for nulo")
    void shouldHandleNullEngineGracefully() {
        String containerName = DockerComposeLabManager.resolveContainerName(null);
        assertThat(containerName).isEqualTo("tbl-lab-unknown");
    }
}
