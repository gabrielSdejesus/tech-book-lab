package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class EngineLifecycleCoordinator {

    public void scheduleTeardown(EngineType engine, Duration gracePeriod, Runnable teardownAction) {
        throw new UnsupportedOperationException("TDD Red: Not implemented yet");
    }

    public boolean cancelScheduledTeardown(EngineType engine) {
        throw new UnsupportedOperationException("TDD Red: Not implemented yet");
    }

    public boolean isTeardownPending(EngineType engine) {
        throw new UnsupportedOperationException("TDD Red: Not implemented yet");
    }

    public void executeExclusive(EngineType engine, Runnable action) {
        throw new UnsupportedOperationException("TDD Red: Not implemented yet");
    }

    public void shutdown() {
        // no-op for now
    }
}
