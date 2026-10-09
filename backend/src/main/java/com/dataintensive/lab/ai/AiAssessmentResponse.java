package com.dataintensive.lab.ai;

import java.util.List;

public record AiAssessmentResponse(
    String status, // "APPROVED", "NEEDS_REVISION", "DISCUSSION"
    String feedback,
    String tradeOffAnalysis,
    String efficiencyNotes,
    List<String> alternativeApproaches,
    String modelUsed,
    String expectedReflection
) {
    public AiAssessmentResponse(
        String status,
        String feedback,
        String tradeOffAnalysis,
        String efficiencyNotes,
        List<String> alternativeApproaches,
        String modelUsed
    ) {
        this(status, feedback, tradeOffAnalysis, efficiencyNotes, alternativeApproaches, modelUsed, null);
    }
}
