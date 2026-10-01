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
    @DisplayName("GET /api/books com Accept-Language: en - Deve retornar catálogo traduzido em inglês")
    void shouldReturnBooksInEnglishWhenAcceptLanguageHeaderIsEn() throws Exception {
        mockMvc.perform(get("/api/books")
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tagLine", is("The definitive guide to architecting distributed, reliable, and scalable systems.")))
                .andExpect(jsonPath("$[0].chapters[0].title", is("Data Models and Query Languages")));
    }

    @Test
    @DisplayName("GET /api/books?lang=en - Query param deve sobrescrever Accept-Language")
    void shouldSupportLangQueryParamOverride() throws Exception {
        mockMvc.perform(get("/api/books")
                        .header("Accept-Language", "pt")
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tagLine", is("The definitive guide to architecting distributed, reliable, and scalable systems.")));
    }

    @Test
    @DisplayName("GET /api/books/{id} com Accept-Language: en - Deve retornar livro e capítulos traduzidos")
    void shouldReturnBookWithEnglishChaptersWhenRequestedWithAcceptLanguage() throws Exception {
        mockMvc.perform(get("/api/books/ddia")
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagLine", is("The definitive guide to architecting distributed, reliable, and scalable systems.")))
                .andExpect(jsonPath("$.chapters[0].title", is("Data Models and Query Languages")))
                .andExpect(jsonPath("$.chapters[0].subtitle", is("Data Models, Graphs, OLAP and CQRS")));
    }

    @Test
    @DisplayName("GET /api/labs/{id} com Accept-Language: en - Deve retornar laboratório e desafios em inglês")
    void shouldReturnLabInEnglishWhenAcceptLanguageHeaderIsEn() throws Exception {
        mockMvc.perform(get("/api/labs/ddia-cap-03-lab-01")
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Relational vs Document and Storage Locality")))
                .andExpect(jsonPath("$.challenges[0].title", is("3NF Modeling (Strict Relational)")))
                .andExpect(jsonPath("$.challenges[0].guidelines[0]", is("Model coherent primary and foreign keys")));
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
    @DisplayName("POST /api/query/reset/{labId} - Deve retornar HTTP 400 Bad Request para lab inexistente")
    void shouldFailResetForUnknownLab() throws Exception {
        mockMvc.perform(post("/api/query/reset/lab-inexistente"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Regra de negócio violada")))
                .andExpect(jsonPath("$.detail", containsString("não encontrado")));
    }

    @Test
    @DisplayName("POST /api/query/execute - Deve retornar HTTP 400 Bad Request para labId inexistente no catálogo")
    void shouldRejectQueryExecutionWithUnknownLabId() throws Exception {
        Map<String, Object> payload = Map.of(
                "query", "SELECT 1;",
                "engineType", "POSTGRES",
                "labId", "lab-fantasma"
        );

        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Regra de negócio violada")))
                .andExpect(jsonPath("$.detail", containsString("não encontrado")));
    }

    @Test
    @DisplayName("POST /api/query/execute - Deve retornar HTTP 400 Bad Request com diagnóstico para erro de sintaxe SQL")
    void shouldRejectInvalidSqlSyntaxWithBadRequest() throws Exception {
        Map<String, Object> payload = Map.of(
                "query", "SELECT FROM WHERE ;",
                "engineType", "POSTGRES",
                "labId", "ddia-cap-03-lab-01"
        );

        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Erro na execução da consulta")))
                .andExpect(jsonPath("$.detail", notNullValue()));
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
    @DisplayName("GET /api/ai/providers - Deve retornar lista de provedores suportados e seus modelos")
    void shouldReturnAiProvidersList() throws Exception {
        mockMvc.perform(get("/api/ai/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].id", hasItems("gemini", "ollama")))
                .andExpect(jsonPath("$[?(@.id == 'gemini')].requiresApiKey", hasItem(true)))
                .andExpect(jsonPath("$[?(@.id == 'gemini')].models", notNullValue()))
                .andExpect(jsonPath("$[?(@.id == 'ollama')].requiresApiKey", hasItem(false)));
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

    @Test
    @DisplayName("POST /api/ai/test-connection - Deve rejeitar provedor não suportado com HTTP 400 Bad Request")
    void shouldRejectUnsupportedProviderInTestConnectionWithBadRequest() throws Exception {
        Map<String, Object> payload = Map.of(
                "provider", "chatgpt"
        );

        mockMvc.perform(post("/api/ai/test-connection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Regra de negócio violada")))
                .andExpect(jsonPath("$.detail", containsString("chatgpt")));
    }

    @Test
    @DisplayName("POST /api/ai/test-connection - Deve rejeitar provider em branco com HTTP 400")
    void shouldRejectBlankProviderInTestConnection() throws Exception {
        Map<String, Object> payload = Map.of(
                "provider", "   "
        );

        mockMvc.perform(post("/api/ai/test-connection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Erro de validação sintática")))
                .andExpect(jsonPath("$.errors", hasItem(hasEntry("field", "provider"))));
    }

    @Test
    @DisplayName("POST /api/ai/assess - Deve rejeitar labId inexistente com HTTP 400 Bad Request")
    void shouldRejectAiAssessmentWithUnknownLabId() throws Exception {
        Map<String, Object> payload = Map.of(
                "labId", "lab-fantasma",
                "challengeId", "ch-1",
                "userQuery", "SELECT 1;"
        );

        mockMvc.perform(post("/api/ai/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Regra de negócio violada")))
                .andExpect(jsonPath("$.detail", containsString("Laboratório não encontrado")));
    }

    @Test
    @DisplayName("POST /api/ai/assess - Deve rejeitar challengeId inexistente no laboratório com HTTP 400")
    void shouldRejectAiAssessmentWithUnknownChallengeId() throws Exception {
        Map<String, Object> payload = Map.of(
                "labId", "ddia-cap-03-lab-01",
                "challengeId", "desafio-fantasma",
                "userQuery", "SELECT 1;"
        );

        mockMvc.perform(post("/api/ai/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Regra de negócio violada")))
                .andExpect(jsonPath("$.detail", containsString("Desafio não encontrado")));
    }
}

