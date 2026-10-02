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
}
