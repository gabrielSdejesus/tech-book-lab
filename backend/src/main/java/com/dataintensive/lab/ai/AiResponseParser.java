package com.dataintensive.lab.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class AiResponseParser {

    private AiResponseParser() {}

    public static AiAssessmentResponse parse(ObjectMapper objectMapper, String json, String modelName) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String status = node.path("status").asText("DISCUSSION");
            String feedback = node.path("feedback").asText();
            String tradeOff = node.path("tradeOffAnalysis").asText();
            String efficiency = node.path("efficiencyNotes").asText();

            List<String> alternatives = new ArrayList<>();
            JsonNode altNode = node.path("alternativeApproaches");
            if (altNode.isArray()) {
                for (JsonNode a : altNode) {
                    alternatives.add(a.asText());
                }
            }

            return new AiAssessmentResponse(status, feedback, tradeOff, efficiency, alternatives, modelName);
        } catch (Exception e) {
            return new AiAssessmentResponse("DISCUSSION", json, "Análise gerada pelo modelo.", "Sem notas de eficiência.", List.of(), modelName);
        }
    }

    public static String extractErrorMessage(ObjectMapper objectMapper, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "Resposta vazia do servidor";
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errorNode = root.path("error");
            if (!errorNode.isMissingNode()) {
                String message = errorNode.path("message").asText();
                if (!message.isBlank()) return message;
            }
        } catch (Exception ignored) {}
        return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
    }
}
