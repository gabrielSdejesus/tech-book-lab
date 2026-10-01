package com.dataintensive.lab.query;

import com.dataintensive.lab.domain.EngineType;
import jakarta.annotation.PreDestroy;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Entity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Neo4jEngineExecutor implements QueryEngineExecutor {

    private final Driver neo4jDriver;

    @Autowired
    public Neo4jEngineExecutor(
            @Value("${lab.neo4j.uri:bolt://localhost:7687}") String neo4jUri,
            @Value("${lab.neo4j.username:neo4j}") String neo4jUser,
            @Value("${lab.neo4j.password:tblpassword}") String neo4jPassword) {
        Driver driver = null;
        try {
            driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(neo4jUser, neo4jPassword));
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao inicializar driver do Neo4j: " + e.getMessage());
        }
        this.neo4jDriver = driver;
    }

    public Neo4jEngineExecutor(Driver driver) {
        this.neo4jDriver = driver;
    }

    @Override
    public EngineType getEngineType() {
        return EngineType.NEO4J;
    }

    @Override
    public QueryResult execute(String cypher, long startTime) {
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
                        row.put(key, sanitizeValue(record.get(key).asObject()));
                    }
                    rows.add(row);
                }

                long duration = System.currentTimeMillis() - startTime;
                if (keys.isEmpty() && rows.isEmpty()) {
                    var summary = result.consume();
                    int nodesCreated = summary.counters().nodesCreated();
                    int relsCreated = summary.counters().relationshipsCreated();
                    int totalUpdates = nodesCreated + relsCreated;
                    return QueryResult.update(totalUpdates, duration);
                }
                return QueryResult.ok(keys, rows, duration);
            });
        }
    }

    private Object sanitizeValue(Object val) {
        if (val instanceof Entity entity) {
            return entity.asMap();
        }
        return val;
    }

    @PreDestroy
    public void close() {
        if (neo4jDriver != null) {
            try {
                neo4jDriver.close();
            } catch (Exception ignored) {}
        }
    }
}
