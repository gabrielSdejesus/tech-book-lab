package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.Book;
import com.dataintensive.lab.domain.Lab;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CatalogService {

    private final CatalogRepository catalogRepository;

    public CatalogService(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    public List<Book> getAllBooks() {
        return catalogRepository.findAllBooks();
    }

    public List<Book> getAllBooks(String locale) {
        return catalogRepository.findAllBooks(normalizeLocale(locale));
    }

    public Optional<Book> findBookById(String bookId) {
        return catalogRepository.findBookById(bookId);
    }

    public Optional<Book> findBookById(String bookId, String locale) {
        return catalogRepository.findBookById(bookId, normalizeLocale(locale));
    }

    public Optional<Lab> findLabById(String labId) {
        return catalogRepository.findLabById(labId);
    }

    public Optional<Lab> findLabById(String labId, String locale) {
        return catalogRepository.findLabById(labId, normalizeLocale(locale));
    }

    public void saveChallengeSolution(String challengeId, String code) {
        saveChallengeSolution(challengeId, code, null);
    }

    public void saveChallengeSolution(String challengeId, String code, String userReflection) {
        catalogRepository.saveChallengeSolution(challengeId, code, userReflection);
    }

    public void deleteChallengeSolution(String challengeId) {
        catalogRepository.deleteChallengeSolution(challengeId);
    }

    public List<com.dataintensive.lab.domain.InteractiveScenario> getScenariosByLabId(String labId, String locale) {
        return catalogRepository.findScenariosByLabId(labId, normalizeLocale(locale));
    }

    public Optional<com.dataintensive.lab.domain.InteractiveScenario> getScenarioById(String scenarioId, String locale) {
        return catalogRepository.findScenarioById(scenarioId, normalizeLocale(locale));
    }

    public void saveScenarioSolution(String scenarioId, com.dataintensive.lab.catalog.dto.ScenarioSolutionRequest request) {
        catalogRepository.saveScenarioSolution(scenarioId, request.userStateJson(), request.userReflection(), "IN_PROGRESS");
    }

    public Optional<com.dataintensive.lab.domain.ChapterQuiz> getChapterQuiz(String chapterId, String locale) {
        return catalogRepository.findQuizByChapterId(chapterId, normalizeLocale(locale));
    }

    public com.dataintensive.lab.catalog.dto.QuizSubmissionResponse submitChapterQuiz(String chapterId, com.dataintensive.lab.catalog.dto.QuizSubmissionRequest request) {
        var quizOpt = catalogRepository.findQuizByChapterId(chapterId, "pt");
        if (quizOpt.isEmpty()) {
            throw new com.dataintensive.lab.domain.DomainValidationException("Capítulo não encontrado ou não possui simulado: " + chapterId);
        }

        var quiz = quizOpt.get();
        java.util.List<com.dataintensive.lab.catalog.dto.QuizQuestionResult> results = new java.util.ArrayList<>();

        for (var q : quiz.questions()) {
            var correctOpt = q.options().stream()
                    .filter(com.dataintensive.lab.domain.QuizOption::isCorrect)
                    .findFirst()
                    .orElse(null);

            String selectedOptionId = request.answers() != null ? request.answers().get(q.id()) : null;
            boolean isCorrect = selectedOptionId != null && correctOpt != null && selectedOptionId.equals(correctOpt.id());

            if (selectedOptionId != null) {
                catalogRepository.saveQuizSubmission(chapterId, q.id(), selectedOptionId, isCorrect);
            }

            results.add(new com.dataintensive.lab.catalog.dto.QuizQuestionResult(
                    q.id(),
                    selectedOptionId,
                    correctOpt != null ? correctOpt.id() : null,
                    isCorrect,
                    q.explanation()
            ));
        }

        int totalQuestions = quiz.questions().size();
        int correctAnswers = (int) results.stream().filter(com.dataintensive.lab.catalog.dto.QuizQuestionResult::isCorrect).count();
        double scorePercentage = totalQuestions > 0 ? (correctAnswers * 100.0) / totalQuestions : 0.0;

        return new com.dataintensive.lab.catalog.dto.QuizSubmissionResponse(
                chapterId,
                totalQuestions,
                correctAnswers,
                Math.round(scorePercentage * 10.0) / 10.0,
                results
        );
    }

    private String normalizeLocale(String raw) {
        if (raw == null || raw.isBlank()) {
            return "pt";
        }
        String clean = raw.trim().toLowerCase();
        if (clean.startsWith("en")) {
            return "en";
        }
        return "pt";
    }
}
