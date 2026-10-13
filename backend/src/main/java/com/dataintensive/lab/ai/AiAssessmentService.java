package com.dataintensive.lab.ai;

import com.dataintensive.lab.catalog.CatalogService;
import com.dataintensive.lab.domain.AssessmentLanguage;
import com.dataintensive.lab.domain.Challenge;
import com.dataintensive.lab.domain.DomainValidationException;
import com.dataintensive.lab.domain.Lab;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

@Service
public class AiAssessmentService {

    private final CatalogService catalogService;
    private final ObjectMapper objectMapper;
    private final AiProviderRegistry aiProviderRegistry;
    private final com.dataintensive.lab.simulation.ArchitectureStressSimulator architectureStressSimulator;
    private final String defaultProvider;

    @Autowired
    public AiAssessmentService(
            CatalogService catalogService,
            ObjectMapper objectMapper,
            AiProviderRegistry aiProviderRegistry,
            com.dataintensive.lab.simulation.ArchitectureStressSimulator architectureStressSimulator,
            @Value("${lab.ai.provider:gemini}") String defaultProvider) {
        this.catalogService = catalogService;
        this.objectMapper = objectMapper;
        this.aiProviderRegistry = aiProviderRegistry;
        this.architectureStressSimulator = architectureStressSimulator != null
                ? architectureStressSimulator
                : new com.dataintensive.lab.simulation.ArchitectureStressSimulator(objectMapper);
        this.defaultProvider = defaultProvider;
    }

    public AiAssessmentService(
            CatalogService catalogService,
            ObjectMapper objectMapper,
            AiProviderRegistry aiProviderRegistry,
            String defaultProvider) {
        this(catalogService, objectMapper, aiProviderRegistry, new com.dataintensive.lab.simulation.ArchitectureStressSimulator(objectMapper), defaultProvider);
    }

    public AiAssessmentService(
            CatalogService catalogService,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            String defaultProvider,
            String geminiApiKey,
            String geminiModel,
            String ollamaBaseUrl,
            String ollamaModel) {
        this(
                catalogService,
                objectMapper,
                new AiProviderRegistry(List.of(
                        new GeminiProviderClient(objectMapper, httpClient, geminiApiKey, geminiModel),
                        new OllamaProviderClient(objectMapper, httpClient, ollamaBaseUrl, ollamaModel),
                        new HeuristicProviderClient()
                )),
                new com.dataintensive.lab.simulation.ArchitectureStressSimulator(objectMapper),
                defaultProvider
        );
    }

    public AiAssessmentService(
            CatalogService catalogService,
            ObjectMapper objectMapper,
            String defaultProvider,
            String geminiApiKey,
            String geminiModel,
            String ollamaBaseUrl,
            String ollamaModel) {
        this(catalogService, objectMapper, null, defaultProvider, geminiApiKey, geminiModel, ollamaBaseUrl, ollamaModel);
    }

    public List<AiProviderInfo> getAvailableProviders() {
        return aiProviderRegistry.getConfigurableTutors();
    }

    public List<AiProviderInfo> getAvailableProviders(String language) {
        return aiProviderRegistry.getConfigurableTutors(language);
    }

    public AiTestConnectionResponse testConnection(AiTestConnectionRequest request) {
        String provider = (request.provider() != null && !request.provider().isBlank())
                ? request.provider().trim().toLowerCase()
                : defaultProvider;

        AiProviderClient client = aiProviderRegistry.getClient(provider);
        return client.testConnection(request);
    }

