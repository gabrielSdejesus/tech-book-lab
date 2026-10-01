package com.dataintensive.lab.query;

import com.dataintensive.lab.domain.DomainValidationException;
import com.dataintensive.lab.domain.EngineType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QueryEngineRegistry {

    private final Map<EngineType, QueryEngineExecutor> executors = new ConcurrentHashMap<>();

    public QueryEngineRegistry(List<QueryEngineExecutor> executorList) {
        if (executorList != null) {
            for (QueryEngineExecutor executor : executorList) {
                register(executor);
            }
        }
    }

    public void register(QueryEngineExecutor executor) {
        executors.put(executor.getEngineType(), executor);
    }

    public Optional<QueryEngineExecutor> findExecutor(EngineType engineType) {
        if (engineType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(executors.get(engineType));
    }

    public QueryEngineExecutor getExecutor(EngineType engineType) {
        return findExecutor(engineType)
                .orElseThrow(() -> new DomainValidationException(
                        "Motor de banco de dados não suportado: " + engineType
                ));
    }

    public QueryEngineExecutor getExecutor(String engineName) {
        if (engineName == null || engineName.isBlank()) {
            throw new DomainValidationException("Identificador do motor não fornecido.");
        }
        for (Map.Entry<EngineType, QueryEngineExecutor> entry : executors.entrySet()) {
            if (entry.getKey().name().equalsIgnoreCase(engineName.trim())) {
                return entry.getValue();
            }
        }
        throw new DomainValidationException("Motor de banco de dados não suportado: " + engineName);
    }

    public List<EngineType> getSupportedEngines() {
        return List.copyOf(executors.keySet());
    }
}
