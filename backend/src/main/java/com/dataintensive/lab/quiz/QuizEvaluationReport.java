package com.dataintensive.lab.quiz;

import com.dataintensive.lab.catalog.dto.QuizQuestionResult;
import com.dataintensive.lab.domain.Difficulty;

import java.util.List;
import java.util.Map;

public record QuizEvaluationReport(
        String chapterId,
        int totalQuestions,
        int correctAnswers,
        double scorePercentage,
        double weightedScore,
        Map<Difficulty, Double> scoreByDifficulty,
        List<String> strengths,
        List<String> areasForImprovement,
        List<String> recommendedTopics,
        List<QuizQuestionResult> results
) {
    public QuizEvaluationReport {
        scoreByDifficulty = scoreByDifficulty != null ? Map.copyOf(scoreByDifficulty) : Map.of();
        strengths = strengths != null ? List.copyOf(strengths) : List.of();
        areasForImprovement = areasForImprovement != null ? List.copyOf(areasForImprovement) : List.of();
        recommendedTopics = recommendedTopics != null ? List.copyOf(recommendedTopics) : List.of();
        results = results != null ? List.copyOf(results) : List.of();
    }
}
