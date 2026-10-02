package com.dataintensive.lab.query;

import com.dataintensive.lab.catalog.CatalogService;
import com.dataintensive.lab.domain.DomainValidationException;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import com.dataintensive.lab.domain.QueryExecutionException;
import com.dataintensive.lab.provisioning.LabDatabaseResetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QueryExecutionService implements LabDatabaseResetter {

    private static final Logger log = LoggerFactory.getLogger(QueryExecutionService.class);

    private final QueryEngineRegistry engineRegistry;
    private final CatalogService catalogService;

    @Autowired
    public QueryExecutionService(QueryEngineRegistry engineRegistry, CatalogService catalogService) {
        this.engineRegistry = engineRegistry;
        this.catalogService = catalogService;
    }

    public QueryExecutionService(
            String postgresUrl,
            String postgresUser,
            String postgresPassword,
            String neo4jUri,
            String neo4jUser,
            String neo4jPassword,
            CatalogService catalogService) {
        this(new QueryEngineRegistry(List.of(
                new PostgresEngineExecutor(postgresUrl, postgresUser, postgresPassword),
                new Neo4jEngineExecutor(neo4jUri, neo4jUser, neo4jPassword)
        )), catalogService);
    }

    public QueryResult execute(QueryRequest request) {
        if (request.query() == null || request.query().trim().isEmpty()) {
            return QueryResult.error("A consulta fornecida está vazia.", 0);
        }

        if (request.labId() != null) {
            Optional<Lab> labOpt = catalogService.findLabById(request.labId());
            if (labOpt.isEmpty()) {
                throw new DomainValidationException("Laboratório não encontrado com id: " + request.labId());
            }
        }

        EngineType engine = request.engineType() != null ? request.engineType() : EngineType.POSTGRES;
        QueryEngineExecutor executor = engineRegistry.getExecutor(engine);

        long startTime = System.currentTimeMillis();
        try {
            return executor.execute(request.query(), startTime);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            throw new QueryExecutionException(e.getMessage(), duration);
        }
    }

    public QueryResult resetLab(String labId) {
        Optional<Lab> labOpt = catalogService.findLabById(labId);
        if (labOpt.isEmpty()) {
            throw new DomainValidationException("Laboratório não encontrado com id: " + labId);
        }

        Lab lab = labOpt.get();
        if (lab.resetSchemaSql() == null || lab.resetSchemaSql().isBlank()) {
            return QueryResult.ok(List.of("status"), List.of(Map.of("status", "Nenhum script de reset configurado")), 0);
        }

        return execute(new QueryRequest(lab.resetSchemaSql(), lab.engineType(), lab.id()));
    }

    @Override
    public void resetDatabase(EngineType engineType, Lab lab) {
        String cleanSql;
        if (engineType == EngineType.NEO4J) {
            cleanSql = "MATCH (n) DETACH DELETE n;";
        } else {
            cleanSql = "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO postgres; GRANT ALL ON SCHEMA public TO public;";
        }

        try {
            execute(new QueryRequest(cleanSql, engineType, null));
        } catch (Exception e) {
            log.warn("Erro ao executar limpeza geral do banco de dados (motor {}): {}", engineType, e.getMessage());
        }

        if (lab != null && lab.resetSchemaSql() != null && !lab.resetSchemaSql().isBlank()) {
            try {
                execute(new QueryRequest(lab.resetSchemaSql(), engineType, null));
            } catch (Exception e) {
                log.warn("Erro ao executar resetSchemaSql do laboratório {}: {}", lab.id(), e.getMessage());
            }
        }
    }
}
