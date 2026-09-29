package com.dataintensive.lab.query;

import java.util.List;
import java.util.Map;

public record QueryResult(
    boolean success,
    String message,
    List<String> columns,
    List<Map<String, Object>> rows,
    int rowCount,
    long executionTimeMs,
    String errorMessage
) {
    public static QueryResult ok(List<String> columns, List<Map<String, Object>> rows, long executionTimeMs) {
        return new QueryResult(true, "Consulta executada com sucesso.", columns, rows, rows.size(), executionTimeMs, null);
    }

    public static QueryResult update(int affectedRows, long executionTimeMs) {
        return new QueryResult(true, "Comando executado com sucesso. Linhas afetadas: " + affectedRows, List.of(), List.of(), affectedRows, executionTimeMs, null);
    }

    public static QueryResult error(String error, long executionTimeMs) {
        return new QueryResult(false, "Erro ao executar consulta.", List.of(), List.of(), 0, executionTimeMs, error);
    }
}
