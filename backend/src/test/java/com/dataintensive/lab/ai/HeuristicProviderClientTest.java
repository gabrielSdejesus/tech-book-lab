package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.AssessmentLanguage;
import com.dataintensive.lab.domain.Challenge;
import com.dataintensive.lab.domain.Lab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicProviderClientTest {

    private HeuristicProviderClient client;

    @BeforeEach
    void setUp() {
        client = new HeuristicProviderClient();
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
        Lab lab = createLab("ddia-cap-03-lab-01");
        Challenge ch = createChallenge("lab-01-ch-1");

        String userQuery = """
                SELECT u.nome, e.cargo
                FROM usuarios u
                JOIN experiencias_profissionais e ON e.usuario_id = u.id;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("LEFT JOIN");
    }

    @Test
    @DisplayName("3NF: Deve aprovar quando sintaxe contiver LEFT JOIN, tabelas, FKs e INSERTs")
    void shouldApproveValidPostgres3NFSolution() {
        Lab lab = createLab("ddia-cap-03-lab-01");
        Challenge ch = createChallenge("lab-01-ch-1");

        String userQuery = """
                CREATE TABLE usuarios (id INT PRIMARY KEY, nome VARCHAR(100));
                CREATE TABLE experiencias_profissionais (id INT PRIMARY KEY, usuario_id INT REFERENCES usuarios(id), cargo VARCHAR(100));
                INSERT INTO usuarios VALUES (1, 'Ana'), (2, 'Beto');
                SELECT u.nome, e.cargo FROM usuarios u
                LEFT JOIN experiencias_profissionais e ON e.usuario_id = u.id;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "Trade-off", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("3NF");
    }

    @Test
    @DisplayName("JSONB: Deve rejeitar quando faltar declaração JSONB ou operadores")
    void shouldRejectJsonbWhenMissingJsonbColumnOrOperators() {
        Lab lab = createLab("ddia-cap-03-lab-01");
        Challenge ch = createChallenge("lab-01-ch-2");

        String userQuery = """
                CREATE TABLE usuarios_documento (id INT PRIMARY KEY, nome VARCHAR(100));
                INSERT INTO usuarios_documento VALUES (1, 'Ana');
                SELECT * FROM usuarios_documento;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("JSONB");
    }

    @Test
    @DisplayName("JSONB: Deve aprovar quando contiver coluna JSONB, INSERT e operadores de consulta")
    void shouldApproveValidJsonbSolution() {
        Lab lab = createLab("ddia-cap-03-lab-01");
        Challenge ch = createChallenge("lab-01-ch-2");

        String userQuery = """
                CREATE TABLE usuarios_documento (id INT PRIMARY KEY, perfil JSONB);
                INSERT INTO usuarios_documento VALUES (1, '{"nome": "Ana"}'), (2, '{"nome": "Beto"}');
                SELECT id, perfil->>'nome' AS nome FROM usuarios_documento WHERE perfil @> '{"nome": "Ana"}';
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "Trade-off", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("JSONB");
    }

    @Test
    @DisplayName("Cypher: Deve rejeitar quando não utilizar operador de profundidade variável")
    void shouldRejectCypherWhenNotUsingVariablePath() {
        Lab lab = createLab("ddia-cap-03-lab-02");
        Challenge ch = createChallenge("lab-02-ch-1");

        String userQuery = """
                MATCH (p:Person {name: 'Alice'})-[:WITHIN]->(city:Location)-[:WITHIN]->(country:Location)
                RETURN country.name;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("profundidade variável");
    }

    @Test
    @DisplayName("Cypher: Deve aprovar solução válida com nós, relacionamentos e profundidade variável")
    void shouldApproveValidCypherSolution() {
        Lab lab = createLab("ddia-cap-03-lab-02");
        Challenge ch = createChallenge("lab-02-ch-1");

        String userQuery = """
                CREATE (p:Person {name: 'Alice'}), (l:Location {name: 'Brasil'})
                CREATE (p)-[:BORN_IN]->(l)
                MATCH (p:Person)-[:WITHIN*1..5]->(c:Location)
                RETURN c.name;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("propriedades");
    }

    @Test
    @DisplayName("Recursive CTE: Deve rejeitar quando faltar WITH RECURSIVE ou UNION")
    void shouldRejectRecursiveCteWhenMissingWithRecursive() {
        Lab lab = createLab("ddia-cap-03-lab-02");
        Challenge ch = createChallenge("lab-02-ch-2");

        String userQuery = "SELECT id, parent_id FROM locais WHERE id = 1;";

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("WITH RECURSIVE");
    }

    @Test
    @DisplayName("Recursive CTE: Deve aprovar solução válida com WITH RECURSIVE, UNION e parent_id")
    void shouldApproveValidRecursiveCte() {
        Lab lab = createLab("ddia-cap-03-lab-02");
        Challenge ch = createChallenge("lab-02-ch-2");

        String userQuery = """
                WITH RECURSIVE local_hierarquia AS (
                    SELECT id, nome, parent_id FROM locais WHERE id = 1
                    UNION ALL
                    SELECT l.id, l.nome, l.parent_id FROM locais l
                    JOIN local_hierarquia h ON l.id = h.parent_id
                )
                SELECT * FROM local_hierarquia;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Star Schema: Deve rejeitar quando faltar tabela de dimensões ou fato")
    void shouldRejectStarSchemaWhenMissingFactOrDimension() {
        Lab lab = createLab("ddia-cap-03-lab-03");
        Challenge ch = createChallenge("lab-03-ch-1");

        String userQuery = "CREATE TABLE fato_vendas (id INT, valor NUMERIC);";

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("dimensões");
    }

    @Test
    @DisplayName("Star Schema: Deve aprovar modelagem com fato, dimensão, FK e inserts")
    void shouldApproveValidStarSchema() {
        Lab lab = createLab("ddia-cap-03-lab-03");
        Challenge ch = createChallenge("lab-03-ch-1");

        String userQuery = """
                CREATE TABLE dim_tempo (id INT PRIMARY KEY, ano INT);
                CREATE TABLE fato_vendas (id INT, dim_tempo_id INT REFERENCES dim_tempo(id));
                INSERT INTO dim_tempo VALUES (1, 2026);
                INSERT INTO fato_vendas VALUES (1, 1);
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("Star Schema");
    }

    @Test
    @DisplayName("Slice and Dice: Deve validar agregações analíticas com GROUP BY")
    void shouldValidateSliceAndDiceAnalytics() {
        Lab lab = createLab("ddia-cap-03-lab-03");
        Challenge ch = createChallenge("lab-03-ch-2");

        String validQuery = """
                SELECT t.ano, SUM(f.valor)
                FROM fato_vendas f
                JOIN dim_tempo t ON t.id = f.dim_tempo_id
                GROUP BY t.ano;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), validQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("Slice & Dice");
    }

    @Test
    @DisplayName("Event Store: Deve rejeitar mutabilidade destrutiva (UPDATE ou DELETE)")
    void shouldRejectMutableOperationsInEventStore() {
        Lab lab = createLab("ddia-cap-03-lab-04");
        Challenge ch = createChallenge("lab-04-ch-1");

        String userQuery = "UPDATE pedidos_eventos SET status = 'CANCELADO' WHERE id = 1;";

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("append-only");
    }

    @Test
    @DisplayName("Event Store: Deve aprovar solução append-only com INSERT e CREATE TABLE")
    void shouldApproveValidAppendOnlyEventStore() {
        Lab lab = createLab("ddia-cap-03-lab-04");
        Challenge ch = createChallenge("lab-04-ch-1");

        String userQuery = """
                CREATE TABLE pedidos_eventos (id INT, pedido_id INT, tipo_evento VARCHAR(50), payload TEXT);
                INSERT INTO pedidos_eventos VALUES (1, 100, 'PEDIDO_CRIADO', '{}');
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("Event Store");
    }

    @Test
    @DisplayName("CQRS View: Deve rejeitar quando faltar definição de visão ou tabela de leitura")
    void shouldRejectCqrsWhenMissingView() {
        Lab lab = createLab("ddia-cap-03-lab-04");
        Challenge ch = createChallenge("lab-04-ch-2");

        String userQuery = "SELECT * FROM pedidos_eventos;";

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("visão materializada");
    }

    @Test
    @DisplayName("CQRS View: Deve aprovar criação de MATERIALIZED VIEW para projeção")
    void shouldApproveCqrsMaterializedView() {
        Lab lab = createLab("ddia-cap-03-lab-04");
        Challenge ch = createChallenge("lab-04-ch-2");

        String userQuery = """
                CREATE MATERIALIZED VIEW pedidos_resumo_leitura AS
                SELECT pedido_id, COUNT(*) AS total_eventos
                FROM pedidos_eventos
                GROUP BY pedido_id;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), userQuery, "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("CQRS");
    }

    @Test
    @DisplayName("Deve anexar expectedReflection quando presente no desafio")
    void shouldAttachExpectedReflectionFromChallengeInAssessmentResponse() {
        Lab lab = createLab("ddia-cap-03-lab-01");
        Challenge ch = new Challenge(
                "lab-01-ch-1",
                1,
                "Modelagem 3NF",
                "Descrição",
                "Cenário",
                "SELECT 1;",
                null,
                null,
                List.of(),
                "Pergunta Reflexiva",
                "Gabarito oficial de trade-off para 3NF"
        );

        AiAssessmentRequest request = new AiAssessmentRequest(lab.id(), ch.id(), "SELECT 1;", "ok", "", "heuristic", null, null, "pt");
        AiAssessmentResponse response = client.assess(lab, ch, request, AssessmentLanguage.PT);

        assertThat(response.expectedReflection()).isEqualTo("Gabarito oficial de trade-off para 3NF");
    }

    private Lab createLab(String id) {
        return new Lab(id, 1, "slug", "Lab Title", "Summary", List.of("Topic"), "tbl", null, List.of());
    }

    private Challenge createChallenge(String id) {
        return new Challenge(id, 1, "Challenge Title", "Description", "Requirements", "Initial Code", List.of(), "Reflection");
    }
}
