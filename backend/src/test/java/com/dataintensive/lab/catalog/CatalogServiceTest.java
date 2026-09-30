package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.Book;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CatalogRepository catalogRepository;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(catalogRepository);
    }

    @Test
    @DisplayName("Deve delegar ao repositório para listar todos os livros")
    void shouldListAllBooks() {
        Book book = new Book("ddia", "Designing Data-Intensive Applications", "Martin Kleppmann", "Tag", "#059669", "/covers/ddia.svg", "Desc", List.of());
        when(catalogRepository.findAllBooks()).thenReturn(List.of(book));

        List<Book> books = catalogService.getAllBooks();

        assertThat(books).containsExactly(book);
        verify(catalogRepository).findAllBooks();
    }

    @Test
    @DisplayName("Deve delegar ao repositório para buscar livro existente")
    void shouldFindBookByIdWhenExists() {
        Book book = new Book("ddia", "Designing Data-Intensive Applications", "Martin Kleppmann", "Tag", "#059669", "/covers/ddia.svg", "Desc", List.of());
        when(catalogRepository.findBookById("ddia")).thenReturn(Optional.of(book));

        Optional<Book> result = catalogService.findBookById("ddia");

        assertThat(result).contains(book);
        verify(catalogRepository).findBookById("ddia");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para retornar vazio ao buscar livro inexistente")
    void shouldReturnEmptyWhenBookDoesNotExist() {
        when(catalogRepository.findBookById("livro-inexistente")).thenReturn(Optional.empty());

        Optional<Book> result = catalogService.findBookById("livro-inexistente");

        assertThat(result).isEmpty();
        verify(catalogRepository).findBookById("livro-inexistente");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para encontrar Lab por ID")
    void shouldFindLabById() {
        Lab lab = new Lab("ddia-cap-03-lab-01", 1, "relacional-vs-documentos", "Relacional", "Sum", List.of(), EngineType.POSTGRES, "tbl_lab", "DROP TABLE...", List.of());
        when(catalogRepository.findLabById("ddia-cap-03-lab-01")).thenReturn(Optional.of(lab));

        Optional<Lab> result = catalogService.findLabById("ddia-cap-03-lab-01");

        assertThat(result).contains(lab);
        verify(catalogRepository).findLabById("ddia-cap-03-lab-01");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para encontrar Lab por slug")
    void shouldFindLabBySlug() {
        Lab lab = new Lab("ddia-cap-03-lab-01", 1, "relacional-vs-documentos", "Relacional", "Sum", List.of(), EngineType.POSTGRES, "tbl_lab", "DROP TABLE...", List.of());
        when(catalogRepository.findLabById("relacional-vs-documentos")).thenReturn(Optional.of(lab));

        Optional<Lab> result = catalogService.findLabById("relacional-vs-documentos");

        assertThat(result).contains(lab);
        verify(catalogRepository).findLabById("relacional-vs-documentos");
    }
}
