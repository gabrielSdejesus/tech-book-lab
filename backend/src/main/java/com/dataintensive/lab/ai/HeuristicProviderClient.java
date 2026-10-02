package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.AssessmentLanguage;
import com.dataintensive.lab.domain.Challenge;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class HeuristicProviderClient implements AiProviderClient {

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

        // 2. Análise específica por desafio conhecido
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

        // 3. Fallback heurístico genérico para qualquer laboratório
        return evaluateGeneric(normalizedQuery, lab, challenge, isEn, modelName);
    }

    private AiAssessmentResponse evaluate3NF(String query, boolean isEn, String modelName) {
        boolean hasTables = query.contains("CREATE TABLE") && query.contains("USUARIOS");
        boolean hasRelationships = query.contains("REFERENCES") || query.contains("FOREIGN KEY");
        boolean hasJoins = query.contains("JOIN");

        if (hasTables && (hasRelationships || hasJoins)) {
            return new AiAssessmentResponse(
                    "APPROVED",
                    isEn
                            ? "Excellent 3NF relational modeling! You separated normalized entities with primary and foreign keys, preventing update anomalies."
                            : "Excelente modelagem 3NF! Você separou as entidades normalizadas com chaves primárias e estrangeiras, eliminando redundâncias e anomalias de atualização.",
                    isEn
                            ? "As Martin Kleppmann highlights in Chapter 3, 3NF optimizes write consistency at the cost of multiple JOINs and disk scattering on read."
                            : "Conforme Martin Kleppmann explica no Capítulo 3, o modelo relacional 3NF otimiza a consistência nas escritas ao custo de exigir múltiplos JOINs e dispersar os registros em disco durante leituras.",
                    isEn ? "Queries with joins benefit from foreign key indexes on dependent tables." : "Consultas com junções se beneficiam fortemente de índices nas colunas de chave estrangeira.",
                    isEn
                            ? List.of("Consider creating composite indexes on (usuario_id, ano_inicio) for filtered profile views", "Analyze the query plan with EXPLAIN ANALYZE")
                            : List.of("Considere criar índices compostos nas chaves estrangeiras", "Analise o plano de execução gerado com EXPLAIN ANALYZE"),
                    modelName
            );
        }

        return new AiAssessmentResponse(
                "NEEDS_REVISION",
                isEn
                        ? "The 3NF model requires separate normalized tables with foreign keys and a multi-table JOIN query to reconstruct the profile."
                        : "A modelagem 3NF requer tabelas separadas e normalizadas com chaves estrangeiras e uma consulta com JOIN para reconstruir o perfil.",
                isEn
                        ? "Normalization divides data into distinct tables to eliminate redundancy, but requires explicit foreign keys."
                        : "A normalização divide os dados em entidades distintas para eliminar duplicações, mas exige chaves estrangeiras explícitas.",
                isEn ? "Without foreign keys and JOINs, relational integrity cannot be ensured." : "Sem chaves estrangeiras e relacionamentos com JOIN, a integridade relacional não pode ser validada.",
                isEn
                        ? List.of("Create usuarios, experiencias_profissionais and formacoes_academicas tables", "Use REFERENCES usuarios(id) on child tables")
                        : List.of("Crie as tabelas usuarios, experiencias_profissionais e formacoes_academicas", "Utilize REFERENCES usuarios(id) nas tabelas dependentes"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateJsonb(String query, boolean isEn, String modelName) {
        boolean hasJsonb = query.contains("JSONB");
        boolean hasOperators = query.contains("->") || query.contains("@>") || query.contains("JSONB_");

        if (hasJsonb || hasOperators) {
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

        return new AiAssessmentResponse(
                "NEEDS_REVISION",
                isEn
                        ? "The document model requires a JSONB column to store embedded user profiles."
                        : "O modelo de documento requer uma coluna do tipo JSONB para armazenar o perfil completo de forma embutida.",
                isEn
                        ? "Storage locality in document stores avoids joining separate tables by collocating all related data."
                        : "A localidade de armazenamento em bancos de documentos evita joins colapsando dados relacionados na mesma estrutura física.",
                isEn ? "Ensure you declare a JSONB column." : "Certifique-se de declarar uma coluna do tipo JSONB.",
                isEn
                        ? List.of("Declare 'perfil JSONB' in your CREATE TABLE statement", "Query nested fields using ->> or @> operators")
                        : List.of("Declare 'perfil JSONB' no comando CREATE TABLE", "Consulte campos internos usando operadores ->> ou @>"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateCypher(String query, boolean isEn, String modelName) {
        boolean hasCreateOrMatch = query.contains("CREATE") || query.contains("MATCH");
        boolean hasLabelsOrRelationships = query.contains(":") && query.contains("-");

        if (hasCreateOrMatch && hasLabelsOrRelationships) {
            return new AiAssessmentResponse(
                    "APPROVED",
                    isEn
                            ? "Excellent property graph implementation! You modeled hierarchical connections with Cypher and traversed relationships smoothly."
                            : "Excelente consulta em grafos de propriedades! Você utilizou Cypher para expressar conexões hierárquicas e travessias entre nós com maestria.",
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

        return new AiAssessmentResponse(
                "NEEDS_REVISION",
                isEn
                        ? "The Cypher solution requires creating nodes and directed relationships, followed by a MATCH query traversing paths."
                        : "A solução em Cypher requer a criação de nós e arestas direcionadas, seguida de uma consulta MATCH para navegar pelo grafo.",
                isEn
                        ? "In property graphs, both vertices and edges have identities and properties."
                        : "Nos grafos de propriedades, nós e relacionamentos possuem identidades próprias e propriedades associadas.",
                isEn ? "Check node labels (:Location, :Person) and directed edges (->)." : "Verifique os rótulos dos nós (:Location, :Person) e arestas direcionadas (->).",
                isEn
                        ? List.of("Create nodes with (n:Label {key: 'value'})", "Connect nodes using (a)-[:RELATIONSHIP]->(b)")
                        : List.of("Crie nós com (n:Label {chave: 'valor'})", "Conecte nós com arestas direcionadas (a)-[:RELATIONSHIP]->(b)"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateRecursiveCte(String query, boolean isEn, String modelName) {
        boolean hasRecursive = query.contains("WITH RECURSIVE");
        boolean hasUnion = query.contains("UNION");

        if (hasRecursive && hasUnion) {
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

        return new AiAssessmentResponse(
                "NEEDS_REVISION",
                isEn
                        ? "A recursive CTE query requires 'WITH RECURSIVE' and a 'UNION ALL' clause combining anchor and recursive terms."
                        : "A consulta recursiva requer a cláusula 'WITH RECURSIVE' e 'UNION ALL' conectando o termo âncora ao termo recursivo.",
                isEn
                        ? "Relational databases navigate graph hierarchies through iterative self-joins in CTEs."
                        : "Bancos relacionais navegam hierarquias em grafos por meio de auto-junções iterativas em CTEs.",
                isEn ? "Missing WITH RECURSIVE or UNION ALL." : "Faltam as cláusulas WITH RECURSIVE ou UNION ALL.",
                isEn
                        ? List.of("Define anchor member: SELECT id, parent_id FROM locais WHERE ...", "Add recursive member: UNION ALL SELECT l.id, l.parent_id FROM locais l JOIN cte c ON l.id = c.parent_id")
                        : List.of("Defina o membro âncora inicial da hierarquia", "Conecte com UNION ALL e faça o JOIN da tabela com a própria CTE recursiva"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateStarSchema(String query, boolean isEn, String modelName) {
        boolean hasDim = query.contains("DIM_") || query.contains("DIMENSAO");
        boolean hasFact = query.contains("FATO_") || query.contains("FATOS");

        if (hasDim || hasFact) {
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

        return new AiAssessmentResponse(
                "NEEDS_REVISION",
                isEn
                        ? "Star Schema modeling requires creating denormalized dimension tables and a central fact table."
                        : "A modelagem Star Schema requer tabelas de dimensões desnormalizadas (dim_*) e uma tabela central de fatos (fato_*).",
                isEn
                        ? "OLAP architectures prioritize query readability and aggregation speed over strict third normal form."
                        : "Arquiteturas analíticas (OLAP) priorizam velocidade de agregação e simplicidade de leitura sobre a forma normal estrita.",
                isEn ? "Create dimension tables (dim_tempo, dim_produto) and fact table (fato_vendas)." : "Crie as tabelas de dimensões (dim_tempo, dim_produto) e a tabela de fatos.",
                isEn
                        ? List.of("Model dim_tempo, dim_produto, dim_loja", "Create fato_vendas with foreign keys to all dimensions")
                        : List.of("Modele dim_tempo, dim_produto, dim_loja", "Crie fato_vendas com chaves estrangeiras para todas as dimensões"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateSliceDice(String query, boolean isEn, String modelName) {
        boolean hasSelect = query.contains("SELECT");
        boolean hasAgg = query.contains("SUM") || query.contains("COUNT") || query.contains("AVG");
        boolean hasGroup = query.contains("GROUP BY");

        if (hasSelect && hasAgg && hasGroup) {
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

        return new AiAssessmentResponse(
                "NEEDS_REVISION",
                isEn
                        ? "Slice & Dice analytics require aggregate functions (SUM, AVG) coupled with a GROUP BY clause across dimension columns."
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

    private AiAssessmentResponse evaluateEventStore(String query, boolean isEn, String modelName) {
        boolean hasCreate = query.contains("CREATE TABLE");
        boolean hasEvent = query.contains("EVENT") || query.contains("PEDIDO");

        if (hasCreate && hasEvent) {
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

        return new AiAssessmentResponse(
                "NEEDS_REVISION",
                isEn
                        ? "Event Sourcing requires an immutable event table storing event types, payloads, and timestamps."
                        : "Event Sourcing requer uma tabela de eventos imutável registrando tipo do evento, dados e timestamp.",
                isEn
                        ? "Event logs represent system state as an ordered sequence of domain events rather than mutable state."
                        : "Logs de eventos representam o estado como uma sequência ordenada de ocorrências de domínio em vez de mutações diretas.",
                isEn ? "Declare an append-only event table." : "Declare uma tabela append-only para registro de eventos.",
                isEn
                        ? List.of("Create pedidos_eventos with (id, pedido_id, tipo_evento, dados_evento, criado_em)", "Insert realistic lifecycle events")
                        : List.of("Crie a tabela pedidos_eventos com campos de evento", "Insira eventos representando o ciclo de vida do pedido"),
                modelName
        );
    }

    private AiAssessmentResponse evaluateCqrsView(String query, boolean isEn, String modelName) {
        boolean hasView = query.contains("VIEW") || query.contains("PROJECAO");

        if (hasView) {
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

        return new AiAssessmentResponse(
                "NEEDS_REVISION",
                isEn
                        ? "CQRS read modeling requires creating a materialized view or projection table derived from the event store."
                        : "A modelagem de leitura CQRS requer uma visão materializada ou tabela de projeção derivada do log de eventos.",
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
}
