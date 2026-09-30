package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.catalog.CatalogRepository;
import com.dataintensive.lab.domain.Challenge;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LabProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(LabProvisioningService.class);

    private final CatalogRepository catalogRepository;
    private final LabContainerManager containerManager;
    private final Map<String, LabSession> activeSessions = new ConcurrentHashMap<>();

    public LabProvisioningService(CatalogRepository catalogRepository, LabContainerManager containerManager) {
        this.catalogRepository = catalogRepository;
        this.containerManager = containerManager;
    }

    public LabSession provisionLab(SessionId sessionId, String labId) {
        return provisionLab(sessionId, labId, null);
    }

    public LabSession provisionLab(SessionId sessionId, String labId, String challengeId) {
        Lab lab = catalogRepository.findLabById(labId)
                .orElseThrow(() -> new LabNotFoundException(labId));

        EngineType requiredEngine = resolveRequiredEngine(lab, challengeId);
        int port = resolveDefaultPort(requiredEngine);

        // Anti-DoS Invariant: Cota de 1 motor ativo por sessão
        LabSession currentSession = activeSessions.get(sessionId.value());
        if (currentSession != null && currentSession.status() == LabEnvironmentStatus.READY) {
            boolean differentLab = !currentSession.labId().equals(labId);
            boolean differentEngine = currentSession.engineType() != requiredEngine;

            if (differentLab || differentEngine) {
                log.info("Sessão {} alternou ambiente (Lab: {}, Motor: {} -> {}). Desprovisionando motor anterior {}...",
                        sessionId, labId, currentSession.engineType(), requiredEngine, currentSession.engineType());
                containerManager.stopEngine(currentSession.engineType());
                activeSessions.put(sessionId.value(), currentSession.withStatus(LabEnvironmentStatus.STOPPED));
            } else {
                // Mesmo motor já em execução e saudável
                if (containerManager.isEngineHealthy(requiredEngine, port)) {
                    LabSession updated = new LabSession(
                            sessionId,
                            lab.id(),
                            challengeId,
                            requiredEngine,
                            LabEnvironmentStatus.READY,
                            Instant.now(),
                            port,
                            null
                    );
                    activeSessions.put(sessionId.value(), updated);
                    return updated;
                }
            }
        }

        containerManager.startEngine(requiredEngine);

        boolean healthy = containerManager.isEngineHealthy(requiredEngine, port);
        LabEnvironmentStatus status = healthy ? LabEnvironmentStatus.READY : LabEnvironmentStatus.PROVISIONING;

        LabSession session = new LabSession(
                sessionId,
                lab.id(),
                challengeId,
                requiredEngine,
                status,
                Instant.now(),
                port,
                null
        );

        activeSessions.put(sessionId.value(), session);
        return session;
    }

    public LabSession getLabStatus(SessionId sessionId, String labId) {
        return getLabStatus(sessionId, labId, null);
    }

    public LabSession getLabStatus(SessionId sessionId, String labId, String challengeId) {
        Lab lab = catalogRepository.findLabById(labId)
                .orElseThrow(() -> new LabNotFoundException(labId));

        EngineType requiredEngine = resolveRequiredEngine(lab, challengeId);
        int port = resolveDefaultPort(requiredEngine);
        LabSession session = activeSessions.get(sessionId.value());

        if (session == null || !session.labId().equals(labId) || session.engineType() != requiredEngine) {
            return new LabSession(
                    sessionId,
                    lab.id(),
                    challengeId,
                    requiredEngine,
                    LabEnvironmentStatus.NOT_PROVISIONED,
                    Instant.now(),
                    port,
                    null
            );
        }

        if (session.status() == LabEnvironmentStatus.PROVISIONING) {
            if (containerManager.isEngineHealthy(session.engineType(), session.allocatedPort())) {
                session = session.withStatus(LabEnvironmentStatus.READY);
                activeSessions.put(sessionId.value(), session);
            }
        }

        return session;
    }

    public LabHeartbeatResult heartbeat(SessionId sessionId, String labId) {
        LabSession session = activeSessions.get(sessionId.value());
        if (session == null || session.status() == LabEnvironmentStatus.STOPPED) {
            throw new SessionExpiredException(sessionId.value());
        }

        Instant now = Instant.now();
        LabSession updated = session.withHeartbeat(now);
        activeSessions.put(sessionId.value(), updated);

        return new LabHeartbeatResult("ACK", labId, 900, now);
    }

    public LabSession teardown(SessionId sessionId, String labId) {
        LabSession session = activeSessions.get(sessionId.value());
        if (session != null) {
            containerManager.stopEngine(session.engineType());
            LabSession stopped = session.withStatus(LabEnvironmentStatus.STOPPED);
            activeSessions.put(sessionId.value(), stopped);
            return stopped;
        }

        Lab lab = catalogRepository.findLabById(labId)
                .orElseThrow(() -> new LabNotFoundException(labId));
        return new LabSession(sessionId, lab.id(), lab.engineType(), LabEnvironmentStatus.STOPPED, Instant.now(), resolveDefaultPort(lab.engineType()), null);
    }

    public int cleanupInactiveSessions(Duration ttl) {
        Instant now = Instant.now();
        int stoppedCount = 0;

        for (Map.Entry<String, LabSession> entry : activeSessions.entrySet()) {
            LabSession session = entry.getValue();
            if (session.status() == LabEnvironmentStatus.READY || session.status() == LabEnvironmentStatus.PROVISIONING) {
                if (Duration.between(session.lastHeartbeatAt(), now).compareTo(ttl) >= 0) {
                    log.info("Sessão {} inativa há mais de {} minutos. Encerrando contêiner de laboratório {}...",
                            session.sessionId(), ttl.toMinutes(), session.engineType());
                    containerManager.stopEngine(session.engineType());
                    activeSessions.put(entry.getKey(), session.withStatus(LabEnvironmentStatus.STOPPED));
                    stoppedCount++;
                }
            }
        }

        return stoppedCount;
    }

    public void overrideSessionLastHeartbeatForTest(SessionId sessionId, Instant time) {
        LabSession session = activeSessions.get(sessionId.value());
        if (session != null) {
            activeSessions.put(sessionId.value(), session.withHeartbeat(time));
        }
    }

    private EngineType resolveRequiredEngine(Lab lab, String challengeId) {
        if (challengeId != null && lab.challenges() != null) {
            for (Challenge ch : lab.challenges()) {
                if (ch.id().equals(challengeId)) {
                    if (ch.engineType() != null) {
                        return ch.engineType();
                    }
                    break;
                }
            }
        }
        return lab.engineType();
    }

    private int resolveDefaultPort(EngineType engineType) {
        return engineType == EngineType.NEO4J ? 7687 : 5432;
    }
}
