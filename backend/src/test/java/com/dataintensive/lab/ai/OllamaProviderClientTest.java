package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.AssessmentLanguage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaProviderClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OllamaProviderClient client = new OllamaProviderClient(objectMapper, null, "http://localhost:11434", "qwen2.5-coder:1.5b");

    @Test
    @DisplayName("Deve retornar descrição em português por padrão e quando solicitado 'pt'")
    void shouldReturnPortugueseDescriptionByDefaultAndWhenPtRequested() {
        AiProviderInfo defaultInfo = client.getInfo();
        assertThat(defaultInfo.description()).isEqualTo("Execução local e privada via Ollama (sem necessidade de API Key).");
        assertThat(defaultInfo.models().getFirst().name()).contains("Recomendado");

        AiProviderInfo ptInfo = client.getInfo("pt");
        assertThat(ptInfo.description()).isEqualTo("Execução local e privada via Ollama (sem necessidade de API Key).");
    }

    @Test
    @DisplayName("Deve retornar descrição em inglês quando solicitado 'en' ou AssessmentLanguage.EN")
    void shouldReturnEnglishDescriptionWhenEnRequested() {
        AiProviderInfo enStringInfo = client.getInfo("en");
        assertThat(enStringInfo.description()).isEqualTo("Local and private execution via Ollama (no API key required).");
        assertThat(enStringInfo.models().getFirst().name()).contains("Recommended");

        AiProviderInfo enEnumInfo = client.getInfo(AssessmentLanguage.EN);
        assertThat(enEnumInfo.description()).isEqualTo("Local and private execution via Ollama (no API key required).");
    }
}
