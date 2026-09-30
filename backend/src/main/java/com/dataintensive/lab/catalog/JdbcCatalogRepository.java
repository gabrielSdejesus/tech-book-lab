package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcCatalogRepository implements CatalogRepository {

    private final JdbcClient jdbcClient;

    public JdbcCatalogRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Book> findAllBooks() {
        List<BookRow> bookRows = jdbcClient.sql("""
                SELECT id, title, author, tag_line, cover_color, cover_image_url, description
                FROM books
                ORDER BY created_at ASC
                """)
                .query(BookRow.class)
                .list();

        return bookRows.stream().map(this::mapToBook).toList();
    }

    @Override
    public Optional<Book> findBookById(String bookId) {
        return jdbcClient.sql("""
                SELECT id, title, author, tag_line, cover_color, cover_image_url, description
                FROM books
                WHERE LOWER(id) = LOWER(:bookId)
                """)
                .param("bookId", bookId)
                .query(BookRow.class)
                .optional()
                .map(this::mapToBook);
    }

    @Override
    public Optional<Lab> findLabById(String labIdOrSlug) {
        return jdbcClient.sql("""
                SELECT id, chapter_id, number, slug, title, summary, engine_type, database_name, reset_schema_sql
                FROM labs
                WHERE LOWER(id) = LOWER(:labId) OR LOWER(slug) = LOWER(:labId)
                """)
                .param("labId", labIdOrSlug)
                .query(LabRow.class)
                .optional()
                .map(this::mapToLab);
    }

    private Book mapToBook(BookRow row) {
        List<Chapter> chapters = findChaptersByBookId(row.id());
        return new Book(
                row.id(),
                row.title(),
                row.author(),
                row.tag_line(),
                row.cover_color(),
                row.cover_image_url(),
                row.description(),
                chapters
        );
    }

    private List<Chapter> findChaptersByBookId(String bookId) {
        List<ChapterRow> chapterRows = jdbcClient.sql("""
                SELECT id, book_id, number, title, subtitle, summary
                FROM chapters
                WHERE book_id = :bookId
                ORDER BY number ASC
                """)
                .param("bookId", bookId)
                .query(ChapterRow.class)
                .list();

        return chapterRows.stream().map(this::mapToChapter).toList();
    }

    private Chapter mapToChapter(ChapterRow row) {
        List<Lab> labs = findLabsByChapterId(row.id());
        return new Chapter(
                row.id(),
                row.number(),
                row.title(),
                row.subtitle(),
                row.summary(),
                labs
        );
    }

    private List<Lab> findLabsByChapterId(String chapterId) {
        List<LabRow> labRows = jdbcClient.sql("""
                SELECT id, chapter_id, number, slug, title, summary, engine_type, database_name, reset_schema_sql
                FROM labs
                WHERE chapter_id = :chapterId
                ORDER BY number ASC
                """)
                .param("chapterId", chapterId)
                .query(LabRow.class)
                .list();

        return labRows.stream().map(this::mapToLab).toList();
    }

    private Lab mapToLab(LabRow row) {
        List<String> keyConcepts = jdbcClient.sql("""
                SELECT concept
                FROM lab_key_concepts
                WHERE lab_id = :labId
                ORDER BY order_index ASC
                """)
                .param("labId", row.id())
                .query(String.class)
                .list();

        List<Challenge> challenges = findChallengesByLabId(row.id());

        EngineType engine = EngineType.valueOf(row.engine_type().toUpperCase());

        return new Lab(
                row.id(),
                row.number(),
                row.slug(),
                row.title(),
                row.summary(),
                keyConcepts,
                engine,
                row.database_name(),
                row.reset_schema_sql(),
                challenges
        );
    }

    private List<Challenge> findChallengesByLabId(String labId) {
        List<ChallengeRow> challengeRows = jdbcClient.sql("""
                SELECT id, lab_id, order_index, title, description, scenario, starter_template, reflection_prompt
                FROM challenges
                WHERE lab_id = :labId
                ORDER BY order_index ASC
                """)
                .param("labId", labId)
                .query(ChallengeRow.class)
                .list();

        return challengeRows.stream().map(this::mapToChallenge).toList();
    }

    private Challenge mapToChallenge(ChallengeRow row) {
        List<String> guidelines = jdbcClient.sql("""
                SELECT guideline_text
                FROM challenge_guidelines
                WHERE challenge_id = :challengeId
                ORDER BY order_index ASC
                """)
                .param("challengeId", row.id())
                .query(String.class)
                .list();

        return new Challenge(
                row.id(),
                row.order_index(),
                row.title(),
                row.description(),
                row.scenario(),
                row.starter_template(),
                guidelines,
                row.reflection_prompt()
        );
    }

    // Intermediate projection records
    public record BookRow(String id, String title, String author, String tag_line, String cover_color, String cover_image_url, String description) {}
    public record ChapterRow(String id, String book_id, int number, String title, String subtitle, String summary) {}
    public record LabRow(String id, String chapter_id, int number, String slug, String title, String summary, String engine_type, String database_name, String reset_schema_sql) {}
    public record ChallengeRow(String id, String lab_id, int order_index, String title, String description, String scenario, String starter_template, String reflection_prompt) {}
}
