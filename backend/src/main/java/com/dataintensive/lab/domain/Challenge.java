package com.dataintensive.lab.domain;

import java.util.List;

public record Challenge(
    String id,
    int order,
    String title,
    String description,
    String scenario,
    String starterTemplate,
    List<String> guidelines,
    String reflectionPrompt
) {}
