package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;

public interface LabContainerManager {
    void startEngine(EngineType engine);
    void stopEngine(EngineType engine);
    boolean isEngineHealthy(EngineType engine, int port);
}
