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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
    @DisplayName("Deve lançar DomainValidationException para provedor não suportado no teste de conexão")
    void shouldRejectUnknownProviderInTestConnection() {
        AiTestConnectionRequest request = new AiTestConnectionRequest("provedor_invalido", "xyz", null);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.dataintensive.lab.domain.DomainValidationException.class,
                () -> aiAssessmentService.testConnection(request)
        );
    }

    @Test
    @DisplayName("Deve lançar DomainValidationException quando laboratório for inexistente na avaliação")
    void shouldThrowDomainValidationExceptionWhenLabDoesNotExist() {
        AiAssessmentRequest request = new AiAssessmentRequest(
                "lab-fantasma",
                "ch-1",
                "SELECT 1;",
                null,
                "Minha reflexão",
                "test-api-key",
                "gemini",
                "gemini-3.8-flash"
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                com.dataintensive.lab.domain.DomainValidationException.class,
                () -> aiAssessmentService.assess(request)
        );
    }

    @Test
    @DisplayName("Deve lançar DomainValidationException quando desafio não pertencer ao laboratório")
    void shouldThrowDomainValidationExceptionWhenChallengeDoesNotExist() {
        AiAssessmentRequest request = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                "desafio-fantasma",
                "SELECT 1;",
                null,
                "Minha reflexão",
                "test-api-key",
                "gemini",
                "gemini-3.8-flash"
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                com.dataintensive.lab.domain.DomainValidationException.class,
                () -> aiAssessmentService.assess(request)
        );
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

    @Test
    @DisplayName("Deve retornar mensagem de validação em inglês quando language for 'en' e submissão for vazia")
    void shouldReturnEnglishValidationWhenSubmissionIsEmptyAndLanguageIsEn() {
        AiAssessmentRequest request = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                "lab-01-ch-1",
                "",
                null,
                null,
                null,
                "gemini",
                null,
                "en"
        );

        AiAssessmentResponse response = aiAssessmentService.assess(request);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("No implementation detected");
        assertThat(response.tradeOffAnalysis()).containsIgnoringCase("trade-offs");
    }

    @Test
    @DisplayName("Deve retornar mensagem de validação em português por padrão ou quando language for 'pt'")
    void shouldReturnPortugueseValidationWhenSubmissionIsEmptyAndLanguageIsPt() {
        AiAssessmentRequest request = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                "lab-01-ch-1",
                "",
                null,
                null,
                null,
                "gemini",
                null,
                "pt"
        );

        AiAssessmentResponse response = aiAssessmentService.assess(request);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).containsIgnoringCase("Nenhuma implementação detectada");
    }

    @Test
    @DisplayName("Deve delegar ao cliente de IA com sucesso e retornar AiAssessmentResponse aprovada")
    void shouldDelegateToAiClientSuccessfully() throws Exception {
        AiProviderClient mockClient = mock(AiProviderClient.class);
        AiProviderRegistry mockRegistry = mock(AiProviderRegistry.class);

        when(mockRegistry.getClient("gemini")).thenReturn(mockClient);
        when(mockClient.isConfigured(any())).thenReturn(true);
        when(mockClient.assess(any(), any(), any(), anyBoolean())).thenReturn(new AiAssessmentResponse(
                "APPROVED",
                "Solução exemplar!",
                "Excelente entendimento de trade-offs.",
                "Execução eficiente.",
                List.of("Considere particionamento"),
                "Mock AI"
        ));

        AiAssessmentService service = new AiAssessmentService(catalogService, objectMapper, mockRegistry, "gemini");

        AiAssessmentRequest request = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                "lab-01-ch-1",
                "SELECT id, nome, bio FROM usuarios WHERE id = 10;",
                null,
                "Reflexão sobre localidade",
                "valid-key",
                "gemini",
                "gemini-3.8-flash",
                "pt"
        );

        AiAssessmentResponse response = service.assess(request);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).isEqualTo("Solução exemplar!");
        assertThat(response.tradeOffAnalysis()).isEqualTo("Excelente entendimento de trade-offs.");
        assertThat(response.modelUsed()).isEqualTo("Mock AI");
    }

    @Test
    @DisplayName("Deve capturar exceção do cliente de IA e retornar fallback pedagógico em português")
    void shouldReturnPedagogicalFallbackInPortugueseWhenClientThrowsException() throws Exception {
        AiProviderClient mockClient = mock(AiProviderClient.class);
        AiProviderRegistry mockRegistry = mock(AiProviderRegistry.class);

        when(mockRegistry.getClient("gemini")).thenReturn(mockClient);
        when(mockClient.isConfigured(any())).thenReturn(true);
        when(mockClient.assess(any(), any(), any(), anyBoolean()))
                .thenThrow(new java.io.IOException("Conexão interrompida"));

        AiAssessmentService service = new AiAssessmentService(catalogService, objectMapper, mockRegistry, "gemini");

        AiAssessmentRequest request = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                "lab-01-ch-1",
                "SELECT id, nome, bio FROM usuarios WHERE id = 10;",
                null,
                "Reflexão sobre localidade",
                "valid-key",
                "gemini",
                "gemini-3.8-flash",
                "pt"
        );

        AiAssessmentResponse response = service.assess(request);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).contains("Falha na comunicação com o Tutor de IA (gemini)");
        assertThat(response.feedback()).contains("Conexão interrompida");
        assertThat(response.tradeOffAnalysis()).contains("Não foi possível obter a análise de trade-offs");
        assertThat(response.modelUsed()).contains("Erro de API");
    }

    @Test
    @DisplayName("Deve capturar exceção do cliente de IA e retornar fallback pedagógico em inglês")
    void shouldReturnPedagogicalFallbackInEnglishWhenClientThrowsException() throws Exception {
        AiProviderClient mockClient = mock(AiProviderClient.class);
        AiProviderRegistry mockRegistry = mock(AiProviderRegistry.class);

        when(mockRegistry.getClient("gemini")).thenReturn(mockClient);
        when(mockClient.isConfigured(any())).thenReturn(true);
        when(mockClient.assess(any(), any(), any(), anyBoolean()))
                .thenThrow(new java.io.IOException("Connection timeout"));

        AiAssessmentService service = new AiAssessmentService(catalogService, objectMapper, mockRegistry, "gemini");

        AiAssessmentRequest request = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                "lab-01-ch-1",
                "SELECT id, nome, bio FROM usuarios WHERE id = 10;",
                null,
                "Reflection about storage locality",
                "valid-key",
                "gemini",
                "gemini-3.8-flash",
                "en"
        );

        AiAssessmentResponse response = service.assess(request);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.feedback()).contains("Communication failure with AI Tutor (gemini)");
        assertThat(response.feedback()).contains("Connection timeout");
        assertThat(response.tradeOffAnalysis()).contains("Could not obtain trade-off analysis");
        assertThat(response.modelUsed()).isEqualTo("AI Error");
    }

    @Test
    @DisplayName("Não deve fazer fallback silencioso para o heurístico quando nenhuma chave for informada para o Gemini")
    void shouldNotFallbackToHeuristicWhenNoApiKeyProvided() {
        AiAssessmentRequest request = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                "lab-01-ch-1",
                """
                CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY, nome VARCHAR(100));
                CREATE TABLE IF NOT EXISTS experiencias_profissionais (id INT PRIMARY KEY, usuario_id INT REFERENCES usuarios(id));
                SELECT u.nome FROM usuarios u JOIN experiencias_profissionais e ON e.usuario_id = u.id;
                """,
                "1 linha",
                "Normalização 3NF",
                null, // sem API key
                null, // sem override de provedor (usa default gemini)
                null,
                "pt"
        );

        AiAssessmentResponse response = aiAssessmentService.assess(request);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.modelUsed()).isEqualTo("Autenticação Pendente");
        assertThat(response.feedback()).containsIgnoringCase("chave de api não informada");
    }

    @Test
    @DisplayName("Deve rejeitar chave placeholder 'string' e exigir autenticação real")
    void shouldRejectPlaceholderStringKey() {
        AiAssessmentRequest request = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                "lab-01-ch-1",
                "SELECT * FROM usuarios WHERE id = 1;",
                "1 linha",
                "Normalização 3NF",
                "string", // placeholder
                "gemini",
                "gemini-3.8-flash",
                "pt"
        );

        AiAssessmentResponse response = aiAssessmentService.assess(request);

        assertThat(response.status()).isEqualTo("NEEDS_REVISION");
        assertThat(response.modelUsed()).isEqualTo("Autenticação Pendente");
        assertThat(response.feedback()).containsIgnoringCase("chave de api não informada");
    }

    @Test
    @DisplayName("Deve utilizar o provedor heurístico diretamente quando explicitamente requisitado com zero autenticação")
    void shouldUseHeuristicProviderDirectlyWhenRequested() {
        AiAssessmentRequest request = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                "lab-01-ch-1",
                """
                CREATE TABLE IF NOT EXISTS usuarios (id INT PRIMARY KEY, nome VARCHAR(100));
                CREATE TABLE IF NOT EXISTS experiencias_profissionais (id INT PRIMARY KEY, usuario_id INT REFERENCES usuarios(id));
                INSERT INTO usuarios VALUES (1, 'Ana'), (2, 'Beto');
                SELECT u.nome FROM usuarios u LEFT JOIN experiencias_profissionais e ON e.usuario_id = u.id;
                """,
                "1 linha",
                "Normalização 3NF",
                null,
                "heuristic", // explicitamente requisitado
                null,
                "pt"
        );

        AiAssessmentResponse response = aiAssessmentService.assess(request);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.modelUsed()).containsIgnoringCase("Heurístico");
    }

    @Test
    @DisplayName("Não deve listar o provedor heurístico entre os provedores de Tutor IA configuráveis")
    void shouldNotListHeuristicInAvailableTutors() {
        List<AiProviderInfo> providers = aiAssessmentService.getAvailableProviders();

        assertThat(providers).extracting(AiProviderInfo::id).doesNotContain("heuristic");
        assertThat(providers).extracting(AiProviderInfo::id).contains("gemini", "ollama");
    }

    @Test
    @DisplayName("Heurístico: Deve retornar expectedReflection quando avaliado via provedor heurístico")
    void shouldReturnExpectedReflectionWhenUsingHeuristicProvider() {
        Challenge ch = new Challenge(
                "lab-01-ch-1",
                1,
                "Modelagem 3NF",
                "Desc",
                "Cenario",
                "template",
                null,
                List.of(),
                "Prompt",
                "Gabarito de trade-off 3NF",
                EngineType.POSTGRES
        );
        Lab lab = new Lab("ddia-cap-03-lab-01", 1, "slug", "Title", "Summary", List.of(), EngineType.POSTGRES, "db", null, List.of(ch));
        CatalogRepository catalogRepo = mock(CatalogRepository.class);
        when(catalogRepo.findLabById("ddia-cap-03-lab-01")).thenReturn(Optional.of(lab));

        CatalogService catService = new CatalogService(catalogRepo);
        AiAssessmentService service = new AiAssessmentService(catService, objectMapper, "gemini", "", "model", "http://ollama", "qwen");

        AiAssessmentRequest req = new AiAssessmentRequest(
                "ddia-cap-03-lab-01",
                "lab-01-ch-1",
                "CREATE TABLE usuarios (id INT PRIMARY KEY, nome VARCHAR(100)); CREATE TABLE experiencias_profissionais (id INT PRIMARY KEY, usuario_id INT REFERENCES usuarios(id), cargo VARCHAR(100)); INSERT INTO usuarios VALUES (1, 'Ana'), (2, 'Beto'); SELECT u.nome FROM usuarios u LEFT JOIN experiencias_profissionais e ON e.usuario_id = u.id;",
                "ok",
                "minha reflexao",
                "",
                "heuristic",
                "",
                "pt"
        );
        AiAssessmentResponse res = service.assess(req);

        assertThat(res.expectedReflection()).isEqualTo("Gabarito de trade-off 3NF");
    }

    @Test
    @DisplayName("Tutor IA: Não deve retornar expectedReflection estático na avaliação via IA generativa")
    void shouldNotReturnStaticExpectedReflectionWhenUsingGenerativeAi() {
        Challenge ch = new Challenge(
                "lab-01-ch-1",
                1,
                "Modelagem 3NF",
                "Desc",
                "Cenario",
                "template",
                null,
                List.of(),
                "Prompt",
                "Gabarito de trade-off 3NF",
                EngineType.POSTGRES
        );
        Lab lab = new Lab("ddia-cap-03-lab-01", 1, "slug", "Title", "Summary", List.of(), EngineType.POSTGRES, "db", null, List.of(ch));
        CatalogRepository catalogRepo = mock(CatalogRepository.class);
        when(catalogRepo.findLabById("ddia-cap-03-lab-01")).thenReturn(Optional.of(lab));

        CatalogService catService = new CatalogService(catalogRepo);
        AiAssessmentService service = new AiAssessmentService(catService, objectMapper, "gemini", "", "gemini-3.8-flash", "http://ollama", "qwen");

        AiAssessmentRequest req = new AiAssessmentRequest("ddia-cap-03-lab-01", "lab-01-ch-1", "CREATE TABLE usuarios (id INT);", "ok", "minha reflexao", "", "gemini", "", "pt");
        AiAssessmentResponse res = service.assess(req);

        assertThat(res.expectedReflection()).isNull();
    }
}



