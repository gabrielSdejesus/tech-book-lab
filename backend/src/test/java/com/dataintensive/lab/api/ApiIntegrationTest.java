package com.dataintensive.lab.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/books - Deve retornar catálogo de livros com status 200")
    void shouldReturnBooksCatalog() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].id", is("ddia")))
                .andExpect(jsonPath("$[0].coverImageUrl", is("/covers/ddia.svg")))
                .andExpect(jsonPath("$[0].title", containsString("Data-Intensive")));
    }

    @Test
    @DisplayName("GET /api/books/{id} - Deve retornar livro existente")
    void shouldReturnBookById() throws Exception {
        mockMvc.perform(get("/api/books/ddia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("ddia")))
                .andExpect(jsonPath("$.coverImageUrl", is("/covers/ddia.svg")))
                .andExpect(jsonPath("$.author", is("Martin Kleppmann")));
    }

    @Test
    @DisplayName("GET /api/books/{id} - Deve retornar o Capítulo 3 com o título 'Modelos de Dados e Linguagens de Consulta'")
    void shouldReturnBookWithCorrectChapter3Title() throws Exception {
        mockMvc.perform(get("/api/books/ddia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].number", is(3)))
                .andExpect(jsonPath("$.chapters[0].title", is("Modelos de Dados e Linguagens de Consulta")));
    }

    @Test
    @DisplayName("GET /api/books/{id} - Deve retornar 404 para livro inexistente")
    void shouldReturn404ForUnknownBook() throws Exception {
        mockMvc.perform(get("/api/books/livro-desconhecido"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/labs/{id} - Deve retornar laboratório com status 200")
    void shouldReturnLabById() throws Exception {
        mockMvc.perform(get("/api/labs/ddia-cap-03-lab-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("ddia-cap-03-lab-01")))
                .andExpect(jsonPath("$.engineType", is("POSTGRES")));
    }

    @Test
    @DisplayName("GET /api/labs/{id} - Deve retornar 404 para laboratório inexistente")
    void shouldReturn404ForUnknownLab() throws Exception {
        mockMvc.perform(get("/api/labs/lab-inexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/infra/status - Deve retornar status de conectividade da infraestrutura")
    void shouldReturnInfraStatus() throws Exception {
        mockMvc.perform(get("/api/infra/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postgresReady", notNullValue()))
                .andExpect(jsonPath("$.neo4jReady", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/query/execute - Deve rejeitar consulta vazia com HTTP 400 Bad Request e RFC 7807")
    void shouldRejectEmptyQueryExecution() throws Exception {
        Map<String, Object> payload = Map.of(
                "query", "   ",
                "engineType", "POSTGRES",
                "labId", "ddia-cap-03-lab-01"
        );

        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Erro de validação sintática")))
                .andExpect(jsonPath("$.errors", hasItem(hasEntry("field", "query"))));
    }

    @Test
    @DisplayName("POST /api/query/execute - Deve rejeitar consulta excessivamente longa (> 10.000 caracteres)")
    void shouldRejectExcessivelyLongQuery() throws Exception {
        String hugeQuery = "SELECT " + "a".repeat(10005);
        Map<String, Object> payload = Map.of(
                "query", hugeQuery,
                "engineType", "POSTGRES",
                "labId", "ddia-cap-03-lab-01"
        );

        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Erro de validação sintática")))
                .andExpect(jsonPath("$.errors", hasItem(hasEntry("field", "query"))));
    }

    @Test
    @DisplayName("POST /api/query/execute - Deve rejeitar payload sem engineType obrigatório")
    void shouldRejectMissingEngineType() throws Exception {
        Map<String, Object> payload = Map.of(
                "query", "SELECT 1;",
                "labId", "ddia-cap-03-lab-01"
        );

        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Erro de validação sintática")))
                .andExpect(jsonPath("$.errors", hasItem(hasEntry("field", "engineType"))));
    }

    @Test
    @DisplayName("POST /api/query/execute - Deve rejeitar payload sem labId obrigatório")
    void shouldRejectMissingLabId() throws Exception {
        Map<String, Object> payload = Map.of(
                "query", "SELECT 1;",
                "engineType", "POSTGRES"
        );

        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Erro de validação sintática")))
                .andExpect(jsonPath("$.errors", hasItem(hasEntry("field", "labId"))));
    }

    @Test
    @DisplayName("POST /api/query/execute - Deve retornar HTTP 400 amigável para JSON malformado")
    void shouldRejectMalformedJson() throws Exception {
        String malformedJson = "{ \"query\": \"SELECT 1\", ";

        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Requisição JSON inválida")))
                .andExpect(jsonPath("$.detail", containsString("formato JSON inválido")));
    }

    @Test
    @DisplayName("POST /api/ai/assess - Deve rejeitar requisição sem campos obrigatórios")
    void shouldRejectEmptyAiAssessmentRequest() throws Exception {
        mockMvc.perform(post("/api/ai/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Erro de validação sintática")))
                .andExpect(jsonPath("$.errors", hasItem(hasEntry("field", "labId"))))
                .andExpect(jsonPath("$.errors", hasItem(hasEntry("field", "challengeId"))))
                .andExpect(jsonPath("$.errors", hasItem(hasEntry("field", "userQuery"))));
    }

    @Test
    @DisplayName("POST /api/query/reset/{labId} - Deve retornar erro para lab inexistente")
    void shouldFailResetForUnknownLab() throws Exception {
        mockMvc.perform(post("/api/query/reset/lab-inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorMessage", containsString("não encontrado")));
    }

    @Test
    @DisplayName("POST /api/ai/test-connection - Deve falhar quando API key não for informada")
    void shouldFailAiTestConnectionWithoutKey() throws Exception {
        Map<String, Object> payload = Map.of(
                "provider", "gemini",
                "apiKey", ""
        );

        mockMvc.perform(post("/api/ai/test-connection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.message", containsString("Nenhuma API Key informada")));
    }

    @Test
    @DisplayName("POST /api/ai/assess - Deve exigir revisão quando código do aluno contiver apenas comentários")
    void shouldRequireRevisionForEmptyUserSubmission() throws Exception {
        Map<String, Object> payload = Map.of(
                "labId", "ddia-cap-03-lab-01",
                "challengeId", "lab-01-ch-1",
                "userQuery", "-- apenas comentarios sem instrucoes SQL",
                "userReflection", "Reflexão sobre localidade de dados",
                "providerOverride", "gemini",
                "apiKeyOverride", "teste-key",
                "modelOverride", "gemini-3.8-flash"
        );

        mockMvc.perform(post("/api/ai/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NEEDS_REVISION")))
                .andExpect(jsonPath("$.feedback", containsString("Nenhuma implementação detectada")));
    }

    @Test
    @DisplayName("POST /api/ai/assess - Deve retornar 400 Bad Request ao submeter idioma não suportado")
    void shouldRejectUnsupportedLanguageWithBadRequest() throws Exception {
        Map<String, Object> payload = Map.of(
                "labId", "ddia-cap-03-lab-01",
                "challengeId", "lab-01-ch-1",
                "userQuery", "SELECT * FROM usuarios;",
                "language", "es"
        );

        mockMvc.perform(post("/api/ai/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("es")));
    }
}

