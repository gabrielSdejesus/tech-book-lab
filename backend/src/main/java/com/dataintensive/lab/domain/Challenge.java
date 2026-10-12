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
    String savedReflection,
    List<String> guidelines,
    String reflectionPrompt,
    String expectedReflection
) {
    public Challenge(
        String id,
        int order,
        String title,
        String description,
        String scenario,
        String starterTemplate,
        String savedCode,
        List<String> guidelines,
        String reflectionPrompt,
        String expectedReflection
    ) {
        this(id, order, title, description, scenario, starterTemplate, savedCode, null, guidelines, reflectionPrompt, expectedReflection);
    }

    public Challenge(
        String id,
        int order,
        String title,
        String description,
        String scenario,
        String starterTemplate,
        String savedCode,
        List<String> guidelines,
        String reflectionPrompt
    ) {
        this(id, order, title, description, scenario, starterTemplate, savedCode, null, guidelines, reflectionPrompt, null);
    }

    public Challenge(
        String id,
        int order,
        String title,
        String description,
        String scenario,
        String starterTemplate,
        List<String> guidelines,
        String reflectionPrompt,
        String expectedReflection
    ) {
        this(id, order, title, description, scenario, starterTemplate, null, null, guidelines, reflectionPrompt, expectedReflection);
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
        this(id, order, title, description, scenario, starterTemplate, null, null, guidelines, reflectionPrompt, null);
    }
}
