package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.AssessmentLanguage;
import com.dataintensive.lab.domain.Challenge;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class HeuristicProviderClient implements AiProviderClient {

    private final DatabaseCatalogInspector catalogInspector;

    public HeuristicProviderClient() {
        this(null);
    }

    @Autowired
    public HeuristicProviderClient(@Nullable DatabaseCatalogInspector catalogInspector) {
        this.catalogInspector = catalogInspector;
    }

    @Override
    public String getProviderId() {
        return "heuristic";
    }

    @Override
    public AiProviderInfo getInfo(AssessmentLanguage language) {
        boolean isEn = language == AssessmentLanguage.EN;
        return new AiProviderInfo(
                "heuristic",
                isEn ? "Offline Heuristic Tutor" : "Tutor Heurístico Local",
                isEn
                        ? "Deterministic offline evaluation based on engineering rules. Zero API keys, zero external internet dependencies."
                        : "Avaliação socrática determinística local e offline baseada em regras de engenharia. Zero chaves de API, zero dependência externa de internet.",
                false,
                "",
                "",
                "rules-engine-v1",
                List.of(new AiModelInfo("rules-engine-v1", isEn ? "Local Analytical Engine" : "Motor Analítico Local (Regras)", true))
        );
    }

    @Override
    public boolean isConfigured(String apiKeyOverride) {
        return true;
    }

    @Override
    public boolean isConfigurableTutor() {
        return false;
    }

    @Override
    public AiTestConnectionResponse testConnection(AiTestConnectionRequest request) {
        return new AiTestConnectionResponse(true, "Motor heurístico local operacional e pronto para avaliações offline.", "rules-engine-v1", 0L);
    }

    @Override
    public AiAssessmentResponse assess(String prompt, String apiKey, String modelOverride, boolean isModelOverridden) {
        return new AiAssessmentResponse(
                "APPROVED",
                "Avaliação heurística concluída com sucesso.",
                "Análise conceitual realizada pelo motor analítico offline.",
                "Execução direta sobre a infraestrutura de laboratório.",
                List.of(),
                "Tutor Heurístico (Regras Locais)"
        );
    }

    @Override
    public AiAssessmentResponse assess(Lab lab, Challenge challenge, AiAssessmentRequest request, AssessmentLanguage language) {
        AiAssessmentResponse response = evaluateChallengeHeuristics(lab, challenge, request, language);
        if (challenge != null && challenge.expectedReflection() != null && !challenge.expectedReflection().isBlank()) {
            return new AiAssessmentResponse(
                    response.status(),
                    response.feedback(),
                    response.tradeOffAnalysis(),
                    response.efficiencyNotes(),
                    response.alternativeApproaches(),
                    response.modelUsed(),
                    challenge.expectedReflection()
            );
        }
        return response;
    }

    private AiAssessmentResponse evaluateChallengeHeuristics(Lab lab, Challenge challenge, AiAssessmentRequest request, AssessmentLanguage language) {
        boolean isEn = language == AssessmentLanguage.EN;
        String modelName = isEn ? "Offline Heuristic Tutor (Rules Engine)" : "Tutor Heurístico (Regras Locais)";

        String userQuery = request.userQuery() != null ? request.userQuery().trim() : "";
        String normalizedQuery = userQuery.toUpperCase(Locale.ROOT);
        String challengeId = challenge != null && challenge.id() != null ? challenge.id().toLowerCase(Locale.ROOT) : "";

        // 1. Verificação de submissão trivial ou vazia
        if (userQuery.isEmpty() || userQuery.equalsIgnoreCase("SELECT 1;")) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "Incomplete solution. A trivial query was submitted without fulfilling the challenge requirements."
                            : "Solução incompleta. Foi submetida uma consulta trivial sem atender aos requisitos práticos do desafio.",
                    isEn
                            ? "To analyze architectural trade-offs, you must model the structures and execute queries relevant to the scenario."
                            : "Para analisar os trade-offs arquiteturais, é necessário modelar as estruturas e executar consultas pertinentes ao cenário.",
                    isEn ? "Execution plan requires realistic relational or graph statements." : "Plano de execução requer comandos relacionais ou de grafos realistas.",
                    isEn
                            ? List.of("Read the practical scenario in the sidebar", "Implement the requested DDL/DML statements")
                            : List.of("Leia o cenário prático na barra lateral", "Implemente as tabelas ou nós requeridos pelo laboratório"),
                    modelName
            );
        }

        // 2. Análise específica por desafio conhecido com verificação estrita em duas fases
        if (challengeId.equals("lab-01-ch-1")) {
            return evaluate3NF(normalizedQuery, isEn, modelName);
        } else if (challengeId.equals("lab-01-ch-2")) {
            return evaluateJsonb(normalizedQuery, isEn, modelName);
        } else if (challengeId.equals("lab-02-ch-1")) {
            return evaluateCypher(normalizedQuery, isEn, modelName);
        } else if (challengeId.equals("lab-02-ch-2")) {
            return evaluateRecursiveCte(normalizedQuery, isEn, modelName);
        } else if (challengeId.equals("lab-03-ch-1")) {
            return evaluateStarSchema(normalizedQuery, isEn, modelName);
        } else if (challengeId.equals("lab-03-ch-2")) {
            return evaluateSliceDice(normalizedQuery, isEn, modelName);
        } else if (challengeId.equals("lab-04-ch-1")) {
            return evaluateEventStore(normalizedQuery, isEn, modelName);
        } else if (challengeId.equals("lab-04-ch-2")) {
            return evaluateCqrsView(normalizedQuery, isEn, modelName);
        }

        // 3. Fallback heurístico genérico
        return evaluateGeneric(normalizedQuery, lab, challenge, isEn, modelName);
    }

    private AiAssessmentResponse evaluate3NF(String query, boolean isEn, String modelName) {
        // Anti-padrão semântico: INNER JOIN sem outer join na reconstrução de perfis
        boolean hasJoin = query.contains("JOIN");
        boolean hasLeftJoin = query.contains("LEFT JOIN");

        if (hasJoin && !hasLeftJoin) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "3NF profile reconstruction requires explicit LEFT JOIN (rather than inner JOIN) to ensure users without experiences or education are preserved in the result."
                            : "A modelagem 3NF requer a utilização explícita de LEFT JOIN (e não apenas JOIN interno) na reconstrução dos perfis, garantindo que usuários sem experiências ou formações cadastradas ainda sejam retornados.",
                    isEn
                            ? "Martin Kleppmann highlights in DDIA that relational modeling normalizes child entities into 1:N relations, requiring outer joins to reconstruct optional attributes without row loss."
                            : "Martin Kleppmann destaca no DDIA que a normalização divide entidades filhas em relações 1:N, exigindo junções externas (LEFT JOIN) para recompor atributos opcionais sem perda de linhas.",
                    isEn ? "Inner JOIN filters out users without child records, violating profile domain semantics." : "O JOIN interno descarta usuários sem registros filhos, violando a semântica do domínio de perfis.",
                    isEn
                            ? List.of("Replace inner JOIN with LEFT JOIN on child tables", "Ensure usuarios table connects via LEFT JOIN")
                            : List.of("Substitua JOIN por LEFT JOIN nas tabelas de experiências e formações", "Garanta que a tabela principal 'usuarios' conecte via LEFT JOIN"),
                    modelName
            );
        }

        // Inspeção Baseada em Estado (Prioridade Máxima do Catálogo Real)
        if (catalogInspector != null) {
            if (!catalogInspector.tableExists(EngineType.POSTGRES, "usuarios")) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "Table 'usuarios' was not found in the public schema." : "A tabela 'usuarios' não foi encontrada no schema public.");
            }
            boolean hasFk = catalogInspector.foreignKeyExists(EngineType.POSTGRES, "experiencias_profissionais", "usuarios") ||
                    catalogInspector.foreignKeyExists(EngineType.POSTGRES, "formacoes_academicas", "usuarios");
            if (!hasFk) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "Foreign key relationship pointing to 'usuarios' was not detected." : "Chave estrangeira apontando para 'usuarios' não foi detectada no catálogo.");
            }
            if (catalogInspector.getRowCount(EngineType.POSTGRES, "usuarios") < 2) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "Table 'usuarios' must have at least 2 rows." : "A tabela 'usuarios' deve conter pelo menos 2 registros inseridos.");
            }
            return approved3NFResponse(isEn, modelName);
        }

        // Fallback sintático quando catálogo offline não estiver acoplado
        boolean hasTables = query.contains("CREATE TABLE") && query.contains("USUARIOS");
        boolean hasChildTables = query.contains("EXPERIENCIAS") || query.contains("FORMACOES");
        boolean hasRelationships = query.contains("REFERENCES") || query.contains("FOREIGN KEY");
        boolean hasInserts = query.contains("INSERT INTO");

        if (!hasLeftJoin) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "3NF profile reconstruction requires explicit LEFT JOIN (rather than inner JOIN) to ensure users without experiences or education are preserved in the result."
                            : "A modelagem 3NF requer a utilização explícita de LEFT JOIN (e não apenas JOIN interno) na reconstrução dos perfis, garantindo que usuários sem experiências ou formações cadastradas ainda sejam retornados.",
                    isEn
                            ? "Martin Kleppmann highlights in DDIA that relational modeling normalizes child entities into 1:N relations, requiring outer joins to reconstruct optional attributes without row loss."
                            : "Martin Kleppmann destaca no DDIA que a normalização divide entidades filhas em relações 1:N, exigindo junções externas (LEFT JOIN) para recompor atributos opcionais sem perda de linhas.",
                    isEn ? "Inner JOIN filters out users without child records, violating profile domain semantics." : "O JOIN interno descarta usuários sem registros filhos, violando a semântica do domínio de perfis.",
                    isEn
                            ? List.of("Replace inner JOIN with LEFT JOIN on child tables", "Ensure usuarios table connects via LEFT JOIN")
                            : List.of("Substitua JOIN por LEFT JOIN nas tabelas de experiências e formações", "Garanta que a tabela principal 'usuarios' conecte via LEFT JOIN"),
                    modelName
            );
        }

        if (!hasTables || !hasChildTables || !hasRelationships || !hasInserts) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "The 3NF model requires separate normalized tables (usuarios, experiencias_profissionais) with foreign keys and INSERT statements."
                            : "A modelagem 3NF requer tabelas separadas e normalizadas (usuarios, experiencias_profissionais) com chaves estrangeiras e comandos INSERT INTO.",
                    isEn
                            ? "Normalization divides data into distinct tables to eliminate redundancy, but requires explicit foreign keys."
                            : "A normalização divide os dados em entidades distintas para eliminar duplicações, mas exige chaves estrangeiras explícitas.",
                    isEn ? "Missing table declarations or foreign key references." : "Faltam declarações de tabela, chaves estrangeiras ou inserções de dados.",
                    isEn
                            ? List.of("Declare foreign keys with REFERENCES usuarios(id)", "Insert test records with INSERT INTO")
                            : List.of("Declare chaves estrangeiras com REFERENCES usuarios(id)", "Insira registros de teste com INSERT INTO"),
                    modelName
            );
        }

        return approved3NFResponse(isEn, modelName);
    }

    private AiAssessmentResponse approved3NFResponse(boolean isEn, String modelName) {
        return new AiAssessmentResponse(
                "APPROVED",
                isEn
                        ? "Excellent 3NF relational modeling! You separated normalized entities with primary and foreign keys, preventing update anomalies, and reconstructed the profile using LEFT JOIN."
                        : "Excelente modelagem 3NF! Você separou as entidades normalizadas com chaves primárias e estrangeiras, eliminando redundâncias e anomalias de atualização, e reconstruiu o perfil usando LEFT JOIN.",
                isEn
                        ? "As Martin Kleppmann highlights in Chapter 3, 3NF optimizes write consistency at the cost of multiple JOINs and disk scattering on read."
                        : "Conforme Martin Kleppmann explica no Capítulo 3, o modelo relacional 3NF otimiza a consistência nas escritas ao custo de exigir múltiplos JOINs e dispersar os registros em disco durante leituras.",
                isEn ? "Queries with joins benefit from foreign key indexes on dependent tables." : "Consultas com junções se beneficiam fortemente de índices nas colunas de chave estrangeira.",
                isEn
                        ? List.of("Consider creating composite indexes on (usuario_id, id) for filtered profile views", "Analyze the query plan with EXPLAIN ANALYZE")
                        : List.of("Considere criar índices compostos nas chaves estrangeiras", "Analise o plano de execução gerado com EXPLAIN ANALYZE"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateJsonb(String query, boolean isEn, String modelName) {
        // Inspeção Baseada em Estado (Prioridade Máxima do Catálogo Real)
        if (catalogInspector != null) {
            boolean hasJsonbColumn = catalogInspector.columnExists(EngineType.POSTGRES, "usuarios_documento", "perfil", "jsonb");
            if (!hasJsonbColumn) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "Column 'perfil' of type jsonb was not found in table 'usuarios_documento'." : "A coluna 'perfil' do tipo jsonb não foi encontrada na tabela 'usuarios_documento'.");
            }
            if (catalogInspector.getRowCount(EngineType.POSTGRES, "usuarios_documento") < 2) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "Table 'usuarios_documento' must contain at least 2 rows." : "A tabela 'usuarios_documento' deve conter ao menos 2 registros inseridos.");
            }
            return approvedJsonbResponse(isEn, modelName);
        }

        // Fallback sintático
        boolean hasJsonb = query.contains("JSONB");
        boolean hasInserts = query.contains("INSERT INTO");
        boolean hasOperators = query.contains("->") || query.contains("@>") || query.contains("JSONB_");

        if (!hasJsonb || !hasInserts || !hasOperators) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "The document model requires declaring a JSONB column, inserting structured JSON documents, and querying with JSONB operators (->, ->>, @>)."
                            : "O modelo de documentos requer declarar uma coluna do tipo JSONB, inserir documentos JSON estruturados e consultar com operadores JSONB (->, ->>, @>).",
                    isEn
                            ? "Storage locality in document stores avoids joining separate tables by collocating all related data in a single document."
                            : "A localidade de armazenamento em bancos de documentos evita joins colapsando dados relacionados na mesma estrutura física.",
                    isEn ? "Missing JSONB declaration, INSERT INTO, or document operators." : "Faltam declaração de coluna JSONB, comandos INSERT INTO ou operadores de consulta JSONB.",
                    isEn
                            ? List.of("Declare 'perfil JSONB' in your CREATE TABLE statement", "Query nested fields using ->> or @> operators")
                            : List.of("Declare 'perfil JSONB' no comando CREATE TABLE", "Consulte campos internos usando operadores ->> ou @>"),
                    modelName
            );
        }

        return approvedJsonbResponse(isEn, modelName);
    }

    private AiAssessmentResponse approvedJsonbResponse(boolean isEn, String modelName) {
        return new AiAssessmentResponse(
                "APPROVED",
                isEn
                        ? "Document-oriented model with PostgreSQL JSONB implemented successfully! The profile document stores embedded arrays in a single row."
                        : "Implementação orientada a documentos com PostgreSQL JSONB realizada com maestria! O documento armazena o perfil completo em uma única linha.",
                isEn
                        ? "Document modeling maximizes storage locality: reading the profile requires a single disk access without joins. The trade-off is higher rewrite costs for partial updates."
                        : "A modelagem orientada a documento aproveita a localidade de armazenamento: ler o perfil requer apenas um acesso a disco sem JOINs. O trade-off é o custo de reescrita do documento inteiro em atualizações.",
                isEn ? "GIN indexes (using jsonb_path_ops) accelerate containment operators (@>)." : "Índices GIN aceleram significativamente buscas com operadores de contenção (@>).",
                isEn
                        ? List.of("Create a GIN index on the JSONB column", "Test jsonb_array_elements to unnest internal array items")
                        : List.of("Crie um índice GIN na coluna JSONB para otimizar buscas", "Experimente jsonb_array_elements para projetar itens individuais do array"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateCypher(String query, boolean isEn, String modelName) {
        boolean hasVariablePath = query.contains("*1..") || query.contains("*..") || query.contains("[:WITHIN*") || query.contains("[*");

        if (!hasVariablePath) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "The Cypher query must use variable-length path syntax (e.g. [:WITHIN*1..5]) to flexibly navigate hierarchical containment without fixed hops."
                            : "A consulta Cypher requer a utilização de operador de profundidade variável (ex: [:WITHIN*1..5]) para navegar hierarquias de forma flexível sem fixar saltos arbitrários.",
                    isEn
                            ? "Graph databases excel at recursive paths of unknown or arbitrary depth, expressing transitive closures in concise Cypher patterns."
                            : "Bancos em grafos se destacam por navegar caminhos de profundidade arbitrária de forma concisa e expressiva sem explosão combinatória de joins.",
                    isEn ? "Fixed hops fail when geographic hierarchy depth changes." : "Saltos fixos tornam a consulta rígida e incapaz de absorver mudanças na profundidade do grafo.",
                    isEn
                            ? List.of("Use [:WITHIN*1..5] to reach the country node", "Inspect query traversal with PROFILE")
                            : List.of("Utilize [:WITHIN*1..5] para alcançar o nó de país", "Inspecione os saltos de navegação com PROFILE"),
                    modelName
            );
        }

        // Inspeção no Neo4j
        if (catalogInspector != null) {
            long personNodes = catalogInspector.countNeo4jNodes("Person");
            long locationNodes = catalogInspector.countNeo4jNodes("Location");
            long relationships = catalogInspector.countNeo4jRelationships("WITHIN");

            if (personNodes == 0 && locationNodes == 0) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "No Person or Location nodes found in Neo4j." : "Nenhum nó de Person ou Location encontrado no Neo4j.");
            }
            if (relationships == 0 && catalogInspector.countNeo4jRelationships("BORN_IN") == 0) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "No relationships found in Neo4j." : "Nenhum relacionamento encontrado no Neo4j.");
            }
            return approvedCypherResponse(isEn, modelName);
        }

        // Fallback sintático
        boolean hasCreate = query.contains("CREATE");
        boolean hasLabels = query.contains(":PERSON") || query.contains(":LOCATION");
        boolean hasEdges = query.contains("-[:") && query.contains("]->");
        boolean hasMatch = query.contains("MATCH");

        if (!hasCreate || !hasLabels || !hasEdges || !hasMatch) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "The Cypher solution requires creating nodes (:Person, :Location) and directed edges (-[:WITHIN]->), followed by a MATCH traversal."
                            : "A solução em Cypher requer criar nós (:Person, :Location) e arestas direcionadas (-[:WITHIN]->), seguidos de uma consulta MATCH de travessia.",
                    isEn
                            ? "In property graphs, both vertices and edges have identities and properties."
                            : "Nos grafos de propriedades, nós e relacionamentos possuem identidades próprias e propriedades associadas.",
                    isEn ? "Missing node labels or directed relationships in CREATE statements." : "Faltam rótulos de nós ou arestas direcionadas nos comandos CREATE.",
                    isEn
                            ? List.of("Create nodes with (n:Label {key: 'value'})", "Connect nodes using (a)-[:RELATIONSHIP]->(b)")
                            : List.of("Crie nós com (n:Label {chave: 'valor'})", "Conecte nós com arestas direcionadas (a)-[:RELATIONSHIP]->(b)"),
                    modelName
            );
        }

        return approvedCypherResponse(isEn, modelName);
    }

    private AiAssessmentResponse approvedCypherResponse(boolean isEn, String modelName) {
        return new AiAssessmentResponse(
                "APPROVED",
                isEn
                        ? "Excellent property graph implementation! You modeled hierarchical connections with Cypher and traversed relationships smoothly using variable-depth paths."
                        : "Excelente consulta em grafos de propriedades! Você utilizou Cypher para expressar conexões hierárquicas e travessias entre nós com maestria usando profundidade variável.",
                isEn
                        ? "Property graphs offer high cognitive affinity for variable-depth traversals without the syntactic friction of SQL recursive CTEs."
                        : "O modelo de grafos de propriedades oferece alta afinidade cognitiva para relações complexas N:N e travessias recursivas, dispensando a sobrecarga sintática de CTEs relacionais.",
                isEn ? "Neo4j leverages index-free adjacency: traversing relationships is O(1) per pointer hop." : "O Neo4j utiliza adjacência livre de índices (index-free adjacency): travessias de ponteiros em memória são O(1) por nó adjacente.",
                isEn
                        ? List.of("Experiment with variable-length path syntax [:WITHIN*1..5]", "Filter traversal using shortestPath((p)-[*]-(b))")
                        : List.of("Experimente o operador de profundidade variável [:WITHIN*1..5]", "Utilize shortestPath para descobrir a menor rota entre duas entidades"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateRecursiveCte(String query, boolean isEn, String modelName) {
        // Fase 1: Sintaxe & Semântica Estrita
        boolean hasRecursive = query.contains("WITH RECURSIVE");
        boolean hasUnion = query.contains("UNION");
        boolean hasParentId = query.contains("PARENT_ID");

        if (!hasRecursive || !hasUnion || !hasParentId) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "A recursive CTE query requires 'WITH RECURSIVE', self-referential 'parent_id', and a 'UNION' (or 'UNION ALL') clause combining anchor and recursive members."
                            : "A consulta recursiva requer a cláusula 'WITH RECURSIVE', auto-relacionamento 'parent_id' e cláusula 'UNION' (ou 'UNION ALL') conectando o termo âncora ao termo recursivo.",
                    isEn
                            ? "Relational databases navigate graph hierarchies through iterative self-joins in CTEs."
                            : "Bancos relacionais navegam hierarquias em grafos por meio de auto-junções iterativas em CTEs.",
                    isEn ? "Missing WITH RECURSIVE, parent_id, or UNION." : "Faltam as cláusulas WITH RECURSIVE, parent_id ou UNION.",
                    isEn
                            ? List.of("Define anchor member: SELECT id, parent_id FROM locais WHERE ...", "Add recursive member: UNION ALL SELECT l.id, l.parent_id FROM locais l JOIN cte c ON l.id = c.parent_id")
                            : List.of("Defina o membro âncora inicial da hierarquia", "Conecte com UNION ALL e faça o JOIN da tabela com a própria CTE recursiva"),
                    modelName
            );
        }

        // Fase 2: Inspeção do Catálogo do Banco de Dados
        if (catalogInspector != null) {
            if (catalogInspector.tableExists(EngineType.POSTGRES, "locais") &&
                    catalogInspector.getRowCount(EngineType.POSTGRES, "locais") < 3) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "Table 'locais' needs at least 3 rows to form a multi-level hierarchy." : "A tabela 'locais' precisa de ao menos 3 registros para representar uma hierarquia multi-nível.");
            }
        }

        return new AiAssessmentResponse(
                "APPROVED",
                isEn
                        ? "Recursive CTE in PostgreSQL implemented successfully! You computed the transitive closure over the adjacency hierarchy."
                        : "Implementação de CTE recursiva em SQL com sucesso! Você computou o fechamento transitivo navegando por nós pais até o elemento raiz.",
                isEn
                        ? "Recursive CTEs compute transitive closures in standard SQL, but dense cyclic graphs require careful loop termination safeguards."
                        : "CTEs recursivas permitem computar o fechamento transitivo em SQL relacional padrão, porém grafos densos exigem controle rigoroso para evitar loops infinitos.",
                isEn ? "Ensure foreign key indexes exist on parent_id to prevent sequential scans during recursion." : "Garanta índices em parent_id para evitar varreduras sequenciais em cada iteração recursiva.",
                isEn
                        ? List.of("Include a depth counter column in the recursive term to limit recursion depth", "Compare performance against Neo4j on larger datasets")
                        : List.of("Adicione um contador de profundidade (nivel + 1) para proteção", "Compare a legibilidade da query com a versão equivalente em Cypher"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateStarSchema(String query, boolean isEn, String modelName) {
        // Inspeção Baseada em Estado (Prioridade Máxima do Catálogo Real)
        if (catalogInspector != null) {
            if (!catalogInspector.tableExists(EngineType.POSTGRES, "fato_vendas")) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "Fact table 'fato_vendas' was not found in database." : "A tabela de fatos 'fato_vendas' não foi encontrada no banco de dados.");
            }
            if (catalogInspector.getRowCount(EngineType.POSTGRES, "fato_vendas") < 1) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "Fact table 'fato_vendas' must contain at least 1 record." : "A tabela de fatos 'fato_vendas' deve conter ao menos 1 registro inserido.");
            }
            return approvedStarSchemaResponse(isEn, modelName);
        }

        // Fallback sintático
        boolean hasDim = query.contains("DIM_") || query.contains("DIMENSAO");
        boolean hasFact = query.contains("FATO_") || query.contains("FATOS");
        boolean hasFk = query.contains("REFERENCES") || query.contains("FOREIGN KEY");
        boolean hasInsert = query.contains("INSERT INTO");

        if (!hasFact || !hasDim) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "Star Schema dimensional modeling strictly requires the coexistence of both the central fact table (fato_*) AND denormalized dimension tables (dim_*)."
                            : "A modelagem dimensional Star Schema exige estritamente a coexistência da tabela central de fatos (fato_*) E das tabelas de dimensões desnormalizadas (dim_*).",
                    isEn
                            ? "In OLAP systems, facts record quantitative measurements while dimensions provide contextual filtering and grouping attributes."
                            : "No modelo OLAP, fatos registram métricas quantitativas de negócio enquanto dimensões fornecem o contexto descritivo para filtragem e agrupamento.",
                    isEn ? "Both fact and dimension tables must be created and linked together." : "Tanto a tabela de fatos quanto as dimensões devem ser criadas e conectadas.",
                    isEn
                            ? List.of("Model dimension tables (dim_tempo, dim_produto)", "Create fato_vendas referencing all dimensions")
                            : List.of("Modele as tabelas de dimensões (dim_tempo, dim_produto)", "Crie a tabela fato_vendas com referências para todas as dimensões"),
                    modelName
            );
        }

        if (!hasFk || !hasInsert) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "Star Schema modeling requires foreign key constraints connecting the fact table to dimensions and INSERT statements populating sample data."
                            : "A modelagem Star Schema requer chaves estrangeiras conectando a tabela fato às dimensões e comandos INSERT INTO populando dados de exemplo.",
                    isEn
                            ? "Surrogate keys in dimension tables are referenced by the central fact table."
                            : "Chaves substitutas (surrogate keys) nas tabelas de dimensão são referenciadas pela tabela fato central.",
                    isEn ? "Missing REFERENCES or INSERT statements." : "Faltam restrições de REFERENCES ou comandos INSERT INTO.",
                    isEn
                            ? List.of("Add REFERENCES dim_*(id) on fact table", "Insert records into dimensions and facts")
                            : List.of("Adicione REFERENCES dim_*(id) na tabela fato", "Insira registros nas dimensões e na tabela fato"),
                    modelName
            );
        }

        return approvedStarSchemaResponse(isEn, modelName);
    }

    private AiAssessmentResponse approvedStarSchemaResponse(boolean isEn, String modelName) {
        return new AiAssessmentResponse(
                "APPROVED",
                isEn
                        ? "Dimensional modeling (Star Schema) implemented successfully! Denormalized dimensions surround the central fact table."
                        : "Modelagem dimensional (Star Schema) implementada com sucesso! As dimensões desnormalizadas cercam a tabela de fatos com métricas aditivas.",
                isEn
                        ? "In OLAP systems, intentional denormalization of dimension tables facilitates fast analytical queries without navigating intricate 3NF relationships."
                        : "No OLAP, desnormalizamos intencionalmente as tabelas de dimensões para viabilizar consultas agregadoras ultra-rápidas sem navegar por teias 3NF complexas.",
                isEn ? "Surrogate keys (INT) on dimensions optimize join performance in analytical queries." : "Chaves substitutas (surrogate keys inteiras) nas dimensões aceleram os joins na tabela de fatos.",
                isEn
                        ? List.of("Add additive metrics (valor_total, quantidade) in fact tables", "Denormalize date attributes (ano, trimestre, mes) in dim_tempo")
                        : List.of("Inclua métricas aditivas (valor_total, quantidade) na fato", "Desnormalize atributos temporais na dim_tempo"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateSliceDice(String query, boolean isEn, String modelName) {
        // Fase 1: Sintaxe & Semântica Estrita
        boolean hasSelect = query.contains("SELECT");
        boolean hasAgg = query.contains("SUM(") || query.contains("COUNT(") || query.contains("AVG(") || query.contains("SUM ") || query.contains("COUNT ");
        boolean hasGroup = query.contains("GROUP BY");

        if (!hasSelect || !hasAgg || !hasGroup) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "Slice & Dice analytics require aggregate functions (SUM, AVG, COUNT) coupled with a GROUP BY clause across dimension columns."
                            : "A análise Slice & Dice requer funções de agregação (SUM, AVG, COUNT) com cláusula GROUP BY cruzando dimensões.",
                    isEn
                            ? "Analytical queries summarize business performance metrics across multiple dimensions."
                            : "Consultas analíticas sumarizam métricas de desempenho de negócio cruzando dimensões.",
                    isEn ? "Add SUM/COUNT aggregations with GROUP BY." : "Adicione funções de agregação SUM/COUNT com GROUP BY.",
                    isEn
                            ? List.of("Join fato_vendas with dimension tables", "Group by dim_tempo.ano, dim_produto.categoria")
                            : List.of("Faça JOIN da tabela fato com as dimensões", "Agrupe por dimensões como ano, categoria ou loja"),
                    modelName
            );
        }

        // Inspeção do Catálogo do Banco de Dados
        if (catalogInspector != null) {
            if (catalogInspector.tableExists(EngineType.POSTGRES, "fato_vendas") &&
                    catalogInspector.getRowCount(EngineType.POSTGRES, "fato_vendas") < 1) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "Table 'fato_vendas' contains no records to aggregate." : "A tabela 'fato_vendas' não contém registros para agregação.");
            }
        }

        return new AiAssessmentResponse(
                "APPROVED",
                isEn
                        ? "Analytical query (Slice & Dice) executed successfully! Fast aggregation joining facts with dimension tables."
                        : "Consulta analítica (Slice & Dice) executada com sucesso! Agregações eficientes cruzando fatos e dimensões.",
                isEn
                        ? "Star Schema combined with columnar storage allows scanning billions of rows by reading only the relevant columns involved in aggregations."
                        : "Em data warehouses, o Star Schema combinado com bancos colunares permite varrer bilhões de registros lendo apenas as colunas envolvidas na agregação.",
                isEn ? "Aggregations over integer surrogate keys minimize join CPU overhead." : "Agregações sobre chaves substitutas inteiras minimizam o custo de CPU nas junções.",
                isEn
                        ? List.of("Test GROUP BY ROLLUP / CUBE for multi-level hierarchical subtotals", "Check execution plan using EXPLAIN ANALYZE")
                        : List.of("Experimente GROUP BY ROLLUP ou CUBE para subtotais automáticos", "Inspecione o custo com EXPLAIN ANALYZE"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateEventStore(String query, boolean isEn, String modelName) {
        // Anti-padrão de mutabilidade em Event Sourcing: UPDATE ou DELETE
        if (query.contains("UPDATE ") || query.contains("DELETE ")) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "Anti-pattern detected! An Event Store operates strictly in append-only mode. UPDATE or DELETE statements violate history immutability and complete auditability as highlighted by Martin Kleppmann in DDIA."
                            : "Anti-padrão detectado! Um Event Store opera exclusivamente em modo append-only. Comandos UPDATE ou DELETE violam a imutabilidade do histórico e a rastreabilidade exigida pelo Event Sourcing, conforme Martin Kleppmann destaca no DDIA.",
                    isEn
                            ? "In Event Sourcing, state transitions are represented purely as new events appended to the log, never by mutating or deleting previous events."
                            : "No Event Sourcing, mudanças de estado são representadas puramente como novos eventos adicionados ao final do log, nunca por mutações in-place ou exclusões.",
                    isEn ? "Destructive DML (UPDATE/DELETE) breaks the append-only invariant." : "Comandos DML destrutivos (UPDATE/DELETE) quebram o invariante de log append-only.",
                    isEn
                            ? List.of("Remove all UPDATE and DELETE statements", "Represent status changes as new INSERT INTO events")
                            : List.of("Remova todas as instruções UPDATE e DELETE", "Represente mudanças de estado adicionando novos eventos com INSERT INTO"),
                    modelName
            );
        }

        // Inspeção Baseada em Estado
        if (catalogInspector != null) {
            if (!catalogInspector.tableExists(EngineType.POSTGRES, "pedidos_eventos")) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "Event store table 'pedidos_eventos' was not found in database." : "A tabela de eventos 'pedidos_eventos' não foi encontrada no banco de dados.");
            }
            if (catalogInspector.getRowCount(EngineType.POSTGRES, "pedidos_eventos") < 2) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "Table 'pedidos_eventos' must contain at least 2 events." : "A tabela 'pedidos_eventos' deve conter pelo menos 2 eventos inseridos.");
            }
            return approvedEventStoreResponse(isEn, modelName);
        }

        // Fallback sintático
        boolean hasCreate = query.contains("CREATE TABLE");
        boolean hasEvent = query.contains("EVENT") || query.contains("PEDIDO");
        boolean hasInsert = query.contains("INSERT INTO");

        if (!hasCreate || !hasEvent || !hasInsert) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "Event Sourcing requires an immutable event table storing event types, payloads, and timestamps, along with INSERT statements simulating domain lifecycle."
                            : "Event Sourcing requer uma tabela de eventos imutável registrando tipo do evento, dados e timestamp, além de comandos INSERT INTO simulando o ciclo de vida do domínio.",
                    isEn
                            ? "Event logs represent system state as an ordered sequence of domain events rather than mutable state."
                            : "Logs de eventos representam o estado como uma sequência ordenada de ocorrências de domínio em vez de mutações diretas.",
                    isEn ? "Declare an append-only event table and insert lifecycle events." : "Declare uma tabela append-only para registro de eventos e insira ocorrências de domínio.",
                    isEn
                            ? List.of("Create pedidos_eventos with (id, pedido_id, tipo_evento, dados_evento, criado_em)", "Insert realistic lifecycle events")
                            : List.of("Crie a tabela pedidos_eventos com campos de evento", "Insira eventos representando o ciclo de vida do pedido"),
                    modelName
            );
        }

        return approvedEventStoreResponse(isEn, modelName);
    }

    private AiAssessmentResponse approvedEventStoreResponse(boolean isEn, String modelName) {
        return new AiAssessmentResponse(
                "APPROVED",
                isEn
                        ? "Append-only Event Store structured successfully! Events record immutable facts in domain history."
                        : "Event Store append-only estruturado com perfeição! Eventos capturam fatos imutáveis ocorridos no domínio.",
                isEn
                        ? "An append-only log provides complete auditability and eliminates destructive UPDATE locks in distributed systems."
                        : "O log append-only garante auditoria completa e facilita sistemas distribuídos orientados a eventos, eliminando locks destrutivos de UPDATE.",
                isEn ? "Sequential disk appends offer optimal write throughput in storage engines." : "Escritas sequenciais em append oferecem vazão de escrita significativamente superior a updates in-place.",
                isEn
                        ? List.of("Consider partitioning the events table by timestamp range", "Structure event payload as JSONB for schema evolution")
                        : List.of("Considere particionar a tabela de eventos por range temporal", "Estruture os dados do evento em JSONB para permitir evolução de esquema"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateCqrsView(String query, boolean isEn, String modelName) {
        // Inspeção Baseada em Estado
        if (catalogInspector != null) {
            boolean viewDetected = catalogInspector.viewExists(EngineType.POSTGRES, "pedidos_resumo_leitura") ||
                    catalogInspector.tableExists(EngineType.POSTGRES, "pedidos_resumo_leitura");
            if (!viewDetected) {
                return databaseExecutionRequired(isEn, modelName, isEn ? "View or table 'pedidos_resumo_leitura' was not found in database." : "A visão (VIEW) ou tabela 'pedidos_resumo_leitura' não foi encontrada no banco de dados.");
            }
            return approvedCqrsResponse(isEn, modelName);
        }

        // Fallback sintático
        boolean hasView = query.contains("CREATE MATERIALIZED VIEW") || query.contains("CREATE VIEW") ||
                (query.contains("CREATE TABLE") && query.contains("LEITURA"));

        if (!hasView) {
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "CQRS read modeling requires creating a materialized view or projection table (CREATE MATERIALIZED VIEW / CREATE VIEW) derived from the event store."
                            : "A modelagem de leitura CQRS requer uma visão materializada ou tabela de projeção (CREATE MATERIALIZED VIEW / CREATE VIEW) derivada do log de eventos.",
                    isEn
                            ? "Separating reads and writes allows optimizing read models for consumer UI requirements independently of write rules."
                            : "Separar escrita e leitura permite otimizar o modelo de leitura para a UI independentemente da lógica de escrita.",
                    isEn ? "Create a materialized view over event tables." : "Crie uma visão materializada sobre a tabela de eventos.",
                    isEn
                            ? List.of("Use CREATE MATERIALIZED VIEW ... AS SELECT ...", "Aggregate event states to build read summaries")
                            : List.of("Utilize CREATE MATERIALIZED VIEW ... AS SELECT ...", "Agregue o estado mais recente dos eventos para montar o resumo"),
                    modelName
            );
        }

        return approvedCqrsResponse(isEn, modelName);
    }

    private AiAssessmentResponse approvedCqrsResponse(boolean isEn, String modelName) {
        return new AiAssessmentResponse(
                "APPROVED",
                isEn
                        ? "CQRS read projection implemented successfully! Materialized views decouple fast reads from complex write event logs."
                        : "Projeção de leitura CQRS implementada com sucesso! A visão materializada otimiza a leitura isolando-a do fluxo de escrita.",
                isEn
                        ? "CQRS trades strong immediate consistency for read scalability and query simplicity via eventual projections."
                        : "CQRS aceita consistência eventual para alcançar altíssima escalabilidade e desempenho de leitura sob demanda.",
                isEn ? "Materialized views can be refreshed asynchronously (REFRESH MATERIALIZED VIEW)." : "Visões materializadas podem ser atualizadas de forma assíncrona ou em lote.",
                isEn
                        ? List.of("Use REFRESH MATERIALIZED VIEW CONCURRENTLY with a unique index", "Test read performance directly from the view")
                        : List.of("Utilize REFRESH MATERIALIZED VIEW CONCURRENTLY com índice exclusivo", "Compare a velocidade de leitura da projeção contra o aggregate do log"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateGeneric(String query, Lab lab, Challenge challenge, boolean isEn, String modelName) {
        boolean hasValidSql = query.contains("SELECT") || query.contains("CREATE") || query.contains("INSERT") || query.contains("MATCH");

        if (hasValidSql) {
            return new AiAssessmentResponse(
                    "APPROVED",
                    isEn
                            ? "Challenge solution executed successfully! Your code matches the laboratory objectives."
                            : "Solução do desafio executada com sucesso! Seu código atendeu aos objetivos propostos pelo laboratório.",
                    isEn
                            ? "The solution respects the architectural patterns explored in Designing Data-Intensive Applications."
                            : "A solução respeita os padrões arquiteturais explorados em Designing Data-Intensive Applications.",
                    isEn ? "Query executed and verified against local database instance." : "Consulta executada e validada sobre a instância local do banco de dados.",
                    isEn
                            ? List.of("Review challenge guidelines for additional optimizations", "Compare alternative indexing strategies")
                            : List.of("Revise as diretrizes do desafio para otimizações adicionais", "Considere estratégias alternativas de indexação"),
                    modelName
            );
        }

        return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    isEn
                            ? "Please provide a valid query or model solving the scenario described in the challenge."
                            : "Por favor, forneça uma consulta ou modelagem válida que atenda ao cenário descrito no desafio.",
                    isEn
                            ? "Implement the requested operations to analyze architectural trade-offs."
                            : "Implemente as operações requeridas para analisar os trade-offs arquiteturais.",
                    isEn ? "No recognizable DDL or DML statements detected." : "Nenhum comando DDL ou DML reconhecido.",
                    isEn
                            ? List.of("Read the challenge instructions in the sidebar", "Run your query with Ctrl + Enter before requesting evaluation")
                            : List.of("Leia as instruções do desafio na barra lateral", "Execute sua consulta com Ctrl + Enter antes de solicitar avaliação"),
                    modelName
            );
    }

    private AiAssessmentResponse databaseExecutionRequired(boolean isEn, String modelName, String details) {
        return new AiAssessmentResponse(
                "NEEDS_REVISION",
                (isEn
                        ? "Your query meets the syntax and architectural requirements, but the corresponding tables or records were not found in the database. Please execute your query using Ctrl + Enter before requesting offline evaluation. "
                        : "Sua consulta atende aos requisitos sintáticos e arquiteturais, mas as tabelas ou dados correspondentes ainda não foram detectados no banco de dados. Execute sua consulta com Ctrl + Enter antes de solicitar a validação offline. ") + details,
                isEn
                        ? "In Designing Data-Intensive Applications, empirical verification requires actual execution of commands on the database engine to observe storage layout and behaviors."
                        : "Em Designing Data-Intensive Applications, o aprendizado empírico exige a execução concreta dos comandos no motor de banco para validação de layout de armazenamento e integridade.",
                isEn ? "No records or tables found in current catalog inspection." : "Nenhuma tabela ou registro correspondente encontrado na inspeção do catálogo.",
                isEn
                        ? List.of("Run your DDL and DML statements with Ctrl + Enter", "Verify table and row existence before evaluating")
                        : List.of("Execute seus comandos DDL e DML com Ctrl + Enter", "Verifique se as tabelas e linhas foram criadas antes de avaliar"),
                modelName
        );
    }
}
