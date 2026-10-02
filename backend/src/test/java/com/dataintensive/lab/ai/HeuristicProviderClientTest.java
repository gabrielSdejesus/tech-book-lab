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
    @DisplayName("Deve aprovar solução válida para modelagem 3NF (PostgreSQL)")
    void shouldApproveValidPostgres3NFSolution() {
        Lab lab = new Lab("ddia-cap-03-lab-01", 1, "relacional-vs-documentos", "Relacional vs Documentos", "Sumário",
                List.of("Normalização 3NF", "Localidade de Armazenamento"), EngineType.POSTGRES, "tbl_lab", null, List.of());

        Challenge challenge = new Challenge("lab-01-ch-1", 1, "Modelagem 3NF", "Modele o perfil profissional normalizado em 3NF",
                "Crie tabelas usuarios, experiencias_profissionais e formacoes_academicas com FKs",
                "CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY);", List.of(), "Qual o trade-off de normalização vs desnormalização?", EngineType.POSTGRES);

        String userQuery = """
                CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY, nome VARCHAR(100));
                CREATE TABLE IF NOT EXISTS experiencias_profissionais (id INT PRIMARY KEY, usuario_id INT REFERENCES usuarios(id), cargo VARCHAR(100));
                CREATE TABLE IF NOT EXISTS formacoes_academicas (id INT PRIMARY KEY, usuario_id INT REFERENCES usuarios(id), curso VARCHAR(100));
                SELECT u.nome, e.cargo, f.curso FROM usuarios u
                LEFT JOIN experiencias_profissionais e ON e.usuario_id = u.id
                LEFT JOIN formacoes_academicas f ON f.usuario_id = u.id;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(
                lab.id(), challenge.id(), userQuery, "3 linhas retornadas com sucesso.", "3NF previne anomalias de atualização mas exige joins na leitura.", "heuristic", null, null, "pt"
        );

        AiAssessmentResponse response = client.assess(lab, challenge, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("normaliz");
        assertThat(response.tradeOffAnalysis()).isNotEmpty();
        assertThat(response.modelUsed()).containsIgnoringCase("Heurístico");
    }

    @Test
    @DisplayName("Deve aprovar solução válida para modelagem com JSONB (PostgreSQL)")
    void shouldApproveValidPostgresJsonbSolution() {
        Lab lab = new Lab("ddia-cap-03-lab-01", 1, "relacional-vs-documentos", "Relacional vs Documentos", "Sumário",
                List.of("PostgreSQL JSONB", "Localidade de Armazenamento"), EngineType.POSTGRES, "tbl_lab", null, List.of());

        Challenge challenge = new Challenge("lab-01-ch-2", 2, "Modelagem JSONB", "Modele o perfil com documento JSONB",
                "Crie tabela com coluna JSONB e busque perfis",
                "CREATE TABLE IF NOT EXISTS usuarios_documento (id INT PRIMARY KEY, perfil JSONB);", List.of(), "Trade-off de localidade", EngineType.POSTGRES);

        String userQuery = """
                CREATE TABLE IF NOT EXISTS usuarios_documento (id INT PRIMARY KEY, perfil JSONB);
                INSERT INTO usuarios_documento VALUES (1, '{"nome": "Ana", "experiencias": [{"cargo": "Dev"}]}');
                SELECT id, perfil->>'nome' AS nome FROM usuarios_documento WHERE perfil @> '{"nome": "Ana"}';
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(
                lab.id(), challenge.id(), userQuery, "1 linha retornada.", "JSONB favorece localidade de leitura em 1 acesso a disco.", "heuristic", null, null, "pt"
        );

        AiAssessmentResponse response = client.assess(lab, challenge, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("jsonb");
        assertThat(response.tradeOffAnalysis()).containsIgnoringCase("localidade");
    }

    @Test
    @DisplayName("Deve aprovar solução válida para grafo com Cypher (Neo4j)")
    void shouldApproveValidNeo4jCypherSolution() {
        Lab lab = new Lab("ddia-cap-03-lab-02", 2, "grafos-propriedades", "Grafos de Propriedades", "Sumário",
                List.of("Property Graphs", "Cypher"), EngineType.NEO4J, "neo4j", null, List.of());

        Challenge challenge = new Challenge("lab-02-ch-1", 1, "Grafos Cypher", "Modele conexões com Cypher",
                "Crie nós de Person e arestas de relacionamento",
                "CREATE (c:Location)", List.of(), "Cognição em grafos", EngineType.NEO4J);

        String userQuery = """
                CREATE (c:Location {name: 'Sao Paulo', type: 'Cidade'}),
                       (s:Location {name: 'SP', type: 'Estado'}),
                       (b:Location {name: 'Brasil', type: 'Pais'}),
                       (p:Person {name: 'Gabriel'}),
                       (c)-[:WITHIN]->(s)-[:WITHIN]->(b),
                       (p)-[:BORN_IN]->(c);
                MATCH (p:Person {name: 'Gabriel'})-[:BORN_IN]->(:Location)-[:WITHIN*1..5]->(country:Location {name: 'Brasil'})
                RETURN p.name, country.name;
                """;

        AiAssessmentRequest request = new AiAssessmentRequest(
                lab.id(), challenge.id(), userQuery, "Gabriel, Brasil retornado com sucesso.", "Grafos modelam caminhos variáveis naturalmente sem recursão tabular.", "heuristic", null, null, "pt"
        );

        AiAssessmentResponse response = client.assess(lab, challenge, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).containsIgnoringCase("grafo");
    }

    @Test
    @DisplayName("Deve solicitar revisão (NEEDS_REVISION) quando faltarem elementos essenciais da solução")
    void shouldRequireRevisionWhenMissingEssentialElements() {
        Lab lab = new Lab("ddia-cap-03-lab-01", 1, "relacional-vs-documentos", "Relacional vs Documentos", "Sumário",
                List.of("Normalização 3NF"), EngineType.POSTGRES, "tbl_lab", null, List.of());

        Challenge challenge = new Challenge("lab-01-ch-1", 1, "Modelagem 3NF", "Modele o perfil profissional normalizado em 3NF",
                "Crie tabelas com relacionamentos",
                "CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY);", List.of(), "Reflexão", EngineType.POSTGRES);

        // Apenas enviou SELECT 1 sem modelar nada
        String userQuery = "SELECT 1;";

        AiAssessmentRequest request = new AiAssessmentRequest(
                lab.id(), challenge.id(), userQuery, "1", "", "heuristic", null, null, "pt"
        );

        AiAssessmentResponse response = client.assess(lab, challenge, request, AssessmentLanguage.PT);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).isNotEmpty();
        assertThat(response.alternativeApproaches()).isNotEmpty();
    }
}
