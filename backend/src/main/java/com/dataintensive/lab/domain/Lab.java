package com.dataintensive.lab.domain;

import java.util.List;

public record Lab(
    String id,
    int number,
    String slug,
    String title,
    String summary,
    List<String> keyConcepts,
    EngineType engineType,
    String databaseName,
    String resetSchemaSql,
    List<Challenge> challenges
) {}
