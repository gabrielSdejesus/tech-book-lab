package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.AssessmentLanguage;

public interface AiProviderClient {
    String getProviderId();

    default AiProviderInfo getInfo() {
        return getInfo(AssessmentLanguage.PT);
    }

    default AiProviderInfo getInfo(String language) {
        try {
            return getInfo(AssessmentLanguage.from(language));
        } catch (Exception e) {
            return getInfo(AssessmentLanguage.PT);
        }
    }

    AiProviderInfo getInfo(AssessmentLanguage language);

    AiTestConnectionResponse testConnection(AiTestConnectionRequest request);
    AiAssessmentResponse assess(String prompt, String apiKey, String modelOverride, boolean isModelOverridden) throws Exception;

    default boolean isConfigured(String apiKeyOverride) {
        return true;
    }
}
