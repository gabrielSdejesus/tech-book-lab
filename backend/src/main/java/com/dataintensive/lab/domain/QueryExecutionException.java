package com.dataintensive.lab.domain;

public class QueryExecutionException extends RuntimeException {

    private final long executionTimeMs;

    public QueryExecutionException(String message, long executionTimeMs) {
        super(message);
        this.executionTimeMs = executionTimeMs;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
}
