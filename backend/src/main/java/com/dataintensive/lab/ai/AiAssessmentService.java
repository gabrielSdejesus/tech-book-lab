package com.dataintensive.lab.ai;

import com.dataintensive.lab.catalog.CatalogService;
import com.dataintensive.lab.domain.Challenge;
import com.dataintensive.lab.domain.Lab;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class AiAssessmentService {

    private final CatalogService catalogService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final String defaultProvider;
    private final String geminiApiKey;
    private final String geminiModel;
    private final String ollamaBaseUrl;
    private final String ollamaModel;

    public AiAssessmentService(
            CatalogService catalogService,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            String defaultProvider,
            String geminiApiKey,
            String geminiModel,
            String ollamaBaseUrl,
            String ollamaModel) {
        this.catalogService = catalogService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.defaultProvider = defaultProvider;
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = (geminiModel != null && !geminiModel.isBlank()) ? geminiModel : "gemini-3.8-flash";
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ollamaModel = ollamaModel;
    }

    public AiAssessmentService(
            CatalogService catalogService,
            ObjectMapper objectMapper,
            @Value("${lab.ai.provider:gemini}") String defaultProvider,
            @Value("${lab.ai.gemini.api-key:}") String geminiApiKey,
            @Value("${lab.ai.gemini.model:gemini-3.8-flash}") String geminiModel,
            @Value("${lab.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${lab.ai.ollama.model:qwen2.5-coder:1.5b}") String ollamaModel) {
        this(catalogService, objectMapper, null, defaultProvider, geminiApiKey, geminiModel, ollamaBaseUrl, ollamaModel);
    }

    public AiTestConnectionResponse testConnection(AiTestConnectionRequest request) {
        String provider = (request.provider() != null && !request.provider().isBlank())
                ? request.provider().trim().toLowerCase()
                : defaultProvider;

        String key = (request.apiKey() != null && !request.apiKey().isBlank())
                ? request.apiKey().trim()
                : geminiApiKey;

        String targetModel = (request.modelOverride() != null && !request.modelOverride().isBlank())
                ? request.modelOverride().trim()
                : geminiModel;

        long startTime = System.currentTimeMillis();

        if ("gemini".equalsIgnoreCase(provider)) {
            if (key == null || key.isBlank()) {
                return new AiTestConnectionResponse(false, "Nenhuma API Key informada para o Google Gemini.", null, 0);
            }
            try {
                // Tenta com o modelo especificado ou descobre o modelo mais recente disponível
                String modelToUse = targetModel;
                String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + modelToUse + ":generateContent?key=" + key;
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
                    return new AiTestConnectionResponse(true, "Autenticação bem-sucedida com a versão mais recente da AI!", modelToUse, latency);
                } else if (resp.statusCode() == 404) {
                    // Se o modelo especificado não foi encontrado, tenta descobrir automaticamente os modelos suportados
                    String discoveredModel = discoverLatestGeminiModel(key);
                    if (discoveredModel != null) {
                        return new AiTestConnectionResponse(true, "Autenticação bem-sucedida! Modelo mais recente selecionado: " + discoveredModel, discoveredModel, latency);
                    }
                    String errorMsg = extractErrorMessage(resp.body());
                    return new AiTestConnectionResponse(false, "Modelo " + modelToUse + " não encontrado (404): " + errorMsg, null, latency);
                } else {
                    String errorMsg = extractErrorMessage(resp.body());
                    return new AiTestConnectionResponse(false, "Falha de autenticação (HTTP " + resp.statusCode() + "): " + errorMsg, null, latency);
                }
            } catch (Exception e) {
                long latency = System.currentTimeMillis() - startTime;
                return new AiTestConnectionResponse(false, "Erro de rede ao conectar com Google Gemini: " + e.getMessage(), null, latency);
            }
        } else if ("ollama".equalsIgnoreCase(provider)) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ollamaBaseUrl + "/api/tags"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                long latency = System.currentTimeMillis() - startTime;
                if (resp.statusCode() == 200) {
                    return new AiTestConnectionResponse(true, "Ollama conectado com sucesso!", ollamaModel, latency);
                } else {
                    return new AiTestConnectionResponse(false, "Ollama respondeu com HTTP " + resp.statusCode(), null, latency);
                }
            } catch (Exception e) {
                long latency = System.currentTimeMillis() - startTime;
                return new AiTestConnectionResponse(false, "Não foi possível conectar ao Ollama em " + ollamaBaseUrl + ": " + e.getMessage(), null, latency);
            }
        }

        return new AiTestConnectionResponse(false, "Provedor desconhecido: " + provider, null, 0);
    }

    public AiAssessmentResponse assess(AiAssessmentRequest request) {
        Optional<Lab> labOpt = catalogService.findLabById(request.labId());
        Lab lab = labOpt.orElse(null);
        Challenge challenge = null;
        if (lab != null && request.challengeId() != null) {
            challenge = lab.challenges().stream()
                    .filter(c -> c.id().equalsIgnoreCase(request.challengeId()))
                    .findFirst()
                    .orElse(null);
        }

        // Validação: Bloqueia submissão de código vazio ou template inicial inalterado
        if (isUntouchedOrEmpty(request.userQuery(), challenge)) {
            return new AiAssessmentResponse(
                "NEEDS_REVISION",
                "Nenhuma implementação detectada. O editor de consultas está vazio ou contém apenas o template inicial inalterado. Escreva a sua solução para o desafio antes de solicitar a avaliação do Tutor.",
                "Para analisar os trade-offs conceituais (como localidade de armazenamento, normalização vs desnormalização ou planos de execução), é necessário implementar e executar a consulta.",
                "Nenhuma consulta real foi submetida.",
                List.of(
                    "Leia o 'Cenário Prático' na barra lateral e os objetivos do desafio.",
                    "Escreva os comandos SQL ou Cypher correspondentes no editor e execute-os com 'Ctrl + Enter' antes de avaliar."
                ),
                "Validador de Submissão"
            );
        }

        String provider = (request.providerOverride() != null && !request.providerOverride().isBlank())
                ? request.providerOverride().trim().toLowerCase()
                : defaultProvider;

        String key = (request.apiKeyOverride() != null && !request.apiKeyOverride().isBlank())
                ? request.apiKeyOverride().trim()
                : geminiApiKey;

        String targetModel = (request.modelOverride() != null && !request.modelOverride().isBlank())
                ? request.modelOverride().trim()
                : geminiModel;

        if ("gemini".equalsIgnoreCase(provider) && (key == null || key.isBlank())) {
            return new AiAssessmentResponse(
                "NEEDS_REVISION",
                "Chave de API não informada para o Google Gemini. Clique no botão 'Tutor IA Config' no canto superior direito para inserir sua API Key gratuita do Google AI Studio, ou alterne para o Ollama local.",
                "A avaliação com IA real em tempo real necessita de uma chave de API válida para analisar sua solução.",
                "Não foi possível contactar o modelo Gemini.",
                List.of("Obtenha sua chave gratuita em https://aistudio.google.com/ e salve no modal de configurações."),
                "Autenticação Pendente"
            );
        }

        String prompt = buildPrompt(lab, challenge, request);

        try {
            if ("gemini".equalsIgnoreCase(provider)) {
                return callGemini(prompt, key, targetModel);
            } else if ("ollama".equalsIgnoreCase(provider)) {
                return callOllama(prompt);
            }
        } catch (Exception e) {
            System.err.println("Erro ao chamar serviço de IA (" + provider + "): " + e.getMessage());
            return new AiAssessmentResponse(
                "NEEDS_REVISION",
                "Falha na comunicação com o Tutor de IA (" + provider + "): " + e.getMessage() + ". Verifique se sua API Key é válida no modal 'Tutor IA Config'.",
                "Não foi possível obter a análise de trade-offs devido a erro no provedor de IA.",
                "Erro de conexão com o modelo.",
                List.of("Valide a chave e o modelo no botão 'Testar Conexão' dentro de 'Tutor IA Config'."),
                "Erro de API (" + provider + ")"
            );
        }

        return fallbackAssessment(lab, challenge, request);
    }

    private boolean isUntouchedOrEmpty(String query, Challenge challenge) {
        if (query == null || query.isBlank()) return true;

        String cleanUser = cleanCode(query);
        if (cleanUser.isEmpty()) return true;

        if (challenge != null && challenge.starterTemplate() != null) {
            String cleanTemplate = cleanCode(challenge.starterTemplate());
            if (cleanUser.equalsIgnoreCase(cleanTemplate)) {
                return true;
            }
        }

        return false;
    }

    private String cleanCode(String code) {
        if (code == null) return "";
        return code.replaceAll("(?m)^\\s*(--|//).*$", "")
                   .replaceAll("/\\*.*?\\*/", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    private String buildPrompt(Lab lab, Challenge challenge, AiAssessmentRequest request) {
        return """
            Você é um Tutor Especialista em Engenharia de Dados e Sistemas Distribuídos avaliando um exercício prático baseado no livro "Designing Data-Intensive Applications" (Martin Kleppmann).
            
            DIRETRIZ DE AVALIAÇÃO:
            1. NÃO exija um gabarito fixo ou uma sintaxe única. Existem diversas formas válidas de resolver o mesmo problema (diferentes tipos de JOINs, CTEs, subqueries, operadores JSONB, travessias em grafos).
            2. NO ENTANTO, SEJA RIGOROSO: Verifique se o código do aluno REALMENTE resolve o desafio proposto no cenário. Se o código estiver incompleto, for apenas o template inicial, ou contiver erros evidentes de lógica, retorne status "NEEDS_REVISION" e aponte com clareza o que falta.
            3. Apenas aprove com "APPROVED" se a consulta for funcional e atender aos requisitos do laboratório.
            
            CONTEÚDO DO LABORATÓRIO:
            - Tópico: %s
            - Conceitos-chave: %s
            - Desafio: %s
            - Cenário de Negócio: %s
            - Pergunta Reflexiva de Trade-off: %s
            
            SUBMISSÃO DO ALUNO:
            - Consulta / Código Executado:
            %s
            
            - Resultado da Execução no Banco Real:
            %s
            
            - Reflexão Conceitual Escrita pelo Aluno:
            %s
            
            Responda EXCLUSIVAMENTE em formato JSON com as seguintes chaves:
            {
              "status": "APPROVED" | "NEEDS_REVISION" | "DISCUSSION",
              "feedback": "Análise pedagógica construtiva e encorajadora sobre a solução implementada, explicando se atendeu ou o que faltou.",
              "tradeOffAnalysis": "Comentário profundo relacionando a resposta do aluno com os conceitos de Martin Kleppmann (ex: localidade de armazenamento, custo de escrita vs leitura, fechamento transitivo, etc).",
              "efficiencyNotes": "Observações sobre performance da query (uso de índices, plano de execução, complexidade computacional).",
              "alternativeApproaches": ["Outra forma viável de resolver 1", "Outra forma viável de resolver 2"]
            }
            """.formatted(
                lab != null ? lab.title() : "Laboratório de Dados",
                lab != null ? String.join(", ", lab.keyConcepts()) : "Sistemas Intensivos em Dados",
                challenge != null ? challenge.title() + ": " + challenge.description() : "Desafio Geral",
                challenge != null ? challenge.scenario() : "Cenário de teste",
                challenge != null ? challenge.reflectionPrompt() : "Trade-offs conceituais",
                request.userQuery() != null ? request.userQuery() : "(Nenhum código)",
                request.executionSummary() != null ? request.executionSummary() : "(Nenhum retorno)",
                request.userReflection() != null ? request.userReflection() : "(Nenhuma reflexão)"
        );
    }

    private AiAssessmentResponse callGemini(String prompt, String apiKey, String modelName) throws Exception {
        String effectiveModel = modelName != null && !modelName.isBlank() ? modelName : geminiModel;
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + effectiveModel + ":generateContent?key=" + apiKey;

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
                return parseAiJsonResponse(aiText, "Google Gemini (" + effectiveModel + ")");
            }
        }

        // Se deu 404 para o modelo específico, tenta fallback automático descobrindo os modelos disponíveis
        if (response.statusCode() == 404) {
            String autoModel = discoverLatestGeminiModel(apiKey);
            if (autoModel != null && !autoModel.equalsIgnoreCase(effectiveModel)) {
                return callGemini(prompt, apiKey, autoModel);
            }
        }

        String err = extractErrorMessage(response.body());
        throw new RuntimeException("HTTP " + response.statusCode() + ": " + err);
    }

    private String discoverLatestGeminiModel(String apiKey) {
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
                        String name = m.path("name").asText(); // ex: models/gemini-3.8-flash
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
                    // Prioriza versão 3.8 Flash, depois 3.5 Flash, ou qualquer flash recente
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

    private AiAssessmentResponse callOllama(String prompt) throws Exception {
        String endpoint = ollamaBaseUrl + "/api/generate";

        Map<String, Object> payload = Map.of(
            "model", ollamaModel,
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
            return parseAiJsonResponse(responseText, "Ollama Local (" + ollamaModel + ")");
        }
        throw new RuntimeException("Falha na chamada Ollama HTTP " + response.statusCode());
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "Resposta vazia do servidor";
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errorNode = root.path("error");
            if (!errorNode.isMissingNode()) {
                String message = errorNode.path("message").asText();
                if (!message.isBlank()) return message;
            }
        } catch (Exception ignored) {}
        return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
    }

    private AiAssessmentResponse parseAiJsonResponse(String json, String modelName) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String status = node.path("status").asText("DISCUSSION");
            String feedback = node.path("feedback").asText();
            String tradeOff = node.path("tradeOffAnalysis").asText();
            String efficiency = node.path("efficiencyNotes").asText();

            List<String> alternatives = new ArrayList<>();
            JsonNode altNode = node.path("alternativeApproaches");
            if (altNode.isArray()) {
                for (JsonNode a : altNode) {
                    alternatives.add(a.asText());
                }
            }

            return new AiAssessmentResponse(status, feedback, tradeOff, efficiency, alternatives, modelName);
        } catch (Exception e) {
            return new AiAssessmentResponse("DISCUSSION", json, "Análise gerada pelo modelo.", "Sem notas de eficiência.", List.of(), modelName);
        }
    }

    private AiAssessmentResponse fallbackAssessment(Lab lab, Challenge challenge, AiAssessmentRequest request) {
        return new AiAssessmentResponse(
            "NEEDS_REVISION",
            "Configuração necessária: Para obter avaliação do Tutor de IA, informe sua chave gratuita do Google AI Studio clicando em 'Tutor IA Config'.",
            "Os trade-offs teóricos de Martin Kleppmann serão analisados em tempo real assim que a chave do Gemini estiver configurada.",
            "Sem métricas de execução de IA.",
            List.of("Obtenha sua chave gratuita em https://aistudio.google.com/"),
            "Tutor IA não configurado"
        );
    }
}
