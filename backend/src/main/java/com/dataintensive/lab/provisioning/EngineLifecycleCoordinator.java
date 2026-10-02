package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class EngineLifecycleCoordinator {

    private static final Logger log = LoggerFactory.getLogger(EngineLifecycleCoordinator.class);

    private final ScheduledExecutorService scheduler;
    private final Map<EngineType, ScheduledFuture<?>> pendingTeardowns = new ConcurrentHashMap<>();
    private final Map<EngineType, ExecutorService> engineExecutors = new ConcurrentHashMap<>();

    public EngineLifecycleCoordinator() {
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "engine-lifecycle-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public void scheduleTeardown(EngineType engine, Duration gracePeriod, Runnable teardownAction) {
        cancelScheduledTeardown(engine);

        log.info("Agendando teardown do motor {} com grace period de {}ms...", engine, gracePeriod.toMillis());

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            pendingTeardowns.remove(engine);
            log.info("Grace period de teardown para motor {} expirado. Executando teardown...", engine);
            executeExclusive(engine, teardownAction);
        }, gracePeriod.toMillis(), TimeUnit.MILLISECONDS);

        pendingTeardowns.put(engine, future);
    }

    public boolean cancelScheduledTeardown(EngineType engine) {
        ScheduledFuture<?> future = pendingTeardowns.remove(engine);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(false);
            if (cancelled) {
                log.info("Teardown agendado do motor {} foi cancelado com sucesso por nova solicitação.", engine);
            }
            return cancelled;
        }
        return false;
    }

    public boolean isTeardownPending(EngineType engine) {
        ScheduledFuture<?> future = pendingTeardowns.get(engine);
        return future != null && !future.isDone();
    }

    public Future<?> executeExclusive(EngineType engine, Runnable action) {
        ExecutorService executor = engineExecutors.computeIfAbsent(engine, e -> Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "engine-coord-" + e.name().toLowerCase());
            t.setDaemon(true);
            return t;
        }));

        return executor.submit(() -> {
            try {
                action.run();
            } catch (Exception e) {
                log.error("Erro ao executar ação exclusiva no motor {}: {}", engine, e.getMessage(), e);
                throw e;
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        pendingTeardowns.values().forEach(f -> f.cancel(false));
        pendingTeardowns.clear();

        scheduler.shutdownNow();
        engineExecutors.values().forEach(ExecutorService::shutdownNow);
        engineExecutors.clear();
    }
}
