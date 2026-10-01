package com.dataintensive.lab.query;

import com.dataintensive.lab.domain.EngineType;

public interface QueryEngineExecutor {
    EngineType getEngineType();
    QueryResult execute(String query, long startTime);

    default String getEngineIdentifier() {
        return getEngineType().name();
    }
}
