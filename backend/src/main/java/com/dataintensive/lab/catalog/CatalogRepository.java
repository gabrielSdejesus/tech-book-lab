package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.Book;
import com.dataintensive.lab.domain.ChapterQuiz;
import com.dataintensive.lab.domain.InteractiveScenario;
import com.dataintensive.lab.domain.Lab;
import com.dataintensive.lab.domain.QuizOption;

import java.util.List;
import java.util.Optional;

public interface CatalogRepository {
    List<Book> findAllBooks();
    default List<Book> findAllBooks(String locale) {
        return findAllBooks();
    }

    Optional<Book> findBookById(String bookId);
    default Optional<Book> findBookById(String bookId, String locale) {
        return findBookById(bookId);
    }

    Optional<Lab> findLabById(String labIdOrSlug);
    default Optional<Lab> findLabById(String labIdOrSlug, String locale) {
        return findLabById(labIdOrSlug);
    }

    void saveChallengeSolution(String challengeId, String code);

    default void saveChallengeSolution(String challengeId, String code, String userReflection) {
        saveChallengeSolution(challengeId, code);
    }

    void deleteChallengeSolution(String challengeId);

    List<InteractiveScenario> findScenariosByLabId(String labId, String locale);

    Optional<InteractiveScenario> findScenarioById(String scenarioId, String locale);

    void saveScenarioSolution(String scenarioId, String userStateJson, String userReflection, String status);

    Optional<ChapterQuiz> findQuizByChapterId(String chapterId, String locale);

    void saveQuizSubmission(String chapterId, String questionId, String selectedOptionId, boolean isCorrect);

    List<QuizOption> findOptionsByQuestionId(String questionId);
}
