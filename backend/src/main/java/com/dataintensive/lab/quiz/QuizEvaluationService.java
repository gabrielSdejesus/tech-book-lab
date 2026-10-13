package com.dataintensive.lab.quiz;

import com.dataintensive.lab.catalog.dto.QuizQuestionResult;
import com.dataintensive.lab.catalog.dto.QuizSubmissionRequest;
import com.dataintensive.lab.domain.ChapterQuiz;
import com.dataintensive.lab.domain.Difficulty;
import com.dataintensive.lab.domain.QuizOption;
import com.dataintensive.lab.domain.QuizQuestion;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuizEvaluationService {

    public QuizEvaluationReport evaluate(ChapterQuiz quiz, QuizSubmissionRequest request) {
        if (quiz == null || quiz.questions() == null || quiz.questions().isEmpty()) {
            return new QuizEvaluationReport("", 0, 0, 0.0, 0.0, Map.of(), List.of(), List.of(), List.of(), List.of());
        }

        List<QuizQuestionResult> results = new ArrayList<>();
        Map<Difficulty, Integer> totalByDiff = new EnumMap<>(Difficulty.class);
        Map<Difficulty, Integer> correctByDiff = new EnumMap<>(Difficulty.class);

        double totalWeightedPoints = 0.0;
        double maxWeightedPoints = 0.0;

        List<String> areasForImprovement = new ArrayList<>();
        List<String> strengths = new ArrayList<>();
        List<String> recommendedTopics = new ArrayList<>();

        for (QuizQuestion q : quiz.questions()) {
            Difficulty diff = q.difficulty() != null ? q.difficulty() : Difficulty.MEDIUM;
            double weight = switch (diff) {
                case EASY -> 1.0;
                case MEDIUM -> 2.0;
                case HARD -> 3.0;
            };

            totalByDiff.put(diff, totalByDiff.getOrDefault(diff, 0) + 1);
            maxWeightedPoints += weight;

            QuizOption correctOpt = q.options().stream()
                    .filter(QuizOption::isCorrect)
                    .findFirst()
                    .orElse(null);

            String selectedOptionId = request.answers() != null ? request.answers().get(q.id()) : null;
            boolean isCorrect = selectedOptionId != null && correctOpt != null && selectedOptionId.equals(correctOpt.id());

            if (isCorrect) {
                correctByDiff.put(diff, correctByDiff.getOrDefault(diff, 0) + 1);
                totalWeightedPoints += weight;
            } else {
                if (q.prompt() != null && !q.prompt().isBlank()) {
                    recommendedTopics.add("Conceito avaliado em (" + diff.name() + "): " + q.prompt());
                }
            }

            results.add(new QuizQuestionResult(
                    q.id(),
                    selectedOptionId,
                    correctOpt != null ? correctOpt.id() : null,
                    isCorrect,
                    q.explanation()
            ));
        }

        Map<Difficulty, Double> scoreByDifficulty = new EnumMap<>(Difficulty.class);
        for (Difficulty diff : totalByDiff.keySet()) {
            int total = totalByDiff.get(diff);
            int correct = correctByDiff.getOrDefault(diff, 0);
            double pct = total > 0 ? (correct * 100.0) / total : 0.0;
            scoreByDifficulty.put(diff, Math.round(pct * 10.0) / 10.0);

            if (pct >= 80.0) {
                strengths.add("Excelente retenção conceitual em questões de nível " + diff.name() + " (" + (int) pct + "%).");
            } else {
                areasForImprovement.add("Necessidade de revisão em questões conceituais de nível " + diff.name() + " (" + (int) pct + "% de acerto).");
            }
        }

        int totalQuestions = quiz.questions().size();
        int correctAnswers = (int) results.stream().filter(QuizQuestionResult::isCorrect).count();
        double scorePercentage = totalQuestions > 0 ? (correctAnswers * 100.0) / totalQuestions : 0.0;
        double weightedScore = maxWeightedPoints > 0 ? (totalWeightedPoints * 100.0) / maxWeightedPoints : 0.0;

        return new QuizEvaluationReport(
                quiz.chapterId(),
                totalQuestions,
                correctAnswers,
                Math.round(scorePercentage * 10.0) / 10.0,
                Math.round(weightedScore * 10.0) / 10.0,
                scoreByDifficulty,
                strengths,
                areasForImprovement,
                recommendedTopics,
                results
        );
    }
}
