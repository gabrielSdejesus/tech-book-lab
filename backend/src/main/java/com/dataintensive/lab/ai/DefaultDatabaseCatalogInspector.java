package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.query.QueryEngineExecutor;
import com.dataintensive.lab.query.QueryEngineRegistry;
import com.dataintensive.lab.query.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultDatabaseCatalogInspector implements DatabaseCatalogInspector {

    private static final Logger log = LoggerFactory.getLogger(DefaultDatabaseCatalogInspector.class);
    private static final long DEFAULT_TIMEOUT_MS = 5000L;

    private final QueryEngineRegistry engineRegistry;

    public DefaultDatabaseCatalogInspector(QueryEngineRegistry engineRegistry) {
        this.engineRegistry = engineRegistry;
    }

    @Override
    public boolean tableExists(EngineType engineType, String tableName) {
        if (engineType == null || tableName == null || tableName.isBlank()) {
            return false;
        }
        try {
            QueryEngineExecutor executor = engineRegistry.getExecutor(engineType);
            if (executor == null) {
                return false;
            }
            String cleanTable = cleanIdentifier(tableName);
            String sql = "SELECT COUNT(*) AS cnt FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '" + cleanTable + "'";
            QueryResult result = executor.execute(sql, DEFAULT_TIMEOUT_MS);
            return extractCount(result) > 0;
        } catch (Exception e) {
            log.warn("Erro ao inspecionar existencia da tabela '{}' no catalogo {}: {}", tableName, engineType, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean columnExists(EngineType engineType, String tableName, String columnName, String expectedType) {
        if (engineType == null || tableName == null || columnName == null) {
            return false;
        }
        try {
            QueryEngineExecutor executor = engineRegistry.getExecutor(engineType);
            if (executor == null) {
                return false;
            }
            String cleanTable = cleanIdentifier(tableName);
            String cleanCol = cleanIdentifier(columnName);
            String sql = "SELECT udt_name, data_type FROM information_schema.columns WHERE table_schema = 'public' AND table_name = '" + cleanTable + "' AND column_name = '" + cleanCol + "'";
            QueryResult result = executor.execute(sql, DEFAULT_TIMEOUT_MS);
            if (result == null || !result.success() || result.rows() == null || result.rows().isEmpty()) {
                return false;
            }
            if (expectedType == null || expectedType.isBlank()) {
                return true;
            }
            Map<String, Object> firstRow = result.rows().getFirst();
            String udtName = String.valueOf(firstRow.getOrDefault("udt_name", "")).toLowerCase();
            String dataType = String.valueOf(firstRow.getOrDefault("data_type", "")).toLowerCase();
            String exp = expectedType.trim().toLowerCase();
            return udtName.contains(exp) || dataType.contains(exp);
        } catch (Exception e) {
            log.warn("Erro ao inspecionar coluna '{}.{}' no catalogo {}: {}", tableName, columnName, engineType, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean foreignKeyExists(EngineType engineType, String tableName, String foreignTableName) {
        if (engineType == null || tableName == null || foreignTableName == null) {
            return false;
        }
        try {
            QueryEngineExecutor executor = engineRegistry.getExecutor(engineType);
            if (executor == null) {
                return false;
            }
            String cleanTable = cleanIdentifier(tableName);
            String cleanForeignTable = cleanIdentifier(foreignTableName);
            String sql = "SELECT COUNT(*) AS cnt FROM information_schema.table_constraints tc " +
                    "JOIN information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name " +
                    "WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public' " +
                    "AND tc.table_name = '" + cleanTable + "' AND ccu.table_name = '" + cleanForeignTable + "'";
            QueryResult result = executor.execute(sql, DEFAULT_TIMEOUT_MS);
            return extractCount(result) > 0;
        } catch (Exception e) {
            log.warn("Erro ao inspecionar foreign key entre '{}' e '{}' no catalogo {}: {}", tableName, foreignTableName, engineType, e.getMessage());
            return false;
        }
    }

    @Override
    public long getRowCount(EngineType engineType, String tableName) {
        if (engineType == null || tableName == null || tableName.isBlank()) {
            return 0L;
        }
        try {
            QueryEngineExecutor executor = engineRegistry.getExecutor(engineType);
            if (executor == null) {
                return 0L;
            }
            String cleanTable = cleanIdentifier(tableName);
            String sql = "SELECT COUNT(*) AS cnt FROM " + cleanTable;
            QueryResult result = executor.execute(sql, DEFAULT_TIMEOUT_MS);
            return extractCount(result);
        } catch (Exception e) {
            log.warn("Erro ao contar linhas na tabela '{}' no catalogo {}: {}", tableName, engineType, e.getMessage());
            return 0L;
        }
    }

    @Override
    public boolean viewExists(EngineType engineType, String viewName) {
        if (engineType == null || viewName == null || viewName.isBlank()) {
            return false;
        }
        try {
            QueryEngineExecutor executor = engineRegistry.getExecutor(engineType);
            if (executor == null) {
                return false;
            }
            String cleanView = cleanIdentifier(viewName);
            String sql = "SELECT COUNT(*) AS cnt FROM (SELECT table_name FROM information_schema.views WHERE table_schema = 'public' AND table_name = '" + cleanView + "' " +
                    "UNION ALL SELECT matviewname AS table_name FROM pg_matviews WHERE schemaname = 'public' AND matviewname = '" + cleanView + "') v";
            QueryResult result = executor.execute(sql, DEFAULT_TIMEOUT_MS);
            return extractCount(result) > 0;
        } catch (Exception e) {
            log.warn("Erro ao inspecionar view '{}' no catalogo {}: {}", viewName, engineType, e.getMessage());
            return false;
        }
    }

    @Override
    public long countNeo4jNodes(String label) {
        if (label == null || label.isBlank()) {
            return 0L;
        }
        try {
            QueryEngineExecutor executor = engineRegistry.getExecutor(EngineType.NEO4J);
            if (executor == null) {
                return 0L;
            }
            String cleanLabel = cleanIdentifier(label);
            String cypher = "MATCH (n:" + cleanLabel + ") RETURN count(n) AS cnt";
            QueryResult result = executor.execute(cypher, DEFAULT_TIMEOUT_MS);
            return extractCount(result);
        } catch (Exception e) {
            log.warn("Erro ao contar nós com label '{}' no Neo4j: {}", label, e.getMessage());
            return 0L;
        }
    }

    @Override
    public long countNeo4jRelationships(String relationshipType) {
        if (relationshipType == null || relationshipType.isBlank()) {
            return 0L;
        }
        try {
            QueryEngineExecutor executor = engineRegistry.getExecutor(EngineType.NEO4J);
            if (executor == null) {
                return 0L;
            }
            String cleanRel = cleanIdentifier(relationshipType);
            String cypher = "MATCH ()-[r:" + cleanRel + "]->() RETURN count(r) AS cnt";
            QueryResult result = executor.execute(cypher, DEFAULT_TIMEOUT_MS);
            return extractCount(result);
        } catch (Exception e) {
            log.warn("Erro ao contar relacionamentos com tipo '{}' no Neo4j: {}", relationshipType, e.getMessage());
            return 0L;
        }
    }

    private String cleanIdentifier(String identifier) {
        return identifier.replaceAll("[^a-zA-Z0-9_]", "");
    }

    private long extractCount(QueryResult result) {
        if (result == null || !result.success() || result.rows() == null || result.rows().isEmpty()) {
            return 0L;
        }
        Map<String, Object> firstRow = result.rows().getFirst();
        if (firstRow.isEmpty()) {
            return 0L;
        }
        Object cntVal = firstRow.values().iterator().next();
        if (cntVal instanceof Number num) {
            return num.longValue();
        }
        if (cntVal != null) {
            try {
                return Long.parseLong(cntVal.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0L;
    }
}
