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
                .andExpect(jsonPath("$[0].title", containsString("Data-Intensive")));
    }

    @Test
    @DisplayName("GET /api/books/{id} - Deve retornar livro existente")
    void shouldReturnBookById() throws Exception {
        mockMvc.perform(get("/api/books/ddia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("ddia")))
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
    @DisplayName("POST /api/query/execute - Deve rejeitar consulta vazia")
    void shouldRejectEmptyQueryExecution() throws Exception {
        Map<String, Object> payload = Map.of(
                "query", "   ",
                "engineType", "POSTGRES",
                "labId", "ddia-cap-03-lab-01"
        );

        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorMessage", containsString("consulta fornecida está vazia")));
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
    @DisplayName("POST /api/ai/assess - Deve exigir revisão quando código do aluno for vazio")
    void shouldRequireRevisionForEmptyUserSubmission() throws Exception {
        Map<String, Object> payload = Map.of(
                "labId", "ddia-cap-03-lab-01",
                "challengeId", "lab-01-ch-1",
                "userQuery", "",
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
}
