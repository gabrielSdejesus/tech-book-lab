package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.catalog.CatalogRepository;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabProvisioningServiceTest {

    @Mock
    private CatalogRepository catalogRepository;

    @Mock
    private LabContainerManager containerManager;

    private LabProvisioningService provisioningService;

    private final SessionId sessionId = SessionId.of(UUID.randomUUID().toString());
    private final String validLabId = "ddia-cap-03-lab-01";
    private final Lab mockLab = new Lab(
            validLabId,
            1,
            "relacional-vs-documentos",
            "Modelagem Relacional vs Documentos",
            "Sumário",
            List.of(),
            EngineType.POSTGRES,
            "tbl_lab",
            "SELECT 1;",
            List.of()
    );

    @BeforeEach
    void setUp() {
        provisioningService = new LabProvisioningService(catalogRepository, containerManager);
    }

    @Test
    @DisplayName("Deve provisionar contêiner sob demanda com sucesso quando o lab existir no catálogo")
    void shouldProvisionLabSuccessfully() {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        LabSession session = provisioningService.provisionLab(sessionId, validLabId);

        assertThat(session.sessionId()).isEqualTo(sessionId);
        assertThat(session.labId()).isEqualTo(validLabId);
        assertThat(session.engineType()).isEqualTo(EngineType.POSTGRES);
        assertThat(session.status()).isEqualTo(LabEnvironmentStatus.READY);
        assertThat(session.allocatedPort()).isEqualTo(5432);

        verify(containerManager).startEngine(EngineType.POSTGRES);
    }

    @Test
    @DisplayName("Deve rejeitar provisionamento de labId inexistente no catálogo com LabNotFoundException")
    void shouldRejectUnknownLabId() {
        when(catalogRepository.findLabById("lab-fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provisioningService.provisionLab(sessionId, "lab-fantasma"))
                .isInstanceOf(LabNotFoundException.class)
                .hasMessageContaining("lab-fantasma");

        verifyNoInteractions(containerManager);
    }

    @Test
    @DisplayName("Deve desprovisionar laboratório anterior ao solicitar novo lab na mesma sessão (Cota Anti-DoS)")
    void shouldTeardownPreviousLabWhenNewOneRequestedInSameSession() {
        Lab secondLab = new Lab(
                "ddia-cap-03-lab-02",
                2,
                "grafo-social",
                "Rede Social com Grafo",
                "Sumário",
                List.of(),
                EngineType.NEO4J,
                "tbl_neo4j",
                "RETURN 1;",
                List.of()
        );

        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(catalogRepository.findLabById("ddia-cap-03-lab-02")).thenReturn(Optional.of(secondLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);
        when(containerManager.isEngineHealthy(EngineType.NEO4J, 7687)).thenReturn(true);

        // Provisiona o primeiro (PostgreSQL)
        provisioningService.provisionLab(sessionId, validLabId);
        verify(containerManager).startEngine(EngineType.POSTGRES);

        // Provisiona o segundo (Neo4j) na mesma sessão
        LabSession newSession = provisioningService.provisionLab(sessionId, "ddia-cap-03-lab-02");

        // Deve ter parado o PostgreSQL anterior antes de subir o Neo4j
        verify(containerManager).stopEngine(EngineType.POSTGRES);
        verify(containerManager).startEngine(EngineType.NEO4J);
        assertThat(newSession.engineType()).isEqualTo(EngineType.NEO4J);
        assertThat(newSession.status()).isEqualTo(LabEnvironmentStatus.READY);
    }

    @Test
    @DisplayName("Deve atualizar heartbeat e renovar TTL de sessão ativa")
    void shouldRenewHeartbeatForActiveSession() {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        provisioningService.provisionLab(sessionId, validLabId);

        LabHeartbeatResult result = provisioningService.heartbeat(sessionId, validLabId);

        assertThat(result.status()).isEqualTo("ACK");
        assertThat(result.ttlRemainingSeconds()).isGreaterThan(890);
    }

    @Test
    @DisplayName("Deve lançar SessionExpiredException ao enviar heartbeat para sessão inexistente")
    void shouldThrowWhenHeartbeatSentForUnknownSession() {
        SessionId unknownSession = SessionId.of(UUID.randomUUID().toString());

        assertThatThrownBy(() -> provisioningService.heartbeat(unknownSession, validLabId))
                .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    @DisplayName("Deve executar teardown explícito do contêiner e marcar como STOPPED")
    void shouldTeardownExplicitly() {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        provisioningService.provisionLab(sessionId, validLabId);
        LabSession stopped = provisioningService.teardown(sessionId, validLabId);

        assertThat(stopped.status()).isEqualTo(LabEnvironmentStatus.STOPPED);
        verify(containerManager).stopEngine(EngineType.POSTGRES);
    }

    @Test
    @DisplayName("Deve desprovisionar automaticamente sessões inativas há mais de 15 minutos")
    void shouldAutoStopInactiveSessionsAfterTtl() {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        LabSession session = provisioningService.provisionLab(sessionId, validLabId);

        // Simula passagem de 16 minutos sem heartbeat
        Instant pastTime = Instant.now().minus(Duration.ofMinutes(16));
        provisioningService.overrideSessionLastHeartbeatForTest(sessionId, pastTime);

        // Dispara limpeza de inatividade
        int cleaned = provisioningService.cleanupInactiveSessions(Duration.ofMinutes(15));

        assertThat(cleaned).isEqualTo(1);
        verify(containerManager).stopEngine(EngineType.POSTGRES);

        LabSession status = provisioningService.getLabStatus(sessionId, validLabId);
        assertThat(status.status()).isEqualTo(LabEnvironmentStatus.STOPPED);
    }
}
