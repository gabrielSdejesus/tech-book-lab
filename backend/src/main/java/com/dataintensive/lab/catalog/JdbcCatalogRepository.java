package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JdbcCatalogRepository implements CatalogRepository {

    private final JdbcClient jdbcClient;

    public JdbcCatalogRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Book> findAllBooks() {
        return findAllBooks("pt");
    }

    @Override
    public List<Book> findAllBooks(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        Map<String, String> translations = loadTranslations(normalizedLocale);

        List<BookRow> bookRows = jdbcClient.sql("""
                SELECT id, title, author, tag_line, cover_color, cover_image_url, description
                FROM books
                ORDER BY created_at ASC
                """)
                .query(BookRow.class)
                .list();

        return bookRows.stream().map(row -> mapToBook(row, translations)).toList();
    }

    @Override
    public Optional<Book> findBookById(String bookId) {
        return findBookById(bookId, "pt");
    }

    @Override
    public Optional<Book> findBookById(String bookId, String locale) {
        String normalizedLocale = normalizeLocale(locale);
        Map<String, String> translations = loadTranslations(normalizedLocale);

        return jdbcClient.sql("""
                SELECT id, title, author, tag_line, cover_color, cover_image_url, description
                FROM books
                WHERE LOWER(id) = LOWER(:bookId)
                """)
                .param("bookId", bookId)
                .query(BookRow.class)
                .optional()
                .map(row -> mapToBook(row, translations));
    }

    @Override
    public Optional<Lab> findLabById(String labIdOrSlug) {
        return findLabById(labIdOrSlug, "pt");
    }

    @Override
    public Optional<Lab> findLabById(String labIdOrSlug, String locale) {
        String normalizedLocale = normalizeLocale(locale);
        Map<String, String> translations = loadTranslations(normalizedLocale);

        return jdbcClient.sql("""
                SELECT id, chapter_id, number, slug, title, summary, engine_type, database_name, reset_schema_sql
                FROM labs
                WHERE LOWER(id) = LOWER(:labId) OR LOWER(slug) = LOWER(:labId)
                """)
                .param("labId", labIdOrSlug)
                .query(LabRow.class)
                .optional()
                .map(row -> mapToLab(row, translations));
    }

    @Override
    public void saveChallengeSolution(String challengeId, String code) {
        saveChallengeSolution(challengeId, code, null);
    }

    @Override
    public void saveChallengeSolution(String challengeId, String code, String userReflection) {
        jdbcClient.sql("DELETE FROM challenge_user_solutions WHERE challenge_id = :challengeId")
                .param("challengeId", challengeId)
                .update();

        jdbcClient.sql("INSERT INTO challenge_user_solutions (challenge_id, code, user_reflection, updated_at) VALUES (:challengeId, :code, :userReflection, CURRENT_TIMESTAMP)")
                .param("challengeId", challengeId)
                .param("code", code)
                .param("userReflection", userReflection)
                .update();
    }

    @Override
    public void deleteChallengeSolution(String challengeId) {
        jdbcClient.sql("DELETE FROM challenge_user_solutions WHERE challenge_id = :challengeId")
                .param("challengeId", challengeId)
                .update();
    }

    private Map<String, String> loadTranslations(String locale) {
        if ("pt".equalsIgnoreCase(locale) || locale == null || locale.isBlank()) {
            return Collections.emptyMap();
        }

        List<TranslationRow> rows = jdbcClient.sql("""
                SELECT entity_type, entity_id, field_name, translation_text
                FROM catalog_translations
                WHERE LOWER(locale) = LOWER(:locale)
                """)
                .param("locale", locale)
                .query(TranslationRow.class)
                .list();

        return rows.stream().collect(Collectors.toMap(
                r -> r.entity_type().toUpperCase() + ":" + r.entity_id() + ":" + r.field_name().toLowerCase(),
                TranslationRow::translation_text,
                (existing, replacement) -> replacement
        ));
    }

    private String lookup(Map<String, String> translations, String entityType, String entityId, String fieldName, String fallback) {
        if (translations == null || translations.isEmpty()) {
            return fallback;
        }
        String key = entityType.toUpperCase() + ":" + entityId + ":" + fieldName.toLowerCase();
        String val = translations.get(key);
        return (val != null && !val.isBlank()) ? val : fallback;
    }

    private Book mapToBook(BookRow row, Map<String, String> translations) {
        List<Chapter> chapters = findChaptersByBookId(row.id(), translations);
        return new Book(
                row.id(),
                lookup(translations, "BOOK", row.id(), "title", row.title()),
                row.author(),
                lookup(translations, "BOOK", row.id(), "tag_line", row.tag_line()),
                row.cover_color(),
                row.cover_image_url(),
                lookup(translations, "BOOK", row.id(), "description", row.description()),
                chapters
        );
    }

    private List<Chapter> findChaptersByBookId(String bookId, Map<String, String> translations) {
        List<ChapterRow> chapterRows = jdbcClient.sql("""
                SELECT id, book_id, number, title, subtitle, summary
                FROM chapters
                WHERE book_id = :bookId
                ORDER BY number ASC
                """)
                .param("bookId", bookId)
                .query(ChapterRow.class)
                .list();

        return chapterRows.stream().map(row -> mapToChapter(row, translations)).toList();
    }

    private Chapter mapToChapter(ChapterRow row, Map<String, String> translations) {
        List<Lab> labs = findLabsByChapterId(row.id(), translations);
        return new Chapter(
                row.id(),
                row.number(),
                lookup(translations, "CHAPTER", row.id(), "title", row.title()),
                lookup(translations, "CHAPTER", row.id(), "subtitle", row.subtitle()),
                lookup(translations, "CHAPTER", row.id(), "summary", row.summary()),
                labs
        );
    }

    private List<Lab> findLabsByChapterId(String chapterId, Map<String, String> translations) {
        List<LabRow> labRows = jdbcClient.sql("""
                SELECT id, chapter_id, number, slug, title, summary, engine_type, database_name, reset_schema_sql
                FROM labs
                WHERE chapter_id = :chapterId
                ORDER BY number ASC
                """)
                .param("chapterId", chapterId)
                .query(LabRow.class)
                .list();

        return labRows.stream().map(row -> mapToLab(row, translations)).toList();
    }

    private Lab mapToLab(LabRow row, Map<String, String> translations) {
        List<LabConceptRow> conceptRows = jdbcClient.sql("""
                SELECT concept, order_index
                FROM lab_key_concepts
                WHERE lab_id = :labId
                ORDER BY order_index ASC
                """)
                .param("labId", row.id())
                .query(LabConceptRow.class)
                .list();

        List<String> keyConcepts = conceptRows.stream()
                .map(cr -> lookup(translations, "LAB_CONCEPT", row.id() + ":" + cr.order_index(), "concept", cr.concept()))
                .toList();

        List<Challenge> challenges = findChallengesByLabId(row.id(), translations);

        return new Lab(
                row.id(),
                row.number(),
                row.slug(),
                lookup(translations, "LAB", row.id(), "title", row.title()),
                lookup(translations, "LAB", row.id(), "summary", row.summary()),
                keyConcepts,
                row.database_name(),
                row.reset_schema_sql(),
                challenges
        );
    }

    private List<Challenge> findChallengesByLabId(String labId, Map<String, String> translations) {
        List<ChallengeRow> challengeRows = jdbcClient.sql("""
                SELECT c.id, c.lab_id, c.order_index, c.title, c.description, c.scenario,
                       c.starter_template, s.code AS saved_code, s.user_reflection AS saved_reflection,
                       c.reflection_prompt, c.expected_reflection, c.engine_type
                FROM challenges c
                LEFT JOIN challenge_user_solutions s ON c.id = s.challenge_id
                WHERE c.lab_id = :labId
                ORDER BY c.order_index ASC
                """)
                .param("labId", labId)
                .query(ChallengeRow.class)
                .list();

        return challengeRows.stream().map(row -> mapToChallenge(row, translations)).toList();
    }

    private Challenge mapToChallenge(ChallengeRow row, Map<String, String> translations) {
        List<ChallengeGuidelineRow> guidelineRows = jdbcClient.sql("""
                SELECT guideline_text, order_index
                FROM challenge_guidelines
                WHERE challenge_id = :challengeId
                ORDER BY order_index ASC
                """)
                .param("challengeId", row.id())
                .query(ChallengeGuidelineRow.class)
                .list();

        List<String> guidelines = guidelineRows.stream()
                .map(gr -> lookup(translations, "GUIDELINE", row.id() + ":" + gr.order_index(), "guideline_text", gr.guideline_text()))
                .toList();

        return new Challenge(
                row.id(),
                row.order_index(),
                lookup(translations, "CHALLENGE", row.id(), "title", row.title()),
                lookup(translations, "CHALLENGE", row.id(), "description", row.description()),
                lookup(translations, "CHALLENGE", row.id(), "scenario", row.scenario()),
                lookup(translations, "CHALLENGE", row.id(), "starter_template", row.starter_template()),
                row.saved_code(),
                row.saved_reflection(),
                guidelines,
                lookup(translations, "CHALLENGE", row.id(), "reflection_prompt", row.reflection_prompt()),
                lookup(translations, "CHALLENGE", row.id(), "expected_reflection", row.expected_reflection())
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

    @Override
    public List<InteractiveScenario> findScenariosByLabId(String labId, String locale) {
        String normalized = normalizeLocale(locale);
        return jdbcClient.sql("""
                SELECT s.id, s.lab_id, s.scenario_type, s.difficulty, s.order_index,
                       COALESCE(st.title, st_def.title) as title,
                       COALESCE(st.description, st_def.description) as description,
                       COALESCE(st.context_briefing, st_def.context_briefing) as context_briefing,
                       COALESCE(st.expected_reflection, st_def.expected_reflection) as expected_reflection,
                       s.constraints_json, s.initial_state_json, s.validation_rules_json,
                       uss.user_state_json as saved_user_state_json,
                       uss.user_reflection as saved_user_reflection,
                       COALESCE(uss.status, 'NOT_STARTED') as status
                FROM interactive_scenarios s
                LEFT JOIN scenario_translations st ON st.scenario_id = s.id AND st.locale = :locale
                LEFT JOIN scenario_translations st_def ON st_def.scenario_id = s.id AND st_def.locale = 'pt'
                LEFT JOIN user_scenario_solutions uss ON uss.scenario_id = s.id
                WHERE s.lab_id = :labId
                ORDER BY s.order_index ASC
                """)
                .param("labId", labId)
                .param("locale", normalized)
                .query((rs, rowNum) -> new InteractiveScenario(
                        rs.getString("id"),
                        rs.getString("lab_id"),
                        ScenarioType.fromString(rs.getString("scenario_type")),
                        Difficulty.fromString(rs.getString("difficulty")),
                        rs.getInt("order_index"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("context_briefing"),
                        rs.getString("expected_reflection"),
                        rs.getString("constraints_json"),
                        rs.getString("initial_state_json"),
                        rs.getString("validation_rules_json"),
                        rs.getString("saved_user_state_json"),
                        rs.getString("saved_user_reflection"),
                        rs.getString("status")
                ))
                .list();
    }

    @Override
    public Optional<InteractiveScenario> findScenarioById(String scenarioId, String locale) {
        String normalized = normalizeLocale(locale);
        return jdbcClient.sql("""
                SELECT s.id, s.lab_id, s.scenario_type, s.difficulty, s.order_index,
                       COALESCE(st.title, st_def.title) as title,
                       COALESCE(st.description, st_def.description) as description,
                       COALESCE(st.context_briefing, st_def.context_briefing) as context_briefing,
                       COALESCE(st.expected_reflection, st_def.expected_reflection) as expected_reflection,
                       s.constraints_json, s.initial_state_json, s.validation_rules_json,
                       uss.user_state_json as saved_user_state_json,
                       uss.user_reflection as saved_user_reflection,
                       COALESCE(uss.status, 'NOT_STARTED') as status
                FROM interactive_scenarios s
                LEFT JOIN scenario_translations st ON st.scenario_id = s.id AND st.locale = :locale
                LEFT JOIN scenario_translations st_def ON st_def.scenario_id = s.id AND st_def.locale = 'pt'
                LEFT JOIN user_scenario_solutions uss ON uss.scenario_id = s.id
                WHERE s.id = :scenarioId
                """)
                .param("scenarioId", scenarioId)
                .param("locale", normalized)
                .query((rs, rowNum) -> new InteractiveScenario(
                        rs.getString("id"),
                        rs.getString("lab_id"),
                        ScenarioType.fromString(rs.getString("scenario_type")),
                        Difficulty.fromString(rs.getString("difficulty")),
                        rs.getInt("order_index"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("context_briefing"),
                        rs.getString("expected_reflection"),
                        rs.getString("constraints_json"),
                        rs.getString("initial_state_json"),
                        rs.getString("validation_rules_json"),
                        rs.getString("saved_user_state_json"),
                        rs.getString("saved_user_reflection"),
                        rs.getString("status")
                ))
                .optional();
    }

    @Override
    public void saveScenarioSolution(String scenarioId, String userStateJson, String userReflection, String status) {
        jdbcClient.sql("DELETE FROM user_scenario_solutions WHERE scenario_id = :scenarioId")
                .param("scenarioId", scenarioId)
                .update();

        jdbcClient.sql("""
                INSERT INTO user_scenario_solutions (scenario_id, user_state_json, user_reflection, status, updated_at)
                VALUES (:scenarioId, :userStateJson, :userReflection, :status, CURRENT_TIMESTAMP)
                """)
                .param("scenarioId", scenarioId)
                .param("userStateJson", userStateJson)
                .param("userReflection", userReflection)
                .param("status", status != null ? status : "IN_PROGRESS")
                .update();
    }

    @Override
    public Optional<ChapterQuiz> findQuizByChapterId(String chapterId, String locale) {
        String normalized = normalizeLocale(locale);

        List<QuizQuestionRow> questionRows = jdbcClient.sql("""
                SELECT q.id, q.chapter_id, q.order_index, q.difficulty,
                       COALESCE(qt.prompt, qt_def.prompt) as prompt,
                       COALESCE(qt.explanation, qt_def.explanation) as explanation,
                       uqs.selected_option_id,
                       uqs.is_correct as is_answered_correctly
                FROM chapter_quiz_questions q
                LEFT JOIN quiz_question_translations qt ON qt.question_id = q.id AND qt.locale = :locale
                LEFT JOIN quiz_question_translations qt_def ON qt_def.question_id = q.id AND qt_def.locale = 'pt'
                LEFT JOIN user_quiz_submissions uqs ON uqs.chapter_id = q.chapter_id AND uqs.question_id = q.id
                WHERE q.chapter_id = :chapterId
                ORDER BY q.order_index ASC
                """)
                .param("chapterId", chapterId)
                .param("locale", normalized)
                .query((rs, rowNum) -> new QuizQuestionRow(
                        rs.getString("id"),
                        rs.getString("chapter_id"),
                        rs.getInt("order_index"),
                        rs.getString("difficulty"),
                        rs.getString("prompt"),
                        rs.getString("explanation"),
                        rs.getString("selected_option_id"),
                        rs.getObject("is_answered_correctly") != null ? rs.getBoolean("is_answered_correctly") : null
                ))
                .list();

        if (questionRows.isEmpty()) {
            return Optional.empty();
        }

        List<QuizOptionRow> optionRows = jdbcClient.sql("""
                SELECT o.id, o.question_id, o.option_letter, o.is_correct,
                       COALESCE(ot.text, ot_def.text) as text
                FROM quiz_options o
                JOIN chapter_quiz_questions q ON q.id = o.question_id
                LEFT JOIN quiz_option_translations ot ON ot.option_id = o.id AND ot.locale = :locale
                LEFT JOIN quiz_option_translations ot_def ON ot_def.option_id = o.id AND ot_def.locale = 'pt'
                WHERE q.chapter_id = :chapterId
                ORDER BY o.question_id, o.option_letter ASC
                """)
                .param("chapterId", chapterId)
                .param("locale", normalized)
                .query((rs, rowNum) -> new QuizOptionRow(
                        rs.getString("id"),
                        rs.getString("question_id"),
                        rs.getString("option_letter"),
                        rs.getString("text"),
                        rs.getBoolean("is_correct")
                ))
                .list();

        Map<String, List<QuizOption>> optionsByQuestion = optionRows.stream()
                .collect(Collectors.groupingBy(
                        QuizOptionRow::questionId,
                        Collectors.mapping(
                                row -> new QuizOption(row.id(), row.questionId(), row.optionLetter(), row.text(), row.isCorrect()),
                                Collectors.toList()
                        )
                ));

        List<QuizQuestion> questions = questionRows.stream().map(row -> new QuizQuestion(
                row.id(),
                row.chapterId(),
                row.orderIndex(),
                Difficulty.fromString(row.difficulty()),
                row.prompt(),
                row.explanation(),
                optionsByQuestion.getOrDefault(row.id(), List.of()),
                row.selectedOptionId(),
                row.isAnsweredCorrectly()
        )).toList();

        int totalQuestions = questions.size();
        int totalAnswered = (int) questions.stream().filter(q -> q.selectedOptionId() != null).count();
        int totalCorrect = (int) questions.stream().filter(q -> Boolean.TRUE.equals(q.isAnsweredCorrectly())).count();

        return Optional.of(new ChapterQuiz(chapterId, questions, totalQuestions, totalAnswered, totalCorrect));
    }

    @Override
    public void saveQuizSubmission(String chapterId, String questionId, String selectedOptionId, boolean isCorrect) {
        jdbcClient.sql("DELETE FROM user_quiz_submissions WHERE chapter_id = :chapterId AND question_id = :questionId")
                .param("chapterId", chapterId)
                .param("questionId", questionId)
                .update();

        jdbcClient.sql("""
                INSERT INTO user_quiz_submissions (chapter_id, question_id, selected_option_id, is_correct, answered_at)
                VALUES (:chapterId, :questionId, :selectedOptionId, :isCorrect, CURRENT_TIMESTAMP)
                """)
                .param("chapterId", chapterId)
                .param("questionId", questionId)
                .param("selectedOptionId", selectedOptionId)
                .param("isCorrect", isCorrect)
                .update();
    }

    @Override
    public List<QuizOption> findOptionsByQuestionId(String questionId) {
        return jdbcClient.sql("""
                SELECT o.id, o.question_id, o.option_letter, o.is_correct,
                       COALESCE(ot.text, '') as text
                FROM quiz_options o
                LEFT JOIN quiz_option_translations ot ON ot.option_id = o.id AND ot.locale = 'pt'
                WHERE o.question_id = :questionId
                ORDER BY o.option_letter ASC
                """)
                .param("questionId", questionId)
                .query((rs, rowNum) -> new QuizOption(
                        rs.getString("id"),
                        rs.getString("question_id"),
                        rs.getString("option_letter"),
                        rs.getString("text"),
                        rs.getBoolean("is_correct")
                ))
                .list();
    }

    // Intermediate projection records
    public record QuizQuestionRow(String id, String chapterId, int orderIndex, String difficulty, String prompt, String explanation, String selectedOptionId, Boolean isAnsweredCorrectly) {}
    public record QuizOptionRow(String id, String questionId, String optionLetter, String text, boolean isCorrect) {}
    public record BookRow(String id, String title, String author, String tag_line, String cover_color, String cover_image_url, String description) {}
    public record ChapterRow(String id, String book_id, int number, String title, String subtitle, String summary) {}
    public record LabRow(String id, String chapter_id, int number, String slug, String title, String summary, String engine_type, String database_name, String reset_schema_sql) {}
    public record ChallengeRow(String id, String lab_id, int order_index, String title, String description, String scenario, String starter_template, String saved_code, String saved_reflection, String reflection_prompt, String expected_reflection, String engine_type) {}
    public record LabConceptRow(String concept, int order_index) {}
    public record ChallengeGuidelineRow(String guideline_text, int order_index) {}
    public record TranslationRow(String entity_type, String entity_id, String field_name, String translation_text) {}
}
