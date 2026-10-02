package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.AssessmentLanguage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiProviderClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeminiProviderClient client = new GeminiProviderClient(objectMapper, null, "test-api-key", "gemini-3.8-flash");

    @Test
    @DisplayName("Deve retornar descrição em português por padrão e quando solicitado 'pt'")
    void shouldReturnPortugueseDescriptionByDefaultAndWhenPtRequested() {
        AiProviderInfo defaultInfo = client.getInfo();
        assertThat(defaultInfo.description()).isEqualTo("Modelos de linguagem do Google AI Studio (requer chave gratuita).");
        assertThat(defaultInfo.models().getFirst().name()).contains("Recomendado");

        AiProviderInfo ptInfo = client.getInfo("pt");
        assertThat(ptInfo.description()).isEqualTo("Modelos de linguagem do Google AI Studio (requer chave gratuita).");
    }

    @Test
    @DisplayName("Deve retornar descrição em inglês quando solicitado 'en' ou AssessmentLanguage.EN")
    void shouldReturnEnglishDescriptionWhenEnRequested() {
        AiProviderInfo enStringInfo = client.getInfo("en");
        assertThat(enStringInfo.description()).isEqualTo("Language models from Google AI Studio (requires free API key).");
        assertThat(enStringInfo.models().getFirst().name()).contains("Recommended");

        AiProviderInfo enEnumInfo = client.getInfo(AssessmentLanguage.EN);
        assertThat(enEnumInfo.description()).isEqualTo("Language models from Google AI Studio (requires free API key).");
    }
}
