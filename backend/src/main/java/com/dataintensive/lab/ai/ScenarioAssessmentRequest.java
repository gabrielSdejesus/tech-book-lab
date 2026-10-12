package com.dataintensive.lab.ai;

public record ScenarioAssessmentRequest(
        String userStateJson,
        String userReflection,
        String apiKeyOverride,
        String providerOverride,
        String modelOverride,
        String language
) {
    public ScenarioAssessmentRequest(String userStateJson, String userReflection) {
        this(userStateJson, userReflection, null, null, null, "pt");
    }
}
