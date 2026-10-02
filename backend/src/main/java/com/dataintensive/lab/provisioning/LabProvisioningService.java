package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.catalog.CatalogRepository;
import com.dataintensive.lab.domain.Challenge;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final LabProvisioningProperties properties;
    private final java.time.Clock clock;
    private final EngineLifecycleCoordinator lifecycleCoordinator;
    private final Optional<LabDatabaseResetter> databaseResetter;
    private final Map<String, LabSession> activeSessions = new ConcurrentHashMap<>();

    @Autowired
    public LabProvisioningService(CatalogRepository catalogRepository,
                                  LabContainerManager containerManager,
                                  LabProvisioningProperties properties,
                                  Optional<java.time.Clock> clock,
                                  Optional<EngineLifecycleCoordinator> lifecycleCoordinator,
                                  Optional<LabDatabaseResetter> databaseResetter) {
        this.catalogRepository = catalogRepository;
        this.containerManager = containerManager;
        this.properties = properties != null ? properties : new LabProvisioningProperties();
        this.clock = clock.orElse(java.time.Clock.systemUTC());
        this.lifecycleCoordinator = lifecycleCoordinator.orElseGet(EngineLifecycleCoordinator::new);
        this.databaseResetter = databaseResetter != null ? databaseResetter : Optional.empty();
    }

    public LabProvisioningService(CatalogRepository catalogRepository,
                                  LabContainerManager containerManager,
                                  LabProvisioningProperties properties,
                                  java.time.Clock clock,
                                  EngineLifecycleCoordinator lifecycleCoordinator,
                                  LabDatabaseResetter databaseResetter) {
        this(catalogRepository, containerManager, properties, Optional.ofNullable(clock), Optional.ofNullable(lifecycleCoordinator), Optional.ofNullable(databaseResetter));
    }

    public LabProvisioningService(CatalogRepository catalogRepository,
                                  LabContainerManager containerManager,
                                  LabProvisioningProperties properties,
                                  java.time.Clock clock,
                                  EngineLifecycleCoordinator lifecycleCoordinator) {
        this(catalogRepository, containerManager, properties, Optional.ofNullable(clock), Optional.ofNullable(lifecycleCoordinator), Optional.empty());
    }

    public LabProvisioningService(CatalogRepository catalogRepository,
                                  LabContainerManager containerManager,
                                  LabProvisioningProperties properties,
                                  java.time.Clock clock) {
        this(catalogRepository, containerManager, properties, clock, new EngineLifecycleCoordinator(), null);
    }

    public LabProvisioningService(CatalogRepository catalogRepository,
                                  LabContainerManager containerManager,
                                  LabProvisioningProperties properties) {
        this(catalogRepository, containerManager, properties, java.time.Clock.systemUTC(), new EngineLifecycleCoordinator(), null);
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

        // Cancela qualquer teardown agendado para o motor requisitado (Debounce inteligente)
        boolean teardownCancelled = lifecycleCoordinator.cancelScheduledTeardown(requiredEngine);

        LabSession currentSession = activeSessions.get(sessionKey);
        if (currentSession != null && currentSession.status() == LabEnvironmentStatus.READY) {
            boolean differentEngine = currentSession.engineType() != requiredEngine;

            if (differentEngine) {
                log.info("Sessão {} alternou motor no laboratório {} ({} -> {}). Desprovisionando motor anterior...",
                        sessionId, labId, currentSession.engineType(), requiredEngine);
                lifecycleCoordinator.executeExclusive(currentSession.engineType(), () -> containerManager.stopEngine(currentSession.engineType()));
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
                            clock.instant(),
                            currentSession.allocatedPort(),
                            null
                    );
                    activeSessions.put(sessionKey, updated);
                    return updated;
                }
            }
        }

        // Se o teardown foi cancelado a tempo e o contêiner já está saudável, mantém READY diretamente
        if (teardownCancelled && containerManager.isEngineHealthy(requiredEngine, port)) {
            LabSession session = new LabSession(
                    sessionId,
                    lab.id(),
                    challengeId,
                    containerName,
                    requiredEngine,
                    LabEnvironmentStatus.READY,
                    clock.instant(),
                    port,
                    null
            );
            activeSessions.put(sessionKey, session);
            return session;
        }

        lifecycleCoordinator.executeExclusive(requiredEngine, () -> containerManager.startEngine(requiredEngine));

        boolean healthy = containerManager.isEngineHealthy(requiredEngine, port);
        LabEnvironmentStatus status = healthy ? LabEnvironmentStatus.READY : LabEnvironmentStatus.PROVISIONING;


        LabSession session = new LabSession(
                sessionId,
                lab.id(),
                challengeId,
                containerName,
                requiredEngine,
                status,
                clock.instant(),
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
                    clock.instant(),
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

        Instant now = clock.instant();
        LabSession updated = session.withHeartbeat(now);
        activeSessions.put(sessionKey, updated);

        long ttlRemaining = (long) properties.getInactivityTimeoutMinutes() * 60L;
        return new LabHeartbeatResult("ACK", labId, ttlRemaining, now);
    }

    public int cleanupInactiveSessions() {
        return cleanupInactiveSessions(Duration.ofMinutes(properties.getInactivityTimeoutMinutes()));
    }

    public LabSession teardown(SessionId sessionId, String labId) {
        String sessionKey = toKey(sessionId, labId);
        LabSession session = activeSessions.get(sessionKey);
        if (session != null) {
            Duration gracePeriod = Duration.ofSeconds(properties.getTeardownGracePeriodSeconds());
            lifecycleCoordinator.scheduleTeardown(session.engineType(), gracePeriod, () -> {
                containerManager.stopEngine(session.engineType());
            });
            LabSession stopped = session.withStatus(LabEnvironmentStatus.STOPPED);
            activeSessions.put(sessionKey, stopped);
            return stopped;
        }

        Lab lab = catalogRepository.findLabById(labId)
                .orElseThrow(() -> new LabNotFoundException(labId));
        String containerName = DockerComposeLabManager.resolveContainerName(lab.engineType());
        return new LabSession(sessionId, lab.id(), null, containerName, lab.engineType(), LabEnvironmentStatus.STOPPED, clock.instant(), resolveDefaultPort(lab.engineType()), null);
    }

    public int cleanupInactiveSessions(Duration ttl) {
        Instant now = clock.instant();
        int stoppedCount = 0;

        for (Map.Entry<String, LabSession> entry : activeSessions.entrySet()) {
            LabSession session = entry.getValue();
            if (session.status() == LabEnvironmentStatus.READY || session.status() == LabEnvironmentStatus.PROVISIONING) {
                if (Duration.between(session.lastHeartbeatAt(), now).compareTo(ttl) >= 0) {
                    log.info("Sessão {} (lab {}) inativa há mais de {} segundos. Encerrando contêiner de laboratório {}...",
                            session.sessionId(), session.labId(), ttl.toSeconds(), session.engineType());
                    lifecycleCoordinator.executeExclusive(session.engineType(), () -> containerManager.stopEngine(session.engineType()));
                    activeSessions.put(entry.getKey(), session.withStatus(LabEnvironmentStatus.STOPPED));
                    stoppedCount++;
                }
            }
        }

        return stoppedCount;
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
