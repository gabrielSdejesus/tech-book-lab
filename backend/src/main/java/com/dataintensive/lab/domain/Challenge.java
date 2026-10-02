package com.dataintensive.lab.domain;

import java.util.List;

public record Challenge(
    String id,
    int order,
    String title,
    String description,
    String scenario,
    String starterTemplate,
    String savedCode,
    List<String> guidelines,
    String reflectionPrompt,
    EngineType engineType
) {
    public Challenge(
        String id,
        int order,
        String title,
        String description,
        String scenario,
        String starterTemplate,
        List<String> guidelines,
        String reflectionPrompt,
        EngineType engineType
    ) {
        this(id, order, title, description, scenario, starterTemplate, null, guidelines, reflectionPrompt, engineType);
    }

    public Challenge(
        String id,
        int order,
        String title,
        String description,
        String scenario,
        String starterTemplate,
        List<String> guidelines,
        String reflectionPrompt
    ) {
        this(id, order, title, description, scenario, starterTemplate, null, guidelines, reflectionPrompt, null);
    }
}
