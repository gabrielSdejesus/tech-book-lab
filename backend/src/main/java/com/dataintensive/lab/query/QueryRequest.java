package com.dataintensive.lab.query;

import com.dataintensive.lab.domain.EngineType;

public record QueryRequest(
    String query,
    EngineType engineType,
    String labId
) {}
