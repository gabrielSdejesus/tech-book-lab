package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;

public interface LabContainerManager {
    void startEngine(EngineType engine);
    void stopEngine(EngineType engine);
    boolean isEngineHealthy(EngineType engine, int port);

    default int startIsolatedContainer(String containerName, EngineType engine) {
        startEngine(engine);
        return engine == EngineType.POSTGRES ? 5432 : 7687;
    }

    default void stopIsolatedContainer(String containerName) {
        // Default fallback: do nothing or stop standard engine
    }
}
