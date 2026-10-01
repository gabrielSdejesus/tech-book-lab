package com.dataintensive.lab.ai;

import java.util.List;

public record AiProviderInfo(
    String id,
    String name,
    String description,
    boolean requiresApiKey,
    String apiKeyPlaceholder,
    String helpUrl,
    String defaultModel,
    List<AiModelInfo> models
) {}
