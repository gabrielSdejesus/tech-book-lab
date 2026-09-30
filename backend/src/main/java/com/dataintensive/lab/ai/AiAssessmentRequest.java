package com.dataintensive.lab.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiAssessmentRequest(
    @NotBlank(message = "O identificador do laboratório (labId) é obrigatório")
    String labId,

    @NotBlank(message = "O identificador do desafio (challengeId) é obrigatório")
    String challengeId,

    @NotBlank(message = "A consulta do usuário é obrigatória")
    @Size(max = 10000, message = "A consulta do usuário não pode exceder 10.000 caracteres")
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
