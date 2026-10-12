package com.dataintensive.lab.domain;

public record InteractiveScenario(
    String id,
    String labId,
    ScenarioType scenarioType,
    Difficulty difficulty,
    int orderIndex,
    String title,
    String description,
    String contextBriefing,
    String expectedReflection,
    String constraintsJson,
    String initialStateJson,
    String validationRulesJson,
    String savedUserStateJson,
    String savedUserReflection,
    String status
) {}
