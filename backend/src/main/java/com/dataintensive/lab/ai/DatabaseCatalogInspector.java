package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.EngineType;

public interface DatabaseCatalogInspector {

    /**
     * Verifica se uma tabela existe no catálogo público do mecanismo especificado.
     */
    boolean tableExists(EngineType engineType, String tableName);

    /**
     * Verifica se uma coluna existe na tabela e se possui o tipo esperado (ex: jsonb).
     */
    boolean columnExists(EngineType engineType, String tableName, String columnName, String expectedType);

    /**
     * Verifica se existe uma restrição de Foreign Key da tabela de origem referenciando a tabela de destino.
     */
    boolean foreignKeyExists(EngineType engineType, String tableName, String foreignTableName);

    /**
     * Retorna a quantidade de registros presentes em uma tabela.
     */
    long getRowCount(EngineType engineType, String tableName);

    /**
     * Verifica se uma view ou materialized view existe no banco de dados.
     */
    boolean viewExists(EngineType engineType, String viewName);

    /**
     * Retorna a quantidade de nós existentes no Neo4j com o rótulo especificado.
     */
    long countNeo4jNodes(String label);

    /**
     * Retorna a quantidade de relacionamentos existentes no Neo4j com o tipo especificado.
     */
    long countNeo4jRelationships(String relationshipType);
}
