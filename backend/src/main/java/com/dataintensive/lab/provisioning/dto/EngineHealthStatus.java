package com.dataintensive.lab.provisioning.dto;

public record EngineHealthStatus(
    boolean healthy,
    int port,
    String serviceName,
    String status
) {
    public static EngineHealthStatus up(int port, String serviceName) {
        return new EngineHealthStatus(true, port, serviceName, "UP");
    }

    public static EngineHealthStatus down(int port, String serviceName) {
        return new EngineHealthStatus(false, port, serviceName, "DOWN");
    }
}
