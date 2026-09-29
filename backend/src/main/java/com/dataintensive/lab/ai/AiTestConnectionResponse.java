package com.dataintensive.lab.ai;

public record AiTestConnectionResponse(
    boolean valid,
    String message,
    String model,
    long latencyMs
) {}
