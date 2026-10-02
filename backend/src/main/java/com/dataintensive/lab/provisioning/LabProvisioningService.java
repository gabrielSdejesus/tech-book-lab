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
import java.util.Optional;
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

    private String toKey(SessionId sessionId, String labId) {
        return sessionId.value() + ":" + labId;
    }

    public LabSession provisionLab(SessionId sessionId, String labId) {
        return provisionLab(sessionId, labId, null);
    }

    public LabSession provisionLab(SessionId sessionId, String labId, String challengeId) {
        Lab lab = catalogRepository.findLabById(labId)
                .orElseThrow(() -> new LabNotFoundException(labId));

        EngineType requiredEngine = resolveRequiredEngine(lab, challengeId);
        int port = resolveDefaultPort(requiredEngine);
        String sessionKey = toKey(sessionId, labId);
        String containerName = DockerComposeLabManager.resolveContainerName(requiredEngine);

        LabSession currentSession = activeSessions.get(sessionKey);
        if (currentSession != null && currentSession.status() == LabEnvironmentStatus.READY) {
            boolean differentEngine = currentSession.engineType() != requiredEngine;

            if (differentEngine) {
                log.info("Sessão {} alternou motor no laboratório {} ({} -> {}). Desprovisionando motor anterior...",
                        sessionId, labId, currentSession.engineType(), requiredEngine);
                containerManager.stopEngine(currentSession.engineType());
                activeSessions.put(sessionKey, currentSession.withStatus(LabEnvironmentStatus.STOPPED));
            } else {
                // Mesmo motor já em execução e saudável
                if (containerManager.isEngineHealthy(requiredEngine, currentSession.allocatedPort())) {
                    LabSession updated = new LabSession(
                            sessionId,
                            lab.id(),
                            challengeId,
                            containerName,
                            requiredEngine,
                            LabEnvironmentStatus.READY,
                            Instant.now(),
                            currentSession.allocatedPort(),
                            null
                    );
                    activeSessions.put(sessionKey, updated);
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
                containerName,
                requiredEngine,
                status,
                Instant.now(),
                port,
                null
        );

        activeSessions.put(sessionKey, session);
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
        String sessionKey = toKey(sessionId, labId);
        LabSession session = activeSessions.get(sessionKey);

        if (session == null || !session.labId().equals(labId) || session.engineType() != requiredEngine) {
            String containerName = DockerComposeLabManager.resolveContainerName(requiredEngine);
            return new LabSession(
                    sessionId,
                    lab.id(),
                    challengeId,
                    containerName,
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
                activeSessions.put(sessionKey, session);
            }
        }

        return session;
    }

    public Optional<LabSession> getSession(SessionId sessionId, String labId) {
        return Optional.ofNullable(activeSessions.get(toKey(sessionId, labId)));
    }

    public LabHeartbeatResult heartbeat(SessionId sessionId, String labId) {
        String sessionKey = toKey(sessionId, labId);
        LabSession session = activeSessions.get(sessionKey);
        if (session == null || session.status() == LabEnvironmentStatus.STOPPED) {
            throw new SessionExpiredException(sessionId.value());
        }

        Instant now = Instant.now();
        LabSession updated = session.withHeartbeat(now);
        activeSessions.put(sessionKey, updated);

        return new LabHeartbeatResult("ACK", labId, 60, now);
    }

    public LabSession teardown(SessionId sessionId, String labId) {
        String sessionKey = toKey(sessionId, labId);
        LabSession session = activeSessions.get(sessionKey);
        if (session != null) {
            containerManager.stopEngine(session.engineType());
            LabSession stopped = session.withStatus(LabEnvironmentStatus.STOPPED);
            activeSessions.put(sessionKey, stopped);
            return stopped;
        }

        Lab lab = catalogRepository.findLabById(labId)
                .orElseThrow(() -> new LabNotFoundException(labId));
        String containerName = DockerComposeLabManager.resolveContainerName(lab.engineType());
        return new LabSession(sessionId, lab.id(), null, containerName, lab.engineType(), LabEnvironmentStatus.STOPPED, Instant.now(), resolveDefaultPort(lab.engineType()), null);
    }

    public int cleanupInactiveSessions(Duration ttl) {
        Instant now = Instant.now();
        int stoppedCount = 0;

        for (Map.Entry<String, LabSession> entry : activeSessions.entrySet()) {
            LabSession session = entry.getValue();
            if (session.status() == LabEnvironmentStatus.READY || session.status() == LabEnvironmentStatus.PROVISIONING) {
                if (Duration.between(session.lastHeartbeatAt(), now).compareTo(ttl) >= 0) {
                    log.info("Sessão {} (lab {}) inativa há mais de {} segundos. Encerrando contêiner de laboratório {}...",
                            session.sessionId(), session.labId(), ttl.toSeconds(), session.engineType());
                    containerManager.stopEngine(session.engineType());
                    activeSessions.put(entry.getKey(), session.withStatus(LabEnvironmentStatus.STOPPED));
                    stoppedCount++;
                }
            }
        }

        return stoppedCount;
    }

    public void overrideSessionLastHeartbeatForTest(SessionId sessionId, Instant time) {
        for (Map.Entry<String, LabSession> entry : activeSessions.entrySet()) {
            if (entry.getValue().sessionId().equals(sessionId)) {
                activeSessions.put(entry.getKey(), entry.getValue().withHeartbeat(time));
            }
        }
    }

    public void overrideSessionLastHeartbeatForTest(SessionId sessionId, String labId, Instant time) {
        String sessionKey = toKey(sessionId, labId);
        LabSession session = activeSessions.get(sessionKey);
        if (session != null) {
            activeSessions.put(sessionKey, session.withHeartbeat(time));
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
