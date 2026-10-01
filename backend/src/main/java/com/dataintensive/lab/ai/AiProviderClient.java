package com.dataintensive.lab.ai;

public interface AiProviderClient {
    String getProviderId();
    AiProviderInfo getInfo();
    AiTestConnectionResponse testConnection(AiTestConnectionRequest request);
    AiAssessmentResponse assess(String prompt, String apiKey, String modelOverride, boolean isModelOverridden) throws Exception;

    default boolean isConfigured(String apiKeyOverride) {
        return true;
    }
}
