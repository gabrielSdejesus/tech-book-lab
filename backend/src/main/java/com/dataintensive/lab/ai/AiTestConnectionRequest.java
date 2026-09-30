package com.dataintensive.lab.ai;

import jakarta.validation.constraints.NotBlank;

public record AiTestConnectionRequest(
    @NotBlank(message = "O provedor de IA (provider) é obrigatório")
    String provider,

    String apiKey,
    String modelOverride
) {}
