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

        EngineType engine = EngineType.valueOf(row.engine_type().toUpperCase());

        return new Lab(
                row.id(),
                row.number(),
                row.slug(),
                lookup(translations, "LAB", row.id(), "title", row.title()),
                lookup(translations, "LAB", row.id(), "summary", row.summary()),
                keyConcepts,
                engine,
                row.database_name(),
                row.reset_schema_sql(),
                challenges
        );
    }

    private List<Challenge> findChallengesByLabId(String labId, Map<String, String> translations) {
        List<ChallengeRow> challengeRows = jdbcClient.sql("""
                SELECT id, lab_id, order_index, title, description, scenario, starter_template, reflection_prompt, engine_type
                FROM challenges
                WHERE lab_id = :labId
                ORDER BY order_index ASC
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

        EngineType engineType = null;
        if (row.engine_type() != null && !row.engine_type().isBlank()) {
            try {
                engineType = EngineType.valueOf(row.engine_type().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return new Challenge(
                row.id(),
                row.order_index(),
                lookup(translations, "CHALLENGE", row.id(), "title", row.title()),
                lookup(translations, "CHALLENGE", row.id(), "description", row.description()),
                lookup(translations, "CHALLENGE", row.id(), "scenario", row.scenario()),
                lookup(translations, "CHALLENGE", row.id(), "starter_template", row.starter_template()),
                guidelines,
                lookup(translations, "CHALLENGE", row.id(), "reflection_prompt", row.reflection_prompt()),
                engineType
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

    // Intermediate projection records
    public record BookRow(String id, String title, String author, String tag_line, String cover_color, String cover_image_url, String description) {}
    public record ChapterRow(String id, String book_id, int number, String title, String subtitle, String summary) {}
    public record LabRow(String id, String chapter_id, int number, String slug, String title, String summary, String engine_type, String database_name, String reset_schema_sql) {}
    public record ChallengeRow(String id, String lab_id, int order_index, String title, String description, String scenario, String starter_template, String reflection_prompt, String engine_type) {}
    public record LabConceptRow(String concept, int order_index) {}
    public record ChallengeGuidelineRow(String guideline_text, int order_index) {}
    public record TranslationRow(String entity_type, String entity_id, String field_name, String translation_text) {}
}
