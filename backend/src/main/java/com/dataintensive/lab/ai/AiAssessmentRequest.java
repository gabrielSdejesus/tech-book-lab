package com.dataintensive.lab.ai;

public record AiAssessmentRequest(
    String labId,
    String challengeId,
    String userQuery,
    String executionSummary,
    String userReflection,
    String apiKeyOverride,
    String providerOverride,
    String modelOverride
) {}
