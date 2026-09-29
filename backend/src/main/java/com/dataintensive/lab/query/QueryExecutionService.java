package com.dataintensive.lab.query;

import com.dataintensive.lab.catalog.CatalogService;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import jakarta.annotation.PreDestroy;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Entity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QueryExecutionService {

    private final String postgresUrl;
    private final String postgresUser;
    private final String postgresPassword;

    private final org.neo4j.driver.Driver neo4jDriver;
    private final CatalogService catalogService;

    public QueryExecutionService(
            @Value("${lab.postgres.url}") String postgresUrl,
            @Value("${lab.postgres.username}") String postgresUser,
            @Value("${lab.postgres.password}") String postgresPassword,
            @Value("${lab.neo4j.uri}") String neo4jUri,
            @Value("${lab.neo4j.username}") String neo4jUser,
            @Value("${lab.neo4j.password}") String neo4jPassword,
            CatalogService catalogService) {
        this.postgresUrl = postgresUrl;
        this.postgresUser = postgresUser;
        this.postgresPassword = postgresPassword;
        this.catalogService = catalogService;

        org.neo4j.driver.Driver driver = null;
        try {
            driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(neo4jUser, neo4jPassword));
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao inicializar driver do Neo4j: " + e.getMessage());
        }
        this.neo4jDriver = driver;
    }

    public QueryResult execute(QueryRequest request) {
        if (request.query() == null || request.query().trim().isEmpty()) {
            return QueryResult.error("A consulta fornecida está vazia.", 0);
        }

        EngineType engine = request.engineType() != null ? request.engineType() : EngineType.POSTGRES;

        long startTime = System.currentTimeMillis();
        try {
            return switch (engine) {
                case POSTGRES -> executePostgres(request.query(), startTime);
                case NEO4J -> executeNeo4j(request.query(), startTime);
            };
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return QueryResult.error(e.getMessage(), duration);
        }
    }

    public QueryResult resetLab(String labId) {
        Optional<Lab> labOpt = catalogService.findLabById(labId);
        if (labOpt.isEmpty()) {
            return QueryResult.error("Laboratório não encontrado com id: " + labId, 0);
        }

        Lab lab = labOpt.get();
        if (lab.resetSchemaSql() == null || lab.resetSchemaSql().isBlank()) {
            return QueryResult.ok(List.of("status"), List.of(Map.of("status", "Nenhum script de reset configurado")), 0);
        }

        return execute(new QueryRequest(lab.resetSchemaSql(), lab.engineType(), lab.id()));
    }

    private QueryResult executePostgres(String sql, long startTime) throws SQLException {
        try (Connection conn = DriverManager.getConnection(postgresUrl, postgresUser, postgresPassword);
             Statement stmt = conn.createStatement()) {

            boolean hasResultSet = stmt.execute(sql);
            long duration = System.currentTimeMillis() - startTime;

            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();
                    List<String> columns = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(meta.getColumnLabel(i));
                    }

                    List<Map<String, Object>> rows = new ArrayList<>();
                    int maxRows = 200;
                    while (rs.next() && rows.size() < maxRows) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(columns.get(i - 1), rs.getObject(i));
                        }
                        rows.add(row);
                    }

                    return QueryResult.ok(columns, rows, duration);
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                return QueryResult.update(Math.max(updateCount, 0), duration);
            }
        }
    }

    private QueryResult executeNeo4j(String cypher, long startTime) {
        if (neo4jDriver == null) {
            return QueryResult.error("Driver do Neo4j não está disponível.", System.currentTimeMillis() - startTime);
        }

        try (Session session = neo4jDriver.session()) {
            return session.executeWrite(tx -> {
                org.neo4j.driver.Result result = tx.run(cypher);
                List<String> keys = result.keys();
                List<Map<String, Object>> rows = new ArrayList<>();

                int maxRows = 200;
                while (result.hasNext() && rows.size() < maxRows) {
                    Record record = result.next();
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String key : keys) {
                        org.neo4j.driver.Value val = record.get(key);
                        row.put(key, convertNeo4jValue(val));
                    }
                    rows.add(row);
                }

                long duration = System.currentTimeMillis() - startTime;
                if (keys.isEmpty() && rows.isEmpty()) {
                    return QueryResult.update(0, duration);
                }
                return QueryResult.ok(keys, rows, duration);
            });
        }
    }

    private Object convertNeo4jValue(org.neo4j.driver.Value value) {
        if (value.isNull()) {
            return null;
        }
        if (value.hasType(neo4jDriver.defaultTypeSystem().NODE())) {
            Entity node = value.asEntity();
            return Map.of(
                "labels", value.asNode().labels(),
                "properties", node.asMap()
            );
        }
        if (value.hasType(neo4jDriver.defaultTypeSystem().RELATIONSHIP())) {
            Entity rel = value.asEntity();
            return Map.of(
                "type", value.asRelationship().type(),
                "properties", rel.asMap()
            );
        }
        if (value.hasType(neo4jDriver.defaultTypeSystem().LIST())) {
            return value.asList(this::convertNeo4jValue);
        }
        if (value.hasType(neo4jDriver.defaultTypeSystem().MAP())) {
            return value.asMap(this::convertNeo4jValue);
        }
        return value.asObject();
    }

    @PreDestroy
    public void cleanup() {
        if (neo4jDriver != null) {
            neo4jDriver.close();
        }
    }
}
