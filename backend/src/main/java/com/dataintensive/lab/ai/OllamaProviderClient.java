package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.AssessmentLanguage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OllamaProviderClient implements AiProviderClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String defaultModel;

    @Autowired
    public OllamaProviderClient(
            ObjectMapper objectMapper,
            @Value("${lab.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${lab.ai.ollama.model:qwen2.5-coder:1.5b}") String defaultModel) {
        this(objectMapper, null, baseUrl, defaultModel);
    }

    public OllamaProviderClient(
            ObjectMapper objectMapper,
            HttpClient httpClient,
            String baseUrl,
            String defaultModel) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : "http://localhost:11434";
        this.defaultModel = (defaultModel != null && !defaultModel.isBlank()) ? defaultModel : "qwen2.5-coder:1.5b";
    }

    @Override
    public String getProviderId() {
        return "ollama";
    }

    @Override
    public AiProviderInfo getInfo(AssessmentLanguage language) {
        boolean isEn = language == AssessmentLanguage.EN;
        return new AiProviderInfo(
                "ollama",
                "Ollama Local",
                isEn
                        ? "Local and private execution via Ollama (no API key required)."
                        : "Execução local e privada via Ollama (sem necessidade de API Key).",
                false,
                "",
                "https://ollama.com/",
                defaultModel,
                List.of(
                        new AiModelInfo("qwen2.5-coder:1.5b", isEn ? "Qwen 2.5 Coder 1.5B (Recommended)" : "Qwen 2.5 Coder 1.5B (Recomendado)", true),
                        new AiModelInfo("deepseek-r1:1.5b", "DeepSeek R1 1.5B", false),
                        new AiModelInfo("llama3.2:1b", "Llama 3.2 1B", false)
                )
        );
    }

    @Override
    public AiProviderInfo getInfo() {
        return getInfo(AssessmentLanguage.PT);
    }

    @Override
    public AiTestConnectionResponse testConnection(AiTestConnectionRequest request) {
        boolean isModelOverridden = request.modelOverride() != null && !request.modelOverride().isBlank();
        String targetModel = isModelOverridden ? request.modelOverride().trim() : defaultModel;

        long startTime = System.currentTimeMillis();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - startTime;
            if (resp.statusCode() == 200) {
                return new AiTestConnectionResponse(true, "Ollama conectado com sucesso!", targetModel, latency);
            } else {
                return new AiTestConnectionResponse(false, "Ollama respondeu com HTTP " + resp.statusCode(), null, latency);
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return new AiTestConnectionResponse(false, "Não foi possível conectar ao Ollama em " + baseUrl + ": " + e.getMessage(), null, latency);
        }
    }

    @Override
    public AiAssessmentResponse assess(String prompt, String apiKey, String modelOverride, boolean isModelOverridden) throws Exception {
        String effectiveModel = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride.trim() : defaultModel;
        String endpoint = baseUrl + "/api/generate";

        Map<String, Object> payload = Map.of(
                "model", effectiveModel,
                "prompt", prompt,
                "stream", false,
                "format", "json"
        );

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            String responseText = root.path("response").asText();
            return AiResponseParser.parse(objectMapper, responseText, "Ollama Local (" + effectiveModel + ")");
        }
        throw new RuntimeException("Falha na chamada Ollama HTTP " + response.statusCode());
    }
}
