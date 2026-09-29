package com.dataintensive.lab.ai;

public record AiTestConnectionRequest(
    String provider,
    String apiKey,
    String modelOverride
) {}
