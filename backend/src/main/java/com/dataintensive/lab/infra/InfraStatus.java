package com.dataintensive.lab.infra;

public record InfraStatus(
    boolean postgresReady,
    String postgresMessage,
    boolean neo4jReady,
    String neo4jMessage,
    long timestamp
) {}
