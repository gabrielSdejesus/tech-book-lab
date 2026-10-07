package com.dataintensive.lab.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiAssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/ai/providers deve retornar descrições em português quando Accept-Language for pt ou ausente")
    void shouldReturnProvidersInPortugueseByDefault() throws Exception {
        mockMvc.perform(get("/api/ai/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'gemini')].description").value(hasItem(containsString("Modelos de linguagem do Google AI Studio"))))
                .andExpect(jsonPath("$[?(@.id == 'ollama')].description").value(hasItem(containsString("Execução local e privada via Ollama"))));

        mockMvc.perform(get("/api/ai/providers").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'gemini')].description").value(hasItem(containsString("Modelos de linguagem do Google AI Studio"))))
                .andExpect(jsonPath("$[?(@.id == 'ollama')].description").value(hasItem(containsString("Execução local e privada via Ollama"))));
    }

    @Test
    @DisplayName("GET /api/ai/providers deve retornar descrições em inglês quando Accept-Language for en")
    void shouldReturnProvidersInEnglishWhenAcceptLanguageEn() throws Exception {
        mockMvc.perform(get("/api/ai/providers").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'gemini')].description").value(hasItem(containsString("Language models from Google AI Studio"))))
                .andExpect(jsonPath("$[?(@.id == 'ollama')].description").value(hasItem(containsString("Local and private execution via Ollama"))));
    }

    @Test
    @DisplayName("GET /api/ai/providers não deve listar o provedor heurístico offline")
    void shouldNotExposeHeuristicProviderInApi() throws Exception {
        mockMvc.perform(get("/api/ai/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'heuristic')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == 'gemini')]").exists())
                .andExpect(jsonPath("$[?(@.id == 'ollama')]").exists());
    }

    @Test
    @DisplayName("POST /api/ai/assess com provedor gemini sem chave deve retornar Autenticação Pendente")
    void shouldReturnPendingAuthenticationWhenAssessingWithoutKey() throws Exception {
        String json = """
            {
              "labId": "ddia-cap-03-lab-01",
              "challengeId": "lab-01-ch-1",
              "userQuery": "SELECT id FROM usuarios WHERE id = 1;",
              "userReflection": "Reflexão sobre 3NF",
              "providerOverride": "gemini",
              "apiKeyOverride": ""
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/ai/assess")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_REVISION"))
                .andExpect(jsonPath("$.modelUsed").value("Autenticação Pendente"));
    }

    @Test
    @DisplayName("POST /api/ai/assess com provedor heurístico offline deve validar rigorosamente e retornar feedback pedagógico")
    void shouldAssessViaHeuristicProviderWithRigorousRequirements() throws Exception {
        String json = """
            {
              "labId": "ddia-cap-03-lab-01",
              "challengeId": "lab-01-ch-1",
              "userQuery": "SELECT u.nome FROM usuarios u JOIN experiencias_profissionais e ON e.usuario_id = u.id;",
              "userReflection": "Reflexão sobre 3NF",
              "providerOverride": "heuristic"
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/ai/assess")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_REVISION"))
                .andExpect(jsonPath("$.feedback").value(containsString("LEFT JOIN")))
                .andExpect(jsonPath("$.modelUsed").value(containsString("Heurístico")));
    }
}
