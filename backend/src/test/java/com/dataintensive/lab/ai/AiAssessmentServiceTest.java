package com.dataintensive.lab.ai;

import com.dataintensive.lab.catalog.CatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiAssessmentServiceTest {

    private AiAssessmentService aiAssessmentService;
    private CatalogService catalogService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService();
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
}
