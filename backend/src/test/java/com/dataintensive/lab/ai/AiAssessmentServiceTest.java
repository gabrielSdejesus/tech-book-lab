package com.dataintensive.lab.ai;

import com.dataintensive.lab.catalog.CatalogRepository;
import com.dataintensive.lab.catalog.CatalogService;
import com.dataintensive.lab.domain.Challenge;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiAssessmentServiceTest {

    private AiAssessmentService aiAssessmentService;
    private CatalogService catalogService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        CatalogRepository catalogRepository = mock(CatalogRepository.class);
        Challenge ch1 = new Challenge(
                "lab-01-ch-1",
                1,
                "Modelagem 3NF (Relacional Estrito)",
                "Crie o modelo 3NF",
                "Cada usuário tem nome, bio",
                "-- 1. Crie as tabelas normalizadas\nCREATE TABLE usuarios (\n    id INT PRIMARY KEY,\n    nome VARCHAR(255),\n    bio VARCHAR(500)\n);",
                List.of(),
                "Reflexão"
        );
        Lab lab1 = new Lab(
                "ddia-cap-03-lab-01",
                1,
                "relacional-vs-documentos",
                "Relacional vs Documentos",
                "Comparativo",
                List.of("Impedance Mismatch"),
                EngineType.POSTGRES,
                "tbl_lab",
                "DROP TABLE...",
                List.of(ch1)
        );
        when(catalogRepository.findLabById("ddia-cap-03-lab-01")).thenReturn(Optional.of(lab1));

        catalogService = new CatalogService(catalogRepository);
        objectMapper = new ObjectMapper();
        aiAssessmentService = new AiAssessmentService(
                catalogService,
                objectMapper,
                "gemini",
                "",
                "gemini-3.8-flash",
                "http://localhost:11434",
                "qwen2.5-coder:1.5b"
        );
    }

    @Test
    @DisplayName("Deve rejeitar teste de conexão quando chave da API estiver vazia")
    void shouldRejectTestConnectionWhenApiKeyIsEmpty() {
        AiTestConnectionRequest request = new AiTestConnectionRequest("gemini", "", null);

        AiTestConnectionResponse response = aiAssessmentService.testConnection(request);

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).containsIgnoringCase("nenhuma api key informada");
    }

    @Test
    @DisplayName("Deve rejeitar provedor desconhecido no teste de conexão")
    void shouldRejectUnknownProviderInTestConnection() {
        AiTestConnectionRequest request = new AiTestConnectionRequest("provedor_invalido", "xyz", null);

        AiTestConnectionResponse response = aiAssessmentService.testConnection(request);

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).containsIgnoringCase("provedor desconhecido");
    }

    @Test
    @DisplayName("Deve retornar NEEDS_REVISION quando o código do usuário estiver vazio")
    void shouldReturnNeedsRevisionWhenQueryIsEmpty() {
        AiAssessmentRequest request = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                "lab-01-ch-1",
                "   ",
                null,
                "Reflexão sobre normalização",
                "test-api-key",
                "gemini",
                "gemini-3.8-flash"
        );

        AiAssessmentResponse response = aiAssessmentService.assess(request);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("nenhuma implementação detectada");
    }

    @Test
    @DisplayName("Deve retornar NEEDS_REVISION quando o código do usuário for idêntico ao template inicial")
    void shouldReturnNeedsRevisionWhenQueryIsIdenticalToStarterTemplate() {
        var lab = catalogService.findLabById("ddia-cap-03-lab-01").orElseThrow();
        var ch = lab.challenges().get(0);

        AiAssessmentRequest request = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                ch.id(),
                ch.starterTemplate(),
                null,
                "Minha reflexão",
                "test-api-key",
                "gemini",
                "gemini-3.8-flash"
        );

        AiAssessmentResponse response = aiAssessmentService.assess(request);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("template inicial inalterado");
    }

    @Test
    @DisplayName("Deve solicitar chave de API quando provedor for Gemini e nenhuma chave for informada")
    void shouldRequestApiKeyWhenMissingForGemini() {
        AiAssessmentRequest request = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                "lab-01-ch-1",
                "SELECT * FROM usuarios WHERE id = 1;",
                null,
                "Minha reflexão",
                "",
                "gemini",
                "gemini-3.8-flash"
        );

        AiAssessmentResponse response = aiAssessmentService.assess(request);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("chave de api não informada");
    }

    @Test
    @DisplayName("Deve falhar no teste de conexão e não substituir por modelo descoberto quando modelOverride for 404")
    void shouldNotReplaceRequestedModelWithDiscoveredModelWhen404Occurs() throws Exception {
        java.net.http.HttpClient mockClient = mock(java.net.http.HttpClient.class);
        @SuppressWarnings("unchecked")
        java.net.http.HttpResponse<String> mockResp = (java.net.http.HttpResponse<String>) mock(java.net.http.HttpResponse.class);
        when(mockResp.statusCode()).thenReturn(404);
        when(mockResp.body()).thenReturn("{\"error\": {\"message\": \"Model gemini-2.5-flash not found\"}}");
        when(mockClient.<String>send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(mockResp);

        AiAssessmentService service = new AiAssessmentService(
                catalogService,
                objectMapper,
                mockClient,
                "gemini",
                "valid-key",
                "gemini-3.8-flash",
                "http://localhost:11434",
                "qwen2.5-coder:1.5b"
        );

        AiTestConnectionRequest request = new AiTestConnectionRequest("gemini", "valid-key", "gemini-2.5-flash");
        AiTestConnectionResponse response = service.testConnection(request);

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).containsIgnoringCase("não encontrado");
    }

    @Test
    @DisplayName("Deve respeitar modelOverride para Ollama no teste de conexão")
    void shouldRespectModelOverrideForOllamaInTestConnection() throws Exception {
        java.net.http.HttpClient mockClient = mock(java.net.http.HttpClient.class);
        @SuppressWarnings("unchecked")
        java.net.http.HttpResponse<String> mockResp = (java.net.http.HttpResponse<String>) mock(java.net.http.HttpResponse.class);
        when(mockResp.statusCode()).thenReturn(200);
        when(mockResp.body()).thenReturn("{\"models\": [{\"name\": \"qwen2.5-coder:7b\"}]}");
        when(mockClient.<String>send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(mockResp);

        AiAssessmentService service = new AiAssessmentService(
                catalogService,
                objectMapper,
                mockClient,
                "gemini",
                "valid-key",
                "gemini-3.8-flash",
                "http://localhost:11434",
                "qwen2.5-coder:1.5b"
        );

        AiTestConnectionRequest request = new AiTestConnectionRequest("ollama", null, "qwen2.5-coder:7b");
        AiTestConnectionResponse response = service.testConnection(request);

        assertThat(response.valid()).isTrue();
        assertThat(response.model()).isEqualTo("qwen2.5-coder:7b");
    }
}

