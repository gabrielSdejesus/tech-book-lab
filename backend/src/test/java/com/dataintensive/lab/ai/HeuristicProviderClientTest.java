package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.AssessmentLanguage;
import com.dataintensive.lab.domain.Challenge;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeuristicProviderClientTest {

    private DatabaseCatalogInspector mockInspector;
    private HeuristicProviderClient client;

    @BeforeEach
    void setUp() {
        mockInspector = mock(DatabaseCatalogInspector.class);
        client = new HeuristicProviderClient(mockInspector);
    }

    @Test
    @DisplayName("Deve retornar metadados corretos do provedor Heurístico")
    void shouldReturnMetadataCorrectly() {
        assertThat(client.getProviderId()).isEqualTo("heuristic");
        assertThat(client.isConfigured(null)).isTrue();
        assertThat(client.isConfigured("any_key")).isTrue();

        AiProviderInfo ptInfo = client.getInfo(AssessmentLanguage.PT);
        assertThat(ptInfo.name()).contains("Heurístico");
        assertThat(ptInfo.requiresApiKey()).isFalse();
        assertThat(ptInfo.description()).contains("offline");

        AiProviderInfo enInfo = client.getInfo(AssessmentLanguage.EN);
        assertThat(enInfo.name()).contains("Heuristic");
        assertThat(enInfo.requiresApiKey()).isFalse();
        assertThat(enInfo.description()).contains("offline");
    }

    @Test
    @DisplayName("testConnection deve retornar sucesso operacional imediato")
    void shouldSucceedTestConnection() {
        AiTestConnectionResponse response = client.testConnection(new AiTestConnectionRequest("heuristic", null, null));

        assertThat(response.valid()).isTrue();
        assertThat(response.message()).containsIgnoringCase("heurístico");
    }

    @Test
    @DisplayName("3NF: Deve rejeitar (NEEDS_REVISION) quando utilizar apenas JOIN interno em vez de LEFT JOIN")
    void shouldRejectPostgres3NFWhenUsingInnerJoinInsteadOfLeftJoin() {
        Lab lab = createLab("ddia-cap-03-lab-01", EngineType.POSTGRES);
        Challenge ch = createChallenge("lab-01-ch-1", EngineType.POSTGRES);

        String userQuery = """
                CREATE TABLE usuarios (id INT PRIMARY KEY, nome VARCHAR(100));
                CREATE TABLE experiencias_profissionais (id INT PRIMARY KEY, usuario_id INT REFERENCES usuarios(id), cargo VARCHAR(100));
                INSERT INTO usuarios VALUES (1, 'Ana'), (2, 'Beto');
                INSERT INTO experiencias_profissionais VALUES (1, 1, 'Engenheira');
                SELECT u.nome, e.cargo FROM usuarios u
                JOIN experiencias_profissionais e ON e.usuario_id = u.id;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("LEFT JOIN");
    }

    @Test
    @DisplayName("3NF: Deve rejeitar quando a sintaxe estiver correta mas tabelas/dados não existirem no banco de dados")
    void shouldRejectPostgres3NFWhenTablesOrDataDoNotExistInDatabase() {
        Lab lab = createLab("ddia-cap-03-lab-01", EngineType.POSTGRES);
        Challenge ch = createChallenge("lab-01-ch-1", EngineType.POSTGRES);

        String userQuery = """
                CREATE TABLE usuarios (id INT PRIMARY KEY, nome VARCHAR(100));
                CREATE TABLE experiencias_profissionais (id INT PRIMARY KEY, usuario_id INT REFERENCES usuarios(id), cargo VARCHAR(100));
                INSERT INTO usuarios VALUES (1, 'Ana'), (2, 'Beto');
                SELECT u.nome, e.cargo FROM usuarios u
                LEFT JOIN experiencias_profissionais e ON e.usuario_id = u.id;
                """;

        when(mockInspector.tableExists(eq(EngineType.POSTGRES), eq("usuarios"))).thenReturn(false);

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("Ctrl + Enter");
    }

    @Test
    @DisplayName("3NF: Deve aprovar quando sintaxe contiver LEFT JOIN, FKs, INSERT e banco confirmar dados")
    void shouldApproveValidPostgres3NFSolutionWhenSyntaxAndDatabaseAreValid() {
        Lab lab = createLab("ddia-cap-03-lab-01", EngineType.POSTGRES);
        Challenge ch = createChallenge("lab-01-ch-1", EngineType.POSTGRES);

        String userQuery = """
                CREATE TABLE usuarios (id INT PRIMARY KEY, nome VARCHAR(100));
                CREATE TABLE experiencias_profissionais (id INT PRIMARY KEY, usuario_id INT REFERENCES usuarios(id), cargo VARCHAR(100));
                INSERT INTO usuarios VALUES (1, 'Ana'), (2, 'Beto');
                SELECT u.nome, e.cargo FROM usuarios u
                LEFT JOIN experiencias_profissionais e ON e.usuario_id = u.id;
                """;

        when(mockInspector.tableExists(eq(EngineType.POSTGRES), eq("usuarios"))).thenReturn(true);
        when(mockInspector.foreignKeyExists(eq(EngineType.POSTGRES), eq("experiencias_profissionais"), eq("usuarios"))).thenReturn(true);
        when(mockInspector.getRowCount(eq(EngineType.POSTGRES), eq("usuarios"))).thenReturn(2L);

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "Trade-off", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("normaliz");
    }

    @Test
    @DisplayName("JSONB: Deve rejeitar quando tabela com tipo JSONB não for detectada no catálogo do banco")
    void shouldRejectJsonbWhenTableDoesNotExistInDatabase() {
        Lab lab = createLab("ddia-cap-03-lab-01", EngineType.POSTGRES);
        Challenge ch = createChallenge("lab-01-ch-2", EngineType.POSTGRES);

        String userQuery = """
                CREATE TABLE usuarios_documento (id INT PRIMARY KEY, perfil JSONB);
                INSERT INTO usuarios_documento VALUES (1, '{"nome": "Ana"}'), (2, '{"nome": "Beto"}');
                SELECT id, perfil->>'nome' AS nome FROM usuarios_documento WHERE perfil @> '{"nome": "Ana"}';
                """;

        when(mockInspector.columnExists(eq(EngineType.POSTGRES), eq("usuarios_documento"), eq("perfil"), eq("jsonb"))).thenReturn(false);

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("Ctrl + Enter");
    }

    @Test
    @DisplayName("JSONB: Deve aprovar quando operadores de documento forem usados e coluna existir no banco")
    void shouldApproveValidPostgresJsonbSolution() {
        Lab lab = createLab("ddia-cap-03-lab-01", EngineType.POSTGRES);
        Challenge ch = createChallenge("lab-01-ch-2", EngineType.POSTGRES);

        String userQuery = """
                CREATE TABLE usuarios_documento (id INT PRIMARY KEY, perfil JSONB);
                INSERT INTO usuarios_documento VALUES (1, '{"nome": "Ana"}'), (2, '{"nome": "Beto"}');
                SELECT id, perfil->>'nome' AS nome FROM usuarios_documento WHERE perfil @> '{"nome": "Ana"}';
                """;

        when(mockInspector.columnExists(eq(EngineType.POSTGRES), eq("usuarios_documento"), eq("perfil"), eq("jsonb"))).thenReturn(true);
        when(mockInspector.getRowCount(eq(EngineType.POSTGRES), eq("usuarios_documento"))).thenReturn(2L);

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "Trade-off", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("jsonb");
    }

    @Test
    @DisplayName("Cypher: Deve rejeitar quando faltar operador de profundidade variável (*1..5)")
    void shouldRejectCypherWhenVariableLengthPathIsMissing() {
        Lab lab = createLab("ddia-cap-03-lab-02", EngineType.NEO4J);
        Challenge ch = createChallenge("lab-02-ch-1", EngineType.NEO4J);

        String userQuery = """
                CREATE (c:Location {name: 'Sao Paulo'}), (p:Person {name: 'Gabriel'}), (c)-[:WITHIN]->(b:Location {name: 'Brasil'});
                MATCH (p:Person)-[:BORN_IN]->(c:Location)-[:WITHIN]->(country:Location)
                RETURN p.name, country.name;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("profundidade variável");
    }

    @Test
    @DisplayName("Cypher: Deve rejeitar quando os nós não existirem no Neo4j")
    void shouldRejectCypherWhenNodesDoNotExistInNeo4j() {
        Lab lab = createLab("ddia-cap-03-lab-02", EngineType.NEO4J);
        Challenge ch = createChallenge("lab-02-ch-1", EngineType.NEO4J);

        String userQuery = """
                CREATE (c:Location {name: 'Sao Paulo'}), (s:Location {name: 'SP'}), (p:Person {name: 'Gabriel'});
                MATCH (p:Person)-[:BORN_IN]->(:Location)-[:WITHIN*1..5]->(country:Location)
                RETURN p.name, country.name;
                """;

        when(mockInspector.countNeo4jNodes(anyString())).thenReturn(0L);

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("Ctrl + Enter");
    }

    @Test
    @DisplayName("Cypher: Deve aprovar quando contiver profundidade variável e nós existirem no Neo4j")
    void shouldApproveValidNeo4jCypherSolution() {
        Lab lab = createLab("ddia-cap-03-lab-02", EngineType.NEO4J);
        Challenge ch = createChallenge("lab-02-ch-1", EngineType.NEO4J);

        String userQuery = """
                CREATE (c:Location {name: 'Sao Paulo'}), (s:Location {name: 'SP'}), (p:Person {name: 'Gabriel'});
                MATCH (p:Person)-[:BORN_IN]->(:Location)-[:WITHIN*1..5]->(country:Location)
                RETURN p.name, country.name;
                """;

        when(mockInspector.countNeo4jNodes("Person")).thenReturn(1L);
        when(mockInspector.countNeo4jNodes("Location")).thenReturn(2L);
        when(mockInspector.countNeo4jRelationships(anyString())).thenReturn(2L);

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("grafo");
    }

    @Test
    @DisplayName("Recursive CTE: Deve rejeitar quando faltar auto-relacionamento parent_id ou UNION ALL")
    void shouldRejectRecursiveCteWhenParentIdOrRecursiveUnionIsMissing() {
        Lab lab = createLab("ddia-cap-03-lab-02", EngineType.POSTGRES);
        Challenge ch = createChallenge("lab-02-ch-2", EngineType.POSTGRES);

        String userQuery = """
                WITH RECURSIVE hierarquia AS (
                    SELECT id, nome FROM locais WHERE id = 1
                )
                SELECT * FROM hierarquia;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("UNION");
    }

    @Test
    @DisplayName("Star Schema: Deve rejeitar quando contiver apenas dimensões sem tabela de fatos (eliminação do ||)")
    void shouldRejectStarSchemaWhenMissingFactTable() {
        Lab lab = createLab("ddia-cap-03-lab-03", EngineType.POSTGRES);
        Challenge ch = createChallenge("lab-03-ch-1", EngineType.POSTGRES);

        String userQuery = """
                CREATE TABLE dim_tempo (id INT PRIMARY KEY, ano INT);
                CREATE TABLE dim_produto (id INT PRIMARY KEY, nome VARCHAR(50));
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("fato");
    }

    @Test
    @DisplayName("Star Schema: Deve aprovar quando fatos e dimensões coexistirem e existirem no banco")
    void shouldApproveStarSchemaWhenFactAndDimensionsCoexistAndExistInDb() {
        Lab lab = createLab("ddia-cap-03-lab-03", EngineType.POSTGRES);
        Challenge ch = createChallenge("lab-03-ch-1", EngineType.POSTGRES);

        String userQuery = """
                CREATE TABLE dim_tempo (id INT PRIMARY KEY, ano INT);
                CREATE TABLE dim_produto (id INT PRIMARY KEY, nome VARCHAR(50));
                CREATE TABLE fato_vendas (
                    id INT PRIMARY KEY,
                    tempo_id INT REFERENCES dim_tempo(id),
                    produto_id INT REFERENCES dim_produto(id),
                    quantidade INT,
                    valor_total NUMERIC(10,2)
                );
                INSERT INTO fato_vendas VALUES (1, 1, 1, 10, 100.00);
                """;

        when(mockInspector.tableExists(eq(EngineType.POSTGRES), eq("fato_vendas"))).thenReturn(true);
        when(mockInspector.getRowCount(eq(EngineType.POSTGRES), eq("fato_vendas"))).thenReturn(1L);

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("Star Schema");
    }

    @Test
    @DisplayName("Event Store: Deve rejeitar sumariamente com anti-padrão quando contiver UPDATE ou DELETE")
    void shouldRejectEventStoreWhenUpdateOrDeleteIsUsed() {
        Lab lab = createLab("ddia-cap-03-lab-04", EngineType.POSTGRES);
        Challenge ch = createChallenge("lab-04-ch-1", EngineType.POSTGRES);

        String userQuery = """
                CREATE TABLE pedidos_eventos (id INT PRIMARY KEY, pedido_id INT, tipo_evento VARCHAR(50), criado_em TIMESTAMP);
                INSERT INTO pedidos_eventos VALUES (1, 10, 'CRIADO', NOW());
                UPDATE pedidos_eventos SET tipo_evento = 'CANCELADO' WHERE id = 1;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("append-only");
        assertThat(response.feedback()).containsIgnoringCase("UPDATE");
    }

    @Test
    @DisplayName("CQRS View: Deve rejeitar quando faltar comando CREATE VIEW ou MATERIALIZED VIEW")
    void shouldRejectCqrsWhenMaterializedViewIsMissing() {
        Lab lab = createLab("ddia-cap-03-lab-04", EngineType.POSTGRES);
        Challenge ch = createChallenge("lab-04-ch-2", EngineType.POSTGRES);

        String userQuery = "SELECT pedido_id, COUNT(*) FROM pedidos_eventos GROUP BY pedido_id;";

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("VIEW");
    }

    @Test
    @DisplayName("CQRS View: Deve aprovar quando view materializada for declarada e existir no banco")
    void shouldApproveCqrsWhenMaterializedViewExists() {
        Lab lab = createLab("ddia-cap-03-lab-04", EngineType.POSTGRES);
        Challenge ch = createChallenge("lab-04-ch-2", EngineType.POSTGRES);

        String userQuery = """
                CREATE MATERIALIZED VIEW pedidos_resumo_leitura AS
                SELECT pedido_id, COUNT(*) AS total_eventos FROM pedidos_eventos GROUP BY pedido_id;
                """;

        when(mockInspector.viewExists(eq(EngineType.POSTGRES), anyString())).thenReturn(true);

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("CQRS");
    }

    @Test
    @DisplayName("Heurístico: Deve anexar a resposta de reflexão padrão (gabarito) do desafio no resultado da avaliação")
    void shouldAttachExpectedReflectionFromChallengeInAssessmentResponse() {
        Lab lab = createLab("ddia-cap-03-lab-01", EngineType.POSTGRES);
        Challenge ch = new Challenge(
                "lab-01-ch-1",
                1,
                "Modelagem 3NF",
                "Descrição",
                "Cenário",
                "SELECT 1;",
                null,
                List.of(),
                "Pergunta Reflexiva",
                "Gabarito oficial de trade-off para 3NF",
                EngineType.POSTGRES
        );

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), "SELECT 1;", "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.expectedReflection()).isEqualTo("Gabarito oficial de trade-off para 3NF");
    }

    private Lab createLab(String id, EngineType engine) {
        return new Lab(id, 1, "slug", "Lab Title", "Summary", List.of("Topic"), engine, "tbl", null, List.of());
    }

    private Challenge createChallenge(String id, EngineType engine) {
        return new Challenge(id, 1, "Challenge Title", "Description", "Requirements", "Initial Code", List.of(), "Reflection", engine);
    }
}
