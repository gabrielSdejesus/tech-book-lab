package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.AssessmentLanguage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class GeminiProviderClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deve retornar metadados corretos de provedor e modelos")
    void shouldReturnMetadataCorrectly() {
        GeminiProviderClient client = new GeminiProviderClient(objectMapper, null, "test-key", "gemini-3.8-flash");

        assertThat(client.getProviderId()).isEqualTo("gemini");
        assertThat(client.isConfigured(null)).isTrue();
        assertThat(client.isConfigured("")).isTrue(); // defaultKey exists
        assertThat(client.isConfigured("custom-key")).isTrue();

        GeminiProviderClient unconfigured = new GeminiProviderClient(objectMapper, null, "", "gemini-3.8-flash");
        assertThat(unconfigured.isConfigured("")).isFalse();
        assertThat(unconfigured.isConfigured(null)).isFalse();
        assertThat(unconfigured.isConfigured("valid-key")).isTrue();

        AiProviderInfo ptInfo = client.getInfo(AssessmentLanguage.PT);
        assertThat(ptInfo.name()).isEqualTo("Google Gemini");
        assertThat(ptInfo.description()).contains("chave gratuita");

        AiProviderInfo enInfo = client.getInfo(AssessmentLanguage.EN);
        assertThat(enInfo.description()).contains("requires free API key");
    }

    @Test
    @DisplayName("isConfigured deve rejeitar chaves que sejam placeholders ou inválidas")
    void shouldRejectPlaceholderAndInvalidApiKeys() {
        GeminiProviderClient client = new GeminiProviderClient(objectMapper, null, "", "gemini-3.8-flash");
        assertThat(client.isConfigured("string")).isFalse();
        assertThat(client.isConfigured("AIzaSy...")).isFalse();
        assertThat(client.isConfigured("your_api_key_here")).isFalse();
        assertThat(client.isConfigured("undefined")).isFalse();
        assertThat(client.isConfigured("null")).isFalse();
        assertThat(client.isConfigured("abc")).isFalse();
        assertThat(client.isConfigured("   ")).isFalse();
        assertThat(client.isConfigured(null)).isFalse();
        assertThat(client.isConfigured("valid-api-key-123")).isTrue();
    }

    @Test
    @DisplayName("testConnection deve retornar erro quando nenhuma API Key for informada")
    void shouldFailTestConnectionWhenNoKeyProvided() {
        GeminiProviderClient client = new GeminiProviderClient(objectMapper, null, "", "gemini-3.8-flash");
        AiTestConnectionResponse response = client.testConnection(new AiTestConnectionRequest("gemini", "", null));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).contains("Nenhuma API Key informada");
    }

    @Test
    @DisplayName("testConnection deve retornar sucesso quando API responder HTTP 200")
    void shouldSucceedTestConnectionOnHttp200() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);

        when(mockResp.statusCode()).thenReturn(200);
        when(mockResp.body()).thenReturn("{}");
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResp);

        GeminiProviderClient client = new GeminiProviderClient(objectMapper, mockHttp, "valid-key", "gemini-3.8-flash");
        AiTestConnectionResponse response = client.testConnection(new AiTestConnectionRequest("gemini", "valid-key", null));

        assertThat(response.valid()).isTrue();
        assertThat(response.message()).contains("Autenticação bem-sucedida");
        assertThat(response.model()).isEqualTo("gemini-3.8-flash");
    }

    @Test
    @DisplayName("testConnection deve retornar falha 404 quando modelo com override não for encontrado")
    void shouldFailTestConnectionOn404WithModelOverride() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);

        when(mockResp.statusCode()).thenReturn(404);
        when(mockResp.body()).thenReturn("{\"error\": {\"message\": \"models/custom-model is not found\"}}");
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResp);

        GeminiProviderClient client = new GeminiProviderClient(objectMapper, mockHttp, "valid-key", "gemini-3.8-flash");
        AiTestConnectionResponse response = client.testConnection(new AiTestConnectionRequest("gemini", "valid-key", "custom-model"));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).contains("não encontrado (404)");
    }

    @Test
    @DisplayName("testConnection deve tentar auto-descoberta quando modelo padrão retornar 404")
    void shouldAutoDiscoverModelOnDefault404() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp404 = mock(HttpResponse.class);
        HttpResponse<String> mockRespList = mock(HttpResponse.class);

        when(mockResp404.statusCode()).thenReturn(404);
        when(mockResp404.body()).thenReturn("{}");

        String listModelsJson = """
                {
                    "models": [
                        {
                            "name": "models/gemini-3.8-flash",
                            "supportedGenerationMethods": ["generateContent"]
                        }
                    ]
                }
                """;
        when(mockRespList.statusCode()).thenReturn(200);
        when(mockRespList.body()).thenReturn(listModelsJson);

        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResp404)
                .thenReturn(mockRespList);

        GeminiProviderClient client = new GeminiProviderClient(objectMapper, mockHttp, "valid-key", "gemini-old-model");
        AiTestConnectionResponse response = client.testConnection(new AiTestConnectionRequest("gemini", "valid-key", null));

        assertThat(response.valid()).isTrue();
        assertThat(response.message()).contains("Modelo mais recente selecionado");
        assertThat(response.model()).isEqualTo("gemini-3.8-flash");
    }

    @Test
    @DisplayName("testConnection deve retornar erro de autenticação quando HTTP for 400 ou 403")
    void shouldFailTestConnectionOnAuthError() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);

        when(mockResp.statusCode()).thenReturn(400);
        when(mockResp.body()).thenReturn("{\"error\": {\"message\": \"API key not valid\"}}");
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResp);

        GeminiProviderClient client = new GeminiProviderClient(objectMapper, mockHttp, "bad-key", "gemini-3.8-flash");
        AiTestConnectionResponse response = client.testConnection(new AiTestConnectionRequest("gemini", "bad-key", null));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).contains("Falha de autenticação (HTTP 400)");
        assertThat(response.message()).contains("API key not valid");
    }

    @Test
    @DisplayName("testConnection deve capturar exceção de rede ou timeout")
    void shouldHandleNetworkExceptionOnTestConnection() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connect timed out"));

        GeminiProviderClient client = new GeminiProviderClient(objectMapper, mockHttp, "key", "gemini-3.8-flash");
        AiTestConnectionResponse response = client.testConnection(new AiTestConnectionRequest("gemini", "key", null));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).contains("Erro de rede");
    }

    @Test
    @DisplayName("assess deve retornar AiAssessmentResponse em caso de sucesso HTTP 200")
    void shouldAssessSuccessfully() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);

        String geminiResponseJson = """
                {
                    "candidates": [
                        {
                            "content": {
                                "parts": [
                                    {
                                        "text": "{\\"status\\":\\"APPROVED\\",\\"feedback\\":\\"Ótima solução!\\"}"
                                    }
                                ]
                            }
                        }
                    ]
                }
                """;

        when(mockResp.statusCode()).thenReturn(200);
        when(mockResp.body()).thenReturn(geminiResponseJson);
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResp);

        GeminiProviderClient client = new GeminiProviderClient(objectMapper, mockHttp, "key", "gemini-3.8-flash");
        AiAssessmentResponse response = client.assess("prompt", "key", null, false);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).isEqualTo("Ótima solução!");
        assertThat(response.modelUsed()).contains("Google Gemini");
    }

    @Test
    @DisplayName("assess deve lançar RuntimeException quando modelo 404 for requisitado com override")
    void shouldThrowExceptionOnAssess404WithOverride() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);

        when(mockResp.statusCode()).thenReturn(404);
        when(mockResp.body()).thenReturn("{\"error\": {\"message\": \"not found\"}}");
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResp);

        GeminiProviderClient client = new GeminiProviderClient(objectMapper, mockHttp, "key", "gemini-3.8-flash");

        assertThatThrownBy(() -> client.assess("prompt", "key", "invalid-model", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não foi encontrado no Google Gemini");
    }

    @Test
    @DisplayName("assess deve lançar RuntimeException diante de erro de servidor HTTP 500")
    void shouldThrowExceptionOnAssess500() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);

        when(mockResp.statusCode()).thenReturn(500);
        when(mockResp.body()).thenReturn("Internal Server Error");
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResp);

        GeminiProviderClient client = new GeminiProviderClient(objectMapper, mockHttp, "key", "gemini-3.8-flash");

        assertThatThrownBy(() -> client.assess("prompt", "key", null, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("HTTP 500");
    }
}
