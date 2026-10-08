package com.dataintensive.lab.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deve fazer parse de JSON estruturado com todos os campos")
    void shouldParseStructuredJson() {
        String json = """
                {
                    "status": "APPROVED",
                    "feedback": "Excelente query!",
                    "tradeOffAnalysis": "A normalização em 3NF reduz anomalias de escrita.",
                    "efficiencyNotes": "Acesso direto por índice chave primária.",
                    "alternativeApproaches": [
                        "Uso de coluna JSONB para dados semiestruturados",
                        "Desnormalização parcial com views materializadas"
                    ]
                }
                """;

        AiAssessmentResponse response = AiResponseParser.parse(objectMapper, json, "Gemini 3.8 Flash");

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.feedback()).isEqualTo("Excelente query!");
        assertThat(response.tradeOffAnalysis()).isEqualTo("A normalização em 3NF reduz anomalias de escrita.");
        assertThat(response.efficiencyNotes()).isEqualTo("Acesso direto por índice chave primária.");
        assertThat(response.alternativeApproaches()).containsExactly(
                "Uso de coluna JSONB para dados semiestruturados",
                "Desnormalização parcial com views materializadas"
        );
        assertThat(response.modelUsed()).isEqualTo("Gemini 3.8 Flash");
    }

    @Test
    @DisplayName("Deve usar status padrão DISCUSSION quando status não for informado no JSON")
    void shouldFallbackStatusToDiscussionWhenMissing() {
        String json = """
                {
                    "feedback": "Feedback sem status",
                    "tradeOffAnalysis": "Trade-offs analisados"
                }
                """;

        AiAssessmentResponse response = AiResponseParser.parse(objectMapper, json, "Ollama");

        assertThat(response.status()).isEqualTo("DISCUSSION");
        assertThat(response.feedback()).isEqualTo("Feedback sem status");
        assertThat(response.alternativeApproaches()).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar fallback com status DISCUSSION quando string não for JSON válido")
    void shouldReturnFallbackOnInvalidJson() {
        String rawText = "Isto é uma análise em texto puro do modelo sem formatação JSON.";

        AiAssessmentResponse response = AiResponseParser.parse(objectMapper, rawText, "Heuristic");

        assertThat(response.status()).isEqualTo("DISCUSSION");
        assertThat(response.feedback()).isEqualTo(rawText);
        assertThat(response.tradeOffAnalysis()).contains("Análise gerada pelo modelo");
        assertThat(response.efficiencyNotes()).contains("Sem notas de eficiência");
        assertThat(response.alternativeApproaches()).isEmpty();
        assertThat(response.modelUsed()).isEqualTo("Heuristic");
    }

    @Test
    @DisplayName("extractErrorMessage deve extrair mensagem do nó error.message do Google Gemini")
    void shouldExtractErrorMessageFromJson() {
        String errorJson = """
                {
                    "error": {
                        "code": 400,
                        "message": "API key not valid. Please pass a valid API key.",
                        "status": "INVALID_ARGUMENT"
                    }
                }
                """;

        String message = AiResponseParser.extractErrorMessage(objectMapper, errorJson);
        assertThat(message).isEqualTo("API key not valid. Please pass a valid API key.");
    }

    @Test
    @DisplayName("extractErrorMessage deve retornar fallback para payload vazio ou nulo")
    void shouldHandleEmptyOrNullErrorPayload() {
        assertThat(AiResponseParser.extractErrorMessage(objectMapper, null)).isEqualTo("Resposta vazia do servidor");
        assertThat(AiResponseParser.extractErrorMessage(objectMapper, "   ")).isEqualTo("Resposta vazia do servidor");
    }

    @Test
    @DisplayName("extractErrorMessage deve retornar texto original ou truncado em 200 caracteres quando não houver nó error")
    void shouldTruncateLongNonJsonMessage() {
        String shortRaw = "Erro interno 500";
        assertThat(AiResponseParser.extractErrorMessage(objectMapper, shortRaw)).isEqualTo(shortRaw);

        String longRaw = "A".repeat(300);
        String extracted = AiResponseParser.extractErrorMessage(objectMapper, longRaw);
        assertThat(extracted).hasSize(203); // 200 + "..."
        assertThat(extracted).endsWith("...");
    }
}
