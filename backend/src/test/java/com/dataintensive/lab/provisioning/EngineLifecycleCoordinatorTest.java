package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EngineLifecycleCoordinatorTest {

    private EngineLifecycleCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new EngineLifecycleCoordinator();
    }

    @AfterEach
    void tearDown() {
        coordinator.shutdown();
    }

    @Test
    @DisplayName("Deve agendar e executar o teardown após a expiração do grace period")
    void shouldScheduleAndExecuteTeardownAfterGracePeriod() throws InterruptedException {
        AtomicBoolean executed = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        coordinator.scheduleTeardown(EngineType.POSTGRES, Duration.ofMillis(50), () -> {
            executed.set(true);
            latch.countDown();
        });

        assertThat(coordinator.isTeardownPending(EngineType.POSTGRES)).isTrue();
        assertThat(executed.get()).isFalse();

        boolean completed = latch.await(500, TimeUnit.MILLISECONDS);
        assertThat(completed).isTrue();
        assertThat(executed.get()).isTrue();
        assertThat(coordinator.isTeardownPending(EngineType.POSTGRES)).isFalse();
    }

    @Test
    @DisplayName("Deve cancelar o teardown agendado quando cancelamento for solicitado dentro do grace period")
    void shouldCancelScheduledTeardownWhenCancelRequested() throws InterruptedException {
        AtomicBoolean executed = new AtomicBoolean(false);

        coordinator.scheduleTeardown(EngineType.POSTGRES, Duration.ofMillis(100), () -> executed.set(true));

        assertThat(coordinator.isTeardownPending(EngineType.POSTGRES)).isTrue();

        boolean cancelled = coordinator.cancelScheduledTeardown(EngineType.POSTGRES);
        assertThat(cancelled).isTrue();
        assertThat(coordinator.isTeardownPending(EngineType.POSTGRES)).isFalse();

        Thread.sleep(150);
        assertThat(executed.get()).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false ao tentar cancelar teardown quando nenhum estiver pendente")
    void shouldReturnFalseWhenNoTeardownPending() {
        boolean cancelled = coordinator.cancelScheduledTeardown(EngineType.POSTGRES);
        assertThat(cancelled).isFalse();
    }

    @Test
    @DisplayName("Deve executar ações de forma estritamente sequencial e ordenada para o mesmo motor")
    void shouldExecuteExclusiveActionsSequentiallyPerEngine() throws InterruptedException {
        List<String> executionLog = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(2);

        coordinator.executeExclusive(EngineType.POSTGRES, () -> {
            try {
                Thread.sleep(80);
                executionLog.add("STOP_DONE");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        coordinator.executeExclusive(EngineType.POSTGRES, () -> {
            executionLog.add("START_DONE");
            latch.countDown();
        });

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(executionLog).containsExactly("STOP_DONE", "START_DONE");
    }

    @Test
    @DisplayName("Deve renovar o debounce caso um novo teardown seja agendado para o mesmo motor")
    void shouldDebounceWhenNewTeardownScheduledForSameEngine() throws InterruptedException {
        AtomicInteger runCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        coordinator.scheduleTeardown(EngineType.POSTGRES, Duration.ofMillis(80), runCount::incrementAndGet);
        Thread.sleep(30);

        // Renova o debounce com nova ação
        coordinator.scheduleTeardown(EngineType.POSTGRES, Duration.ofMillis(80), () -> {
            runCount.incrementAndGet();
            latch.countDown();
        });

        boolean completed = latch.await(500, TimeUnit.MILLISECONDS);
        assertThat(completed).isTrue();
        assertThat(runCount.get()).isEqualTo(1);
    }
}
