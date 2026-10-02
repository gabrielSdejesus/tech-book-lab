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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GeminiProviderClient implements AiProviderClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String defaultApiKey;
    private final String defaultModel;

    @Autowired
    public GeminiProviderClient(
            ObjectMapper objectMapper,
            @Value("${lab.ai.gemini.api-key:}") String defaultApiKey,
            @Value("${lab.ai.gemini.model:gemini-3.8-flash}") String defaultModel) {
        this(objectMapper, null, defaultApiKey, defaultModel);
    }

    public GeminiProviderClient(
            ObjectMapper objectMapper,
            HttpClient httpClient,
            String defaultApiKey,
            String defaultModel) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.defaultApiKey = defaultApiKey;
        this.defaultModel = (defaultModel != null && !defaultModel.isBlank()) ? defaultModel : "gemini-3.8-flash";
    }

    @Override
    public String getProviderId() {
        return "gemini";
    }

    @Override
    public AiProviderInfo getInfo(AssessmentLanguage language) {
        boolean isEn = language == AssessmentLanguage.EN;
        return new AiProviderInfo(
                "gemini",
                "Google Gemini",
                isEn
                        ? "Language models from Google AI Studio (requires free API key)."
                        : "Modelos de linguagem do Google AI Studio (requer chave gratuita).",
                true,
                "AIzaSy...",
                "https://aistudio.google.com/",
                defaultModel,
                List.of(
                        new AiModelInfo("gemini-3.8-flash", isEn ? "Gemini 3.8 Flash (Recommended)" : "Gemini 3.8 Flash (Recomendado)", true),
                        new AiModelInfo("gemini-3.5-flash", "Gemini 3.5 Flash", false),
                        new AiModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash", false)
                )
        );
    }

    @Override
    public AiProviderInfo getInfo() {
        return getInfo(AssessmentLanguage.PT);
    }

    @Override
    public boolean isConfigured(String apiKeyOverride) {
        String key = (apiKeyOverride != null && !apiKeyOverride.isBlank()) ? apiKeyOverride.trim() : defaultApiKey;
        return key != null && !key.isBlank();
    }

    @Override
    public AiTestConnectionResponse testConnection(AiTestConnectionRequest request) {
        String key = (request.apiKey() != null && !request.apiKey().isBlank())
                ? request.apiKey().trim()
                : defaultApiKey;

        if (key == null || key.isBlank()) {
            return new AiTestConnectionResponse(false, "Nenhuma API Key informada para o Google Gemini.", null, 0);
        }

        boolean isModelOverridden = request.modelOverride() != null && !request.modelOverride().isBlank();
        String targetModel = isModelOverridden ? request.modelOverride().trim() : defaultModel;

        long startTime = System.currentTimeMillis();
        try {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + targetModel + ":generateContent?key=" + key;
            Map<String, Object> payload = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", "ping")))),
                    "generationConfig", Map.of("maxOutputTokens", 5)
            );
            String body = objectMapper.writeValueAsString(payload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - startTime;

            if (resp.statusCode() == 200) {
                return new AiTestConnectionResponse(true, "Autenticação bem-sucedida com o modelo " + targetModel + "!", targetModel, latency);
            } else if (resp.statusCode() == 404) {
                if (isModelOverridden) {
                    String errorMsg = AiResponseParser.extractErrorMessage(objectMapper, resp.body());
                    return new AiTestConnectionResponse(false, "Modelo solicitado (" + targetModel + ") não encontrado (404): " + errorMsg, null, latency);
                }
                String discoveredModel = discoverLatestGeminiModel(key);
                if (discoveredModel != null) {
                    return new AiTestConnectionResponse(true, "Autenticação bem-sucedida! Modelo mais recente selecionado: " + discoveredModel, discoveredModel, latency);
                }
                String errorMsg = AiResponseParser.extractErrorMessage(objectMapper, resp.body());
                return new AiTestConnectionResponse(false, "Modelo " + targetModel + " não encontrado (404): " + errorMsg, null, latency);
            } else {
                String errorMsg = AiResponseParser.extractErrorMessage(objectMapper, resp.body());
                return new AiTestConnectionResponse(false, "Falha de autenticação (HTTP " + resp.statusCode() + "): " + errorMsg, null, latency);
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return new AiTestConnectionResponse(false, "Erro de rede ao conectar com Google Gemini: " + e.getMessage(), null, latency);
        }
    }

    @Override
    public AiAssessmentResponse assess(String prompt, String apiKey, String modelOverride, boolean isModelOverridden) throws Exception {
        String key = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : defaultApiKey;
        String effectiveModel = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride.trim() : defaultModel;

        return callGemini(prompt, key, effectiveModel, isModelOverridden);
    }

    private AiAssessmentResponse callGemini(String prompt, String apiKey, String modelName, boolean isModelOverridden) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.2,
                "responseMimeType", "application/json"
        );
        Map<String, Object> payload = Map.of(
                "contents", List.of(content),
                "generationConfig", generationConfig
        );

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidate = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!candidate.isMissingNode()) {
                String aiText = candidate.asText();
                return AiResponseParser.parse(objectMapper, aiText, "Google Gemini (" + modelName + ")");
            }
        }

        if (response.statusCode() == 404) {
            if (isModelOverridden) {
                String err = AiResponseParser.extractErrorMessage(objectMapper, response.body());
                throw new RuntimeException("Modelo especificado '" + modelName + "' não foi encontrado no Google Gemini (404): " + err);
            }
            String autoModel = discoverLatestGeminiModel(apiKey);
            if (autoModel != null && !autoModel.equalsIgnoreCase(modelName)) {
                return callGemini(prompt, apiKey, autoModel, false);
            }
        }

        String err = AiResponseParser.extractErrorMessage(objectMapper, response.body());
        throw new RuntimeException("HTTP " + response.statusCode() + ": " + err);
    }

    public String discoverLatestGeminiModel(String apiKey) {
        try {
            String listUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(listUrl))
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(resp.body());
                JsonNode models = root.path("models");
                if (models.isArray()) {
                    List<String> modelNames = new ArrayList<>();
                    for (JsonNode m : models) {
                        String name = m.path("name").asText();
                        if (name.startsWith("models/")) name = name.substring("models/".length());
                        JsonNode methods = m.path("supportedGenerationMethods");
                        boolean canGenerate = false;
                        if (methods.isArray()) {
                            for (JsonNode method : methods) {
                                if ("generateContent".equalsIgnoreCase(method.asText())) {
                                    canGenerate = true;
                                    break;
                                }
                            }
                        }
                        if (canGenerate) {
                            modelNames.add(name);
                        }
                    }
                    Optional<String> gemini38 = modelNames.stream().filter(m -> m.contains("3.8-flash")).findFirst();
                    if (gemini38.isPresent()) return gemini38.get();

                    Optional<String> gemini35 = modelNames.stream().filter(m -> m.contains("3.5-flash")).findFirst();
                    if (gemini35.isPresent()) return gemini35.get();

                    Optional<String> anyFlash = modelNames.stream().filter(m -> m.contains("flash")).findFirst();
                    if (anyFlash.isPresent()) return anyFlash.get();

                    if (!modelNames.isEmpty()) return modelNames.get(0);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
