package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;

public interface LabDatabaseResetter {
    void resetDatabase(EngineType engineType, Lab lab);
}
