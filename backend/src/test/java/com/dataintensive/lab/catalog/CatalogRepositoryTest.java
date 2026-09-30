package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.Book;
import com.dataintensive.lab.domain.Chapter;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CatalogRepositoryTest {

    @Autowired
    private CatalogRepository catalogRepository;

    @Test
    @DisplayName("Deve carregar todos os livros do banco de dados com capítulos e laboratórios aninhados")
    void shouldFindAllBooksWithNestedHierarchy() {
        List<Book> books = catalogRepository.findAllBooks();

        assertThat(books).isNotEmpty();
        Book ddia = books.stream().filter(b -> b.id().equals("ddia")).findFirst().orElseThrow();
        assertThat(ddia.title()).isEqualTo("Designing Data-Intensive Applications");
        assertThat(ddia.author()).isEqualTo("Martin Kleppmann");
        assertThat(ddia.chapters()).isNotEmpty();

        Chapter ch3 = ddia.chapters().get(0);
        assertThat(ch3.number()).isEqualTo(3);
        assertThat(ch3.title()).isEqualTo("Modelos de Dados e Linguagens de Consulta");
        assertThat(ch3.labs()).hasSize(4);
    }

    @Test
    @DisplayName("Deve buscar livro por ID existente no banco de dados")
    void shouldFindBookById() {
        Optional<Book> book = catalogRepository.findBookById("ddia");

        assertThat(book).isPresent();
        assertThat(book.get().id()).isEqualTo("ddia");
        assertThat(book.get().chapters()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar livro inexistente")
    void shouldReturnEmptyForUnknownBook() {
        Optional<Book> book = catalogRepository.findBookById("livro-fantasma");

        assertThat(book).isEmpty();
    }

    @Test
    @DisplayName("Deve buscar laboratório por ID com desafios, guidelines e conceitos")
    void shouldFindLabById() {
        Optional<Lab> lab = catalogRepository.findLabById("ddia-cap-03-lab-01");

        assertThat(lab).isPresent();
        assertThat(lab.get().engineType()).isEqualTo(EngineType.POSTGRES);
        assertThat(lab.get().databaseName()).isEqualTo("tbl_lab");
        assertThat(lab.get().keyConcepts()).contains("Impedance Mismatch");
        assertThat(lab.get().challenges()).hasSize(2);
        assertThat(lab.get().challenges().get(0).guidelines()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve buscar laboratório por slug")
    void shouldFindLabBySlug() {
        Optional<Lab> lab = catalogRepository.findLabById("relacional-vs-documentos");

        assertThat(lab).isPresent();
        assertThat(lab.get().id()).isEqualTo("ddia-cap-03-lab-01");
    }
}
