package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.catalog.CatalogRepository;
import com.dataintensive.lab.domain.Challenge;
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

    @Mock
    private LabDatabaseResetter databaseResetter;

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

    private LabProvisioningProperties properties;
    private MutableClock clock;
    private EngineLifecycleCoordinator lifecycleCoordinator;

    static class MutableClock extends java.time.Clock {
        private Instant currentInstant;
        private final java.time.ZoneId zone = java.time.ZoneId.of("UTC");

        public MutableClock(Instant initialInstant) {
            this.currentInstant = initialInstant;
        }

        public void advance(Duration duration) {
            this.currentInstant = this.currentInstant.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return zone;
        }

        @Override
        public java.time.Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }

    @BeforeEach
    void setUp() {
        properties = new LabProvisioningProperties();
        clock = new MutableClock(Instant.parse("2026-10-02T12:00:00Z"));
        lifecycleCoordinator = new EngineLifecycleCoordinator();
        provisioningService = new LabProvisioningService(catalogRepository, containerManager, properties, clock, lifecycleCoordinator, databaseResetter);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        lifecycleCoordinator.shutdown();
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
    @DisplayName("Não deve derrubar contêiner de outro lab na mesma sessão permitindo isolamento individualizado")
    void shouldNotKillPreviousLabContainerWhenSwitchingToDifferentLab() {
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

        // Provisiona o segundo (Neo4j) na mesma sessão
        LabSession newSession = provisioningService.provisionLab(sessionId, "ddia-cap-03-lab-02");

        // O PostgreSQL do Lab 1 NÃO deve ser derrubado imediatamente (mantém vivo até inatividade)
        verify(containerManager, never()).stopEngine(EngineType.POSTGRES);
        verify(containerManager).startEngine(EngineType.NEO4J);
        assertThat(newSession.engineType()).isEqualTo(EngineType.NEO4J);
        assertThat(newSession.status()).isEqualTo(LabEnvironmentStatus.READY);

        // Ambos os labs devem permanecer registrados e consultáveis
        LabSession session1 = provisioningService.getLabStatus(sessionId, validLabId);
        assertThat(session1.status()).isEqualTo(LabEnvironmentStatus.READY);
    }

    @Test
    @DisplayName("Deve registrar nome padronizado do contêiner e porta dinâmica no LabSession")
    void shouldTrackUniqueContainerNameAndPortInLabSession() {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(eq(EngineType.POSTGRES), anyInt())).thenReturn(true);

        LabSession session = provisioningService.provisionLab(sessionId, validLabId);

        assertThat(session.containerName())
                .isEqualTo("tbl-lab-postgres");
    }

    @Test
    @DisplayName("Deve atualizar heartbeat e renovar TTL de 1 minuto (60s) para sessão ativa")
    void shouldRenewHeartbeatForActiveSession() {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        provisioningService.provisionLab(sessionId, validLabId);

        LabHeartbeatResult result = provisioningService.heartbeat(sessionId, validLabId);

        assertThat(result.status()).isEqualTo("ACK");
        assertThat(result.ttlRemainingSeconds()).isEqualTo(900);
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
        properties.setTeardownGracePeriodSeconds(0);
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        provisioningService.provisionLab(sessionId, validLabId);
        LabSession stopped = provisioningService.teardown(sessionId, validLabId);

        assertThat(stopped.status()).isEqualTo(LabEnvironmentStatus.STOPPED);
        verify(containerManager).stopEngine(EngineType.POSTGRES);
    }


    @Test
    @DisplayName("Deve desprovisionar automaticamente sessões inativas há mais de 15 minutos")
    void shouldAutoStopInactiveSessionsAfterFifteenMinutesTtl() {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        LabSession session = provisioningService.provisionLab(sessionId, validLabId);

        // Simula passagem de 16 minutos no relógio sem heartbeat
        clock.advance(Duration.ofMinutes(16));

        // Dispara limpeza de inatividade com janela de 15 minutos
        int cleaned = provisioningService.cleanupInactiveSessions(Duration.ofMinutes(15));

        assertThat(cleaned).isEqualTo(1);
        verify(containerManager).stopEngine(EngineType.POSTGRES);

        LabSession status = provisioningService.getLabStatus(sessionId, validLabId);
        assertThat(status.status()).isEqualTo(LabEnvironmentStatus.STOPPED);
    }

    @Test
    @DisplayName("Deve provisionar motor específico da tarefa quando challengeId for informado (Ex: Lab 3.2 Desafio 2 usa POSTGRES)")
    void shouldProvisionChallengeSpecificEngineWhenChallengeIdProvided() {
        Challenge ch1 = new Challenge("lab-02-ch-1", 1, "Cypher", "Desc", "Cenário", "RETURN 1;", List.of(), "Reflexão", EngineType.NEO4J);
        Challenge ch2 = new Challenge("lab-02-ch-2", 2, "SQL Recursivo", "Desc", "Cenário", "WITH RECURSIVE...", List.of(), "Reflexão", EngineType.POSTGRES);
        Lab hybridLab = new Lab(
                "ddia-cap-03-lab-02",
                2,
                "grafos-propriedades",
                "Grafos de Propriedades vs SQL Recursivo",
                "Sumário",
                List.of(),
                EngineType.NEO4J,
                "neo4j",
                "MATCH (n) DETACH DELETE n;",
                List.of(ch1, ch2)
        );

        when(catalogRepository.findLabById("ddia-cap-03-lab-02")).thenReturn(Optional.of(hybridLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        LabSession session = provisioningService.provisionLab(sessionId, "ddia-cap-03-lab-02", "lab-02-ch-2");

        assertThat(session.engineType()).isEqualTo(EngineType.POSTGRES);
        assertThat(session.allocatedPort()).isEqualTo(5432);
        assertThat(session.challengeId()).isEqualTo("lab-02-ch-2");
        verify(containerManager).startEngine(EngineType.POSTGRES);
    }

    @Test
    @DisplayName("Deve alternar motores ao trocar de desafio no mesmo laboratório híbrido (Neo4j -> Postgres)")
    void shouldSwitchEngineWhenSwitchingBetweenChallengesInHybridLab() {
        Challenge ch1 = new Challenge("lab-02-ch-1", 1, "Cypher", "Desc", "Cenário", "RETURN 1;", List.of(), "Reflexão", EngineType.NEO4J);
        Challenge ch2 = new Challenge("lab-02-ch-2", 2, "SQL Recursivo", "Desc", "Cenário", "WITH RECURSIVE...", List.of(), "Reflexão", EngineType.POSTGRES);
        Lab hybridLab = new Lab(
                "ddia-cap-03-lab-02",
                2,
                "grafos-propriedades",
                "Grafos de Propriedades vs SQL Recursivo",
                "Sumário",
                List.of(),
                EngineType.NEO4J,
                "neo4j",
                "MATCH (n) DETACH DELETE n;",
                List.of(ch1, ch2)
        );

        when(catalogRepository.findLabById("ddia-cap-03-lab-02")).thenReturn(Optional.of(hybridLab));
        when(containerManager.isEngineHealthy(EngineType.NEO4J, 7687)).thenReturn(true);
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        // Desafio 1 -> sobe Neo4j
        provisioningService.provisionLab(sessionId, "ddia-cap-03-lab-02", "lab-02-ch-1");
        verify(containerManager).startEngine(EngineType.NEO4J);

        // Usuário clica no Desafio 2 -> deve parar Neo4j e subir Postgres
        LabSession session2 = provisioningService.provisionLab(sessionId, "ddia-cap-03-lab-02", "lab-02-ch-2");
        verify(containerManager).stopEngine(EngineType.NEO4J);
        verify(containerManager).startEngine(EngineType.POSTGRES);
        assertThat(session2.engineType()).isEqualTo(EngineType.POSTGRES);
        assertThat(session2.challengeId()).isEqualTo("lab-02-ch-2");
    }

    @Test
    @DisplayName("Deve agendar teardown e não parar contêiner imediatamente ao solicitar teardown")
    void shouldScheduleTeardownInsteadOfImmediateStop() {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        provisioningService.provisionLab(sessionId, validLabId);

        LabSession stoppedSession = provisioningService.teardown(sessionId, validLabId);

        assertThat(stoppedSession.status()).isEqualTo(LabEnvironmentStatus.STOPPED);
        assertThat(lifecycleCoordinator.isTeardownPending(EngineType.POSTGRES)).isTrue();
        // Não deve ter parado o motor de imediato (aguarda grace period)
        verify(containerManager, never()).stopEngine(EngineType.POSTGRES);
    }

    @Test
    @DisplayName("Deve cancelar teardown agendado e manter contêiner READY ao retornar rapidamente para o laboratório")
    void shouldCancelScheduledTeardownAndMaintainContainerReadyWhenSwitchingQuickly() {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        // 1. Provisiona lab
        provisioningService.provisionLab(sessionId, validLabId);
        verify(containerManager, times(1)).startEngine(EngineType.POSTGRES);

        // 2. Usuário clica na estante -> teardown agendado
        provisioningService.teardown(sessionId, validLabId);
        assertThat(lifecycleCoordinator.isTeardownPending(EngineType.POSTGRES)).isTrue();

        // 3. Usuário clica rapidamente de volta no lab antes do grace period expirar
        LabSession restored = provisioningService.provisionLab(sessionId, validLabId);

        // Teardown pendente foi cancelado
        assertThat(lifecycleCoordinator.isTeardownPending(EngineType.POSTGRES)).isFalse();
        assertThat(restored.status()).isEqualTo(LabEnvironmentStatus.READY);

        // Contêiner não precisou sofrer stop nem novo start desnecessário
        verify(containerManager, never()).stopEngine(EngineType.POSTGRES);
        verify(containerManager, times(1)).startEngine(EngineType.POSTGRES);
    }

    @Test
    @DisplayName("Deve executar reset automático do banco de dados ao provisionar novo laboratório para a sessão")
    void shouldResetDatabaseWhenProvisioningNewLabForSession() {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        LabSession session = provisioningService.provisionLab(sessionId, validLabId);

        assertThat(session.status()).isEqualTo(LabEnvironmentStatus.READY);
        verify(databaseResetter, times(1)).resetDatabase(EngineType.POSTGRES, mockLab);
    }

    @Test
    @DisplayName("Não deve executar reset automático do banco ao re-provisionar o mesmo laboratório na sessão")
    void shouldNotResetDatabaseWhenReprovisioningSameLabInSession() {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        // Primeiro provisionamento do Lab 1
        provisioningService.provisionLab(sessionId, validLabId);
        verify(databaseResetter, times(1)).resetDatabase(EngineType.POSTGRES, mockLab);

        // Segundo provisionamento do MESMO Lab 1 (ex: troca de desafio ou reload)
        provisioningService.provisionLab(sessionId, validLabId, "lab-01-ch-2");
        // O reset NÃO deve ter sido chamado novamente
        verify(databaseResetter, times(1)).resetDatabase(EngineType.POSTGRES, mockLab);
    }

    @Test
    @DisplayName("Deve executar reset do banco ao alternar entre laboratórios diferentes")
    void shouldResetDatabaseWhenSwitchingBetweenDifferentLabs() {
        Lab thirdLab = new Lab(
                "ddia-cap-03-lab-03",
                3,
                "modelagem-dimensional-olap",
                "Modelagem Dimensional OLAP",
                "Sumário",
                List.of(),
                EngineType.POSTGRES,
                "tbl_lab",
                "DROP TABLE IF EXISTS fato_vendas;",
                List.of()
        );

        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(catalogRepository.findLabById("ddia-cap-03-lab-03")).thenReturn(Optional.of(thirdLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        // 1. Provisiona Lab 1 -> deve resetar para Lab 1
        provisioningService.provisionLab(sessionId, validLabId);
        verify(databaseResetter, times(1)).resetDatabase(EngineType.POSTGRES, mockLab);

        // 2. Transita para Lab 3 (outro lab do mesmo motor POSTGRES) -> deve resetar para Lab 3
        provisioningService.provisionLab(sessionId, "ddia-cap-03-lab-03");
        verify(databaseResetter, times(1)).resetDatabase(EngineType.POSTGRES, thirdLab);
    }
}