    public AiAssessmentResponse assess(AiAssessmentRequest request) {
        AssessmentLanguage lang = AssessmentLanguage.from(request.language());

        Optional<Lab> labOpt = catalogService.findLabById(request.labId());
        if (labOpt.isEmpty()) {
            throw new DomainValidationException("Laboratório não encontrado com id: " + request.labId());
        }
        Lab lab = labOpt.get();

        Challenge challenge = lab.challenges().stream()
                .filter(c -> c.id().equalsIgnoreCase(request.challengeId()))
                .findFirst()
                .orElseThrow(() -> new DomainValidationException(
                        "Desafio não encontrado com id: " + request.challengeId() + " no laboratório: " + request.labId()
                ));

        // Validação: Bloqueia submissão de código vazio ou template inicial inalterado
        if (isUntouchedOrEmpty(request.userQuery(), challenge)) {
            if (lang == AssessmentLanguage.EN) {
                return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    "No implementation detected. The query editor is empty or contains only the untouched starter template. Write your solution to the challenge before requesting the Tutor's evaluation.",
                    "To analyze conceptual trade-offs (such as storage locality, normalization vs denormalization, or query execution plans), it is necessary to implement and run the query.",
                    "No real query was submitted.",
                    List.of(
                        "Read the 'Practical Scenario' in the sidebar and the challenge objectives.",
                        "Write the corresponding SQL or Cypher commands in the editor and run them with 'Ctrl + Enter' before evaluating."
                    ),
                    "Submission Validator"
                );
            }
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

        AiProviderClient client = aiProviderRegistry.getClient(provider);

        if (!client.isConfigured(request.apiKeyOverride())) {
            if (lang == AssessmentLanguage.EN) {
                return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    "API Key not provided for " + client.getInfo().name() + ". Click 'AI Tutor Settings' in the top right to insert your free Google AI Studio API Key, or switch to local Ollama.",
                    "Real-time evaluation requires a valid API key to analyze your solution.",
                    "Could not contact " + client.getInfo().name() + " model.",
                    List.of("Get your free API key at " + client.getInfo().helpUrl() + " and save it in the settings modal."),
                    "Pending Authentication"
                );
            }
            return new AiAssessmentResponse(
                "NEEDS_REVISION",
                "Chave de API não informada para o " + client.getInfo().name() + ". Clique no botão 'Tutor IA Config' no canto superior direito para inserir sua API Key gratuita do Google AI Studio, ou alterne para o Ollama local.",
                "A avaliação com IA real em tempo real necessita de uma chave de API válida para analisar sua solução.",
                "Não foi possível contactar o modelo " + client.getInfo().name() + ".",
                List.of("Obtenha sua chave gratuita em " + client.getInfo().helpUrl() + " e salve no modal de configurações."),
                "Autenticação Pendente"
            );
        }

        if (client instanceof HeuristicProviderClient heuristicClient) {
            return heuristicClient.assess(lab, challenge, request, lang);
        }

        String prompt = buildPrompt(lab, challenge, request, lang);
        boolean isModelOverridden = request.modelOverride() != null && !request.modelOverride().isBlank();

