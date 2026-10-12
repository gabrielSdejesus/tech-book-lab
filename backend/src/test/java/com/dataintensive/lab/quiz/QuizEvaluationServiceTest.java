package com.dataintensive.lab.quiz;

import com.dataintensive.lab.catalog.dto.QuizSubmissionRequest;
import com.dataintensive.lab.domain.ChapterQuiz;
import com.dataintensive.lab.domain.Difficulty;
import com.dataintensive.lab.domain.QuizOption;
import com.dataintensive.lab.domain.QuizQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuizEvaluationServiceTest {

    private QuizEvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        evaluationService = new QuizEvaluationService();
    }

    @Test
    @DisplayName("Deve avaliar simulado calculando pontuação ponderada por dificuldade e gerando recomendações pedagógicas")
    void shouldEvaluateQuizWithWeightedScoreAndPedagogicalReport() {
        var optEasyA = new QuizOption("opt-1", "q-1", "A", "Documentos JSON", true);
        var optEasyB = new QuizOption("opt-2", "q-1", "B", "Tabelas 3NF", false);
        var qEasy = new QuizQuestion("q-1", "cap-3", 1, Difficulty.EASY, "Pergunta Fácil", "Explicação Fácil", List.of(optEasyA, optEasyB), null, null);

        var optMedA = new QuizOption("opt-3", "q-2", "A", "B-Tree", true);
        var optMedB = new QuizOption("opt-4", "q-2", "B", "LSM-Tree", false);
        var qMed = new QuizQuestion("q-2", "cap-3", 2, Difficulty.MEDIUM, "Pergunta Média", "Explicação Média", List.of(optMedA, optMedB), null, null);

        var optHardA = new QuizOption("opt-5", "q-3", "A", "Consistência Linear", true);
        var optHardB = new QuizOption("opt-6", "q-3", "B", "Eventual", false);
        var qHard = new QuizQuestion("q-3", "cap-3", 3, Difficulty.HARD, "Pergunta Difícil", "Explicação Difícil", List.of(optHardA, optHardB), null, null);

        var quiz = new ChapterQuiz("cap-3", List.of(qEasy, qMed, qHard), 3, 0, 0);

        // Aluno acerta a Fácil (opt-1) e a Média (opt-3), mas erra a Difícil (responde opt-6)
        var submission = new QuizSubmissionRequest(Map.of(
                "q-1", "opt-1",
                "q-2", "opt-3",
                "q-3", "opt-6"
        ));

        QuizEvaluationReport report = evaluationService.evaluate(quiz, submission);

        assertThat(report.chapterId()).isEqualTo("cap-3");
        assertThat(report.totalQuestions()).isEqualTo(3);
        assertThat(report.correctAnswers()).isEqualTo(2);
        assertThat(report.scorePercentage()).isEqualTo(66.7);

        // Pontuação ponderada: Fácil peso 1, Média peso 2, Difícil peso 3 (total = 6). Acertos: 1 + 2 = 3 / 6 = 50.0%
        assertThat(report.weightedScore()).isEqualTo(50.0);

        // Quebra por dificuldade
        assertThat(report.scoreByDifficulty()).containsEntry(Difficulty.EASY, 100.0);
        assertThat(report.scoreByDifficulty()).containsEntry(Difficulty.MEDIUM, 100.0);
        assertThat(report.scoreByDifficulty()).containsEntry(Difficulty.HARD, 0.0);

        // Recomendações pedagógicas
        assertThat(report.areasForImprovement()).isNotEmpty();
        assertThat(report.recommendedTopics()).anyMatch(t -> t.toLowerCase().contains("difícil") || t.toLowerCase().contains("consistência"));
    }
}
