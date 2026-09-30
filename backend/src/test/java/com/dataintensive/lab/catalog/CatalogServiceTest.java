package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.Book;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogServiceTest {

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService();
    }

    @Test
    @DisplayName("Deve listar todos os livros disponíveis no catálogo")
    void shouldListAllBooks() {
        List<Book> books = catalogService.getAllBooks();

        assertThat(books).isNotEmpty();
        Book ddia = books.get(0);
        assertThat(ddia.id()).isEqualTo("ddia");
        assertThat(ddia.title()).isEqualTo("Designing Data-Intensive Applications");
        assertThat(ddia.author()).isEqualTo("Martin Kleppmann");
        assertThat(ddia.chapters()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve buscar livro por ID existente")
    void shouldFindBookByIdWhenExists() {
        Optional<Book> book = catalogService.findBookById("ddia");

        assertThat(book).isPresent();
        assertThat(book.get().title()).contains("Data-Intensive");
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar livro inexistente")
    void shouldReturnEmptyWhenBookDoesNotExist() {
        Optional<Book> book = catalogService.findBookById("livro-inexistente");

        assertThat(book).isEmpty();
    }

    @Test
    @DisplayName("Deve encontrar Lab 1 pelo ID e validar motor Postgres e desafios")
    void shouldFindLab1ById() {
        Optional<Lab> lab = catalogService.findLabById("ddia-cap-03-lab-01");

        assertThat(lab).isPresent();
        assertThat(lab.get().engineType()).isEqualTo(EngineType.POSTGRES);
        assertThat(lab.get().databaseName()).isEqualTo("ddia_lab");
        assertThat(lab.get().challenges()).hasSize(2);
        assertThat(lab.get().resetSchemaSql()).contains("DROP TABLE IF EXISTS");
    }

    @Test
    @DisplayName("Deve encontrar Lab 2 pelo ID e validar motor Neo4j")
    void shouldFindLab2ById() {
        Optional<Lab> lab = catalogService.findLabById("ddia-cap-03-lab-02");

        assertThat(lab).isPresent();
        assertThat(lab.get().engineType()).isEqualTo(EngineType.NEO4J);
        assertThat(lab.get().databaseName()).isEqualTo("neo4j");
        assertThat(lab.get().resetSchemaSql()).contains("MATCH (n) DETACH DELETE n");
    }

    @Test
    @DisplayName("Deve encontrar Lab por slug")
    void shouldFindLabBySlug() {
        Optional<Lab> lab = catalogService.findLabById("relacional-vs-documentos");

        assertThat(lab).isPresent();
        assertThat(lab.get().id()).isEqualTo("ddia-cap-03-lab-01");
    }
}