        try {
            return client.assess(prompt, request.apiKeyOverride(), request.modelOverride(), isModelOverridden);
        } catch (Exception e) {
            System.err.println("Erro ao chamar serviço de IA (" + provider + "): " + e.getMessage());
            if (lang == AssessmentLanguage.EN) {
                return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    "Communication failure with AI Tutor (" + provider + "): " + e.getMessage() + ". Check if your API Key is valid in 'AI Tutor Settings'.",
                    "Could not obtain trade-off analysis due to an error in the AI provider.",
                    "Connection error with the model.",
                    List.of("Validate your key and model using 'Test Connection' inside 'AI Tutor Settings'."),
                    "AI Error"
                );
            }
            return new AiAssessmentResponse(
                "NEEDS_REVISION",
                "Falha na comunicação com o Tutor de IA (" + provider + "): " + e.getMessage() + ". Verifique se sua API Key é válida no modal 'Tutor IA Config'.",
                "Não foi possível obter a análise de trade-offs devido a erro no provedor de IA.",
                "Erro de conexão com o modelo.",
                List.of("Valide a chave e o modelo no botão 'Testar Conexão' dentro de 'Tutor IA Config'."),
                "Erro de API (" + provider + ")"
            );
        }
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

    private String buildPrompt(Lab lab, Challenge challenge, AiAssessmentRequest request, AssessmentLanguage lang) {
        if (lang == AssessmentLanguage.EN) {
            return """
                You are an Expert Data Engineering and Distributed Systems Tutor evaluating a practical exercise based on the book "Designing Data-Intensive Applications" (Martin Kleppmann).

                EVALUATION GUIDELINES:
                1. DO NOT require a single fixed template or syntax. There are multiple valid ways to solve the same problem (different JOIN types, CTEs, subqueries, JSONB operators, graph traversals).
                2. HOWEVER, BE RIGOROUS: Check whether the student's code ACTUALLY solves the challenge proposed in the scenario. If the code is incomplete, is merely the starter template, or contains obvious logic errors, return status "NEEDS_REVISION" and clearly state what is missing.
                3. Only approve with "APPROVED" if the query is functional and meets the laboratory requirements.

                LABORATORY CONTENT:
                - Topic: %s
                - Key Concepts: %s
                - Challenge: %s
                - Business Scenario: %s
                - Reflective Trade-off Question: %s

                STUDENT SUBMISSION:
                - Query / Executed Code:
                %s

                - Execution Result on Real Database:
                %s

                - Student's Conceptual Reflection:
                %s

                Respond EXCLUSIVELY in JSON format with the following keys:
                {
                  "status": "APPROVED" | "NEEDS_REVISION" | "DISCUSSION",
                  "feedback": "Constructive and encouraging pedagogical analysis explaining whether the solution met requirements or what is missing.",
                  "tradeOffAnalysis": "Deep commentary relating the student's answer to Martin Kleppmann's concepts (e.g., storage locality, read vs write cost, transitive closure, etc).",
                  "efficiencyNotes": "Performance observations (index usage, query execution plan, computational complexity).",
                  "alternativeApproaches": ["Viable alternative 1", "Viable alternative 2"]
                }
                """.formatted(
                    lab != null ? lab.title() : "Data Laboratory",
                    lab != null ? String.join(", ", lab.keyConcepts()) : "Data-Intensive Systems",
                    challenge != null ? challenge.title() + ": " + challenge.description() : "General Challenge",
                    challenge != null ? challenge.scenario() : "Test Scenario",
                    challenge != null ? challenge.reflectionPrompt() : "Conceptual Trade-offs",
                    request.userQuery() != null ? request.userQuery() : "(No code)",
                    request.executionSummary() != null ? request.executionSummary() : "(No result)",
                    request.userReflection() != null ? request.userReflection() : "(No reflection)"
            );
        }

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

    public AiAssessmentResponse assessScenario(String scenarioId, ScenarioAssessmentRequest request) {
        AssessmentLanguage lang = AssessmentLanguage.from(request.language());
        boolean isEn = lang == AssessmentLanguage.EN;
        String locale = lang.name().toLowerCase();

        com.dataintensive.lab.domain.InteractiveScenario scenario = catalogService.getScenarioById(scenarioId, locale)
                .orElseThrow(() -> new DomainValidationException("Cenário não encontrado com id: " + scenarioId));

        String userState = (request.userStateJson() != null && !request.userStateJson().isBlank())
                ? request.userStateJson()
                : (scenario.savedUserStateJson() != null && !scenario.savedUserStateJson().isBlank()
                ? scenario.savedUserStateJson()
                : scenario.initialStateJson());

        com.dataintensive.lab.simulation.ArchitectureModel model = architectureStressSimulator.parseModel(userState);
        com.dataintensive.lab.simulation.StressWorkload workload = architectureStressSimulator.parseWorkload(scenario.constraintsJson(), null);
        com.dataintensive.lab.simulation.StressSimulationResult simulationResult = architectureStressSimulator.simulate(model, workload);

        String provider = (request.providerOverride() != null && !request.providerOverride().isBlank())
                ? request.providerOverride().trim().toLowerCase()
                : defaultProvider;

        AiProviderClient client = aiProviderRegistry.getClient(provider);

        if (client instanceof HeuristicProviderClient) {
            return evaluateScenarioHeuristically(scenario, request, simulationResult, lang);
        }

        String prompt = buildScenarioPrompt(scenario, model, workload, simulationResult, request.userReflection(), lang);
        boolean isModelOverridden = request.modelOverride() != null && !request.modelOverride().isBlank();
        try {
            return client.assess(prompt, request.apiKeyOverride(), request.modelOverride(), isModelOverridden);
        } catch (Exception e) {
            System.err.println("Erro ao chamar serviço de IA para cenário (" + provider + "): " + e.getMessage());
            String errorMsg = isEn
                    ? "Communication failure with AI Tutor (" + provider + "): " + e.getMessage()
                    : "Falha na comunicação com o Tutor de IA (" + provider + "): " + e.getMessage();
            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    errorMsg,
                    "Falha ao contatar modelo de IA.",
                    "Erro de comunicação.",
                    List.of(),
                    "Erro de API (" + provider + ")"
            );
        }
    }

    private AiAssessmentResponse evaluateScenarioHeuristically(
            com.dataintensive.lab.domain.InteractiveScenario scenario,
            ScenarioAssessmentRequest request,
            com.dataintensive.lab.simulation.StressSimulationResult simResult,
            AssessmentLanguage lang) {
        boolean isEn = lang == AssessmentLanguage.EN;

        String userReflection = request.userReflection() != null ? request.userReflection().trim() : "";
        boolean hasDetailedReflection = userReflection.length() >= 25;

        if (!simResult.slaMet()) {
            String feedback = isEn
                    ? "Your architecture proposal violated performance or SLA constraints. " + String.join(" ", simResult.violations())
                    : "Sua proposta de arquitetura violou restrições de desempenho ou SLA contratado. " + String.join(" ", simResult.violations());
            String tradeOff = isEn
                    ? "Trade-off analysis: The current component topology generates excessive latency (p99: " + simResult.estimatedLatencyP99Ms() + "ms) or high lock contention (" + (int)(simResult.lockContentionIndex() * 100) + "%)."
                    : "Análise de trade-off: A topologia atual gera sobrecarga de latência (p99: " + simResult.estimatedLatencyP99Ms() + "ms) ou alta contenção de concorrência (" + (int)(simResult.lockContentionIndex() * 100) + "%).";
            String efficiency = isEn
                    ? "Estimated p50: " + simResult.estimatedLatencyP50Ms() + "ms | Throughput: " + simResult.estimatedThroughputOps() + " ops/s."
                    : "Latência p50 estimada: " + simResult.estimatedLatencyP50Ms() + "ms | Vazão: " + simResult.estimatedThroughputOps() + " ops/s.";

            return new AiAssessmentResponse(
                    "NEEDS_REVISION",
                    feedback,
                    tradeOff,
                    efficiency,
                    simResult.recommendations(),
                    "rules-engine-v1",
                    scenario.expectedReflection()
            );
        }

        if (hasDetailedReflection) {
            String feedback = isEn
                    ? "Architecture approved! Your design safely meets the SLA requirements with sound engineering judgment."
                    : "Arquitetura aprovada! Seu design atende com folga aos requisitos de SLA com sólido embasamento de engenharia.";
            String tradeOff = isEn
                    ? "Trade-off analysis: The distribution between storage, indexing, and partitioning mitigates hotspots and guarantees bounded latency."
                    : "Análise de trade-off: A distribuição entre armazenamento, indexação e particionamento mitiga hotspots e garante latência delimitada.";
            String efficiency = isEn
                    ? "Estimated p99: " + simResult.estimatedLatencyP99Ms() + "ms (SLA met) | Throughput: " + simResult.estimatedThroughputOps() + " ops/s."
                    : "Latência p99 estimada: " + simResult.estimatedLatencyP99Ms() + "ms (SLA atendido) | Vazão: " + simResult.estimatedThroughputOps() + " ops/s.";

            return new AiAssessmentResponse(
                    "APPROVED",
                    feedback,
                    tradeOff,
                    efficiency,
                    simResult.recommendations(),
                    "rules-engine-v1",
                    scenario.expectedReflection()
            );
        }

        String feedback = isEn
                ? "SLA is technically met, but your trade-off reflection is too brief. How does this design handle future traffic spikes?"
                : "O SLA foi atendido tecnicamente, mas sua justificativa de trade-offs foi muito sucinta. Como este design se comportará sob picos imprevistos de tráfego?";
        String tradeOff = isEn
                ? "Consider explaining your decision on normalization vs denormalization according to DDIA Chapter 3."
                : "Aprofunde sua reflexão detalhando os impactos de normalização vs desnormalização conforme o Capítulo 3 do DDIA.";

        return new AiAssessmentResponse(
                "DISCUSSION",
                feedback,
                tradeOff,
                "p50: " + simResult.estimatedLatencyP50Ms() + "ms | p99: " + simResult.estimatedLatencyP99Ms() + "ms",
                simResult.recommendations(),
                "rules-engine-v1",
                scenario.expectedReflection()
        );
    }

    private String buildScenarioPrompt(
            com.dataintensive.lab.domain.InteractiveScenario scenario,
            com.dataintensive.lab.simulation.ArchitectureModel model,
            com.dataintensive.lab.simulation.StressWorkload workload,
            com.dataintensive.lab.simulation.StressSimulationResult simResult,
            String userReflection,
            AssessmentLanguage lang) {
        return """
                Você é um Tutor Socrático especializado em Engenharia de Sistemas Intensivos de Dados (baseado no livro Designing Data-Intensive Applications de Martin Kleppmann).
                Analise criticamente o modelo de arquitetura proposto pelo aluno para o seguinte cenário prático:
                
                CENÁRIO: %s
                BRIEFING: %s
                MODELO DE DADOS ESCOLHIDO: %s
                CHAVE DE PARTICIONAMENTO: %s
                RÉPLICAS: %d
                ESTRATÉGIA DE CACHE: %s
                COMPONENTES: %s
                
                RESULTADO DO TESTE DE ESTRESSE:
                - Latência Estimada p50: %.1f ms
                - Latência Estimada p99: %.1f ms (SLA Alvo: %.1f ms)
                - Throughput Estimado: %.1f ops/s
                - Índice de Contenção de Locks: %.2f
                - SLA Atendido: %s
                - Violações Detectadas: %s
                
                JUSTIFICATIVA / REFLEXÃO DO ALUNO:
                %s
                
                GABARITO ESPERADO DE TRADE-OFF:
                %s
                
                Responda EXCLUSIVAMENTE em formato JSON com as seguintes chaves:
                {
                  "status": "APPROVED" | "NEEDS_REVISION" | "DISCUSSION",
                  "feedback": "Análise pedagógica construtiva e socrática sobre o diagrama e decisões de engenharia.",
                  "tradeOffAnalysis": "Comentário profundo relacionando a resposta do aluno com os conceitos de Martin Kleppmann (DDIA).",
                  "efficiencyNotes": "Observações sobre métricas de SLA, contenção de locks e throughput sob estresse.",
                  "alternativeApproaches": ["Abordagem alternativa 1", "Abordagem alternativa 2"]
                }
                """.formatted(
                scenario.title(),
                scenario.contextBriefing(),
                model.dataModel(),
                model.partitioningKey(),
                model.replicationFactor(),
                model.cachingStrategy(),
                model.nodes().stream().map(n -> n.type().name() + " (" + n.label() + ")").toList(),
                simResult.estimatedLatencyP50Ms(),
                simResult.estimatedLatencyP99Ms(),
                workload.targetSlaP99Ms(),
                simResult.estimatedThroughputOps(),
                simResult.lockContentionIndex(),
                simResult.slaMet() ? "SIM" : "NÃO",
                String.join("; ", simResult.violations()),
                userReflection != null ? userReflection : "",
                scenario.expectedReflection() != null ? scenario.expectedReflection() : ""
        );
    }
}
