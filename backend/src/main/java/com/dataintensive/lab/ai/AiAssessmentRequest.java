package com.dataintensive.lab.ai;

public record AiAssessmentRequest(
    String labId,
    String challengeId,
    String userQuery,
    String executionSummary,
    String userReflection,
    String apiKeyOverride,
    String providerOverride,
    String modelOverride,
    String language
) {
    public AiAssessmentRequest(
        String labId,
        String challengeId,
        String userQuery,
        String executionSummary,
        String userReflection,
        String apiKeyOverride,
        String providerOverride,
        String modelOverride
    ) {
        this(labId, challengeId, userQuery, executionSummary, userReflection, apiKeyOverride, providerOverride, modelOverride, "pt");
    }
}

