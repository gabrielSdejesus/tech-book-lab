package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.AssessmentLanguage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class OllamaProviderClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deve retornar metadados corretos do provedor Ollama")
    void shouldReturnMetadataCorrectly() {
        OllamaProviderClient client = new OllamaProviderClient(objectMapper, null, "http://localhost:11434", "qwen2.5-coder:1.5b");

        assertThat(client.getProviderId()).isEqualTo("ollama");
        assertThat(client.isConfigured(null)).isTrue();

        AiProviderInfo ptInfo = client.getInfo(AssessmentLanguage.PT);
        assertThat(ptInfo.name()).isEqualTo("Ollama Local");
        assertThat(ptInfo.requiresApiKey()).isFalse();

        AiProviderInfo enInfo = client.getInfo(AssessmentLanguage.EN);
        assertThat(enInfo.description()).contains("Local and private execution");
    }

    @Test
    @DisplayName("testConnection deve retornar sucesso quando /api/tags responder HTTP 200")
    void shouldSucceedTestConnectionOnHttp200() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);

        when(mockResp.statusCode()).thenReturn(200);
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResp);

        OllamaProviderClient client = new OllamaProviderClient(objectMapper, mockHttp, "http://localhost:11434", "qwen2.5-coder:1.5b");
        AiTestConnectionResponse response = client.testConnection(new AiTestConnectionRequest("ollama", null, null));

        assertThat(response.valid()).isTrue();
        assertThat(response.message()).contains("Ollama conectado com sucesso");
        assertThat(response.model()).isEqualTo("qwen2.5-coder:1.5b");
    }

    @Test
    @DisplayName("testConnection deve retornar falha quando servidor responder status diferente de 200")
    void shouldFailTestConnectionOnHttp500() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);

        when(mockResp.statusCode()).thenReturn(500);
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResp);

        OllamaProviderClient client = new OllamaProviderClient(objectMapper, mockHttp, "http://localhost:11434", "qwen2.5-coder:1.5b");
        AiTestConnectionResponse response = client.testConnection(new AiTestConnectionRequest("ollama", null, null));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).contains("Ollama respondeu com HTTP 500");
    }

    @Test
    @DisplayName("testConnection deve tratar exceção de conexão recusada")
    void shouldHandleConnectionRefused() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new ConnectException("Connection refused"));

        OllamaProviderClient client = new OllamaProviderClient(objectMapper, mockHttp, "http://localhost:11434", "qwen2.5-coder:1.5b");
        AiTestConnectionResponse response = client.testConnection(new AiTestConnectionRequest("ollama", null, null));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).contains("Não foi possível conectar ao Ollama");
    }

    @Test
    @DisplayName("assess deve retornar AiAssessmentResponse em caso de sucesso HTTP 200")
    void shouldAssessSuccessfully() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);

        String ollamaJson = """
                {
                    "response": "{\\"status\\":\\"APPROVED\\",\\"feedback\\":\\"Consulta correta!\\"}"
                }
                """;

        when(mockResp.statusCode()).thenReturn(200);
        when(mockResp.body()).thenReturn(ollamaJson);
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResp);

        OllamaProviderClient client = new OllamaProviderClient(objectMapper, mockHttp, "http://localhost:11434", "qwen2.5-coder:1.5b");
        AiAssessmentResponse response = client.assess("prompt", null, null, false);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).isEqualTo("Consulta correta!");
        assertThat(response.modelUsed()).contains("Ollama Local");
    }

    @Test
    @DisplayName("assess deve lançar RuntimeException quando Ollama responder com erro HTTP")
    void shouldThrowExceptionOnAssessFailure() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);

        when(mockResp.statusCode()).thenReturn(502);
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResp);

        OllamaProviderClient client = new OllamaProviderClient(objectMapper, mockHttp, "http://localhost:11434", "qwen2.5-coder:1.5b");

        assertThatThrownBy(() -> client.assess("prompt", null, null, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha na chamada Ollama HTTP 502");
    }
}
