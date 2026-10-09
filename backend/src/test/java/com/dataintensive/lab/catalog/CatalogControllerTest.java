package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.Book;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CatalogControllerTest {

    @Test
    @DisplayName("resolveLocale deve priorizar langParam sobre Accept-Language")
    void shouldPrioritizeLangParamOverAcceptLanguage() {
        assertThat(CatalogController.resolveLocale("en", "pt-BR,pt;q=0.9")).isEqualTo("en");
        assertThat(CatalogController.resolveLocale("pt-BR", "en-US,en;q=0.9")).isEqualTo("pt");
    }

    @Test
    @DisplayName("resolveLocale deve resolver idioma a partir de Accept-Language quando langParam for ausente ou inválido")
    void shouldResolveLocaleFromAcceptLanguage() {
        assertThat(CatalogController.resolveLocale(null, "en-US,en;q=0.9")).isEqualTo("en");
        assertThat(CatalogController.resolveLocale("", "pt-BR,pt;q=0.9,en;q=0.8")).isEqualTo("pt");
        assertThat(CatalogController.resolveLocale("es", "en-GB,en;q=0.5")).isEqualTo("en");
    }

    @Test
    @DisplayName("resolveLocale deve retornar 'pt' como fallback seguro quando nenhum idioma suportado for detectado")
    void shouldFallbackToPortuguese() {
        assertThat(CatalogController.resolveLocale(null, null)).isEqualTo("pt");
        assertThat(CatalogController.resolveLocale("   ", "   ")).isEqualTo("pt");
        assertThat(CatalogController.resolveLocale("fr", "de-DE,de;q=0.9,ja;q=0.8")).isEqualTo("pt");
    }

    @Test
    @DisplayName("getAllBooks deve delegar ao CatalogService com locale resolvido")
    void shouldDelegateGetAllBooksWithResolvedLocale() {
        CatalogService mockService = mock(CatalogService.class);
        CatalogController controller = new CatalogController(mockService);

        Book mockBook = new Book("ddia", "DDIA", "Martin", "Tag", "#000", "/cover.svg", "Desc", List.of());
        when(mockService.getAllBooks("en")).thenReturn(List.of(mockBook));

        List<Book> books = controller.getAllBooks("en-US", "en");

        assertThat(books).hasSize(1);
        assertThat(books.get(0).id()).isEqualTo("ddia");
        verify(mockService).getAllBooks("en");
    }

    @Test
    @DisplayName("getBookById deve retornar 200 OK quando livro existir e 404 quando não existir")
    void shouldHandleGetBookById() {
        CatalogService mockService = mock(CatalogService.class);
        CatalogController controller = new CatalogController(mockService);

        Book mockBook = new Book("ddia", "DDIA", "Martin", "Tag", "#000", "/cover.svg", "Desc", List.of());
        when(mockService.findBookById("ddia", "pt")).thenReturn(Optional.of(mockBook));
        when(mockService.findBookById("inexistente", "pt")).thenReturn(Optional.empty());

        ResponseEntity<Book> found = controller.getBookById("ddia", null, "pt");
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody()).isNotNull();
        assertThat(found.getBody().id()).isEqualTo("ddia");

        ResponseEntity<Book> notFound = controller.getBookById("inexistente", null, null);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("getLabById deve retornar 200 OK quando lab existir e 404 quando não existir")
    void shouldHandleGetLabById() {
        CatalogService mockService = mock(CatalogService.class);
        CatalogController controller = new CatalogController(mockService);

        Lab mockLab = new Lab("lab-1", 1, "slug", "Lab 1", "Sum", List.of(), EngineType.POSTGRES, "db", "SQL", List.of());
        when(mockService.findLabById("lab-1", "en")).thenReturn(Optional.of(mockLab));
        when(mockService.findLabById("unknown", "en")).thenReturn(Optional.empty());

        ResponseEntity<Lab> found = controller.getLabById("lab-1", "en", null);
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody()).isNotNull();
        assertThat(found.getBody().id()).isEqualTo("lab-1");

        ResponseEntity<Lab> notFound = controller.getLabById("unknown", "en", null);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("saveChallengeSolution deve retornar 200 OK e delegar ao CatalogService")
    void shouldSaveChallengeSolution() {
        CatalogService mockService = mock(CatalogService.class);
        CatalogController controller = new CatalogController(mockService);

        ResponseEntity<Void> response = controller.saveChallengeSolution(
                "lab-01-ch-1",
                new CatalogController.SaveSolutionRequest("SELECT 1;")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(mockService).saveChallengeSolution("lab-01-ch-1", "SELECT 1;", null);
    }

    @Test
    @DisplayName("saveChallengeSolution com userReflection deve retornar 200 OK e delegar ao CatalogService")
    void shouldSaveChallengeSolutionWithReflection() {
        CatalogService mockService = mock(CatalogService.class);
        CatalogController controller = new CatalogController(mockService);

        ResponseEntity<Void> response = controller.saveChallengeSolution(
                "lab-01-ch-1",
                new CatalogController.SaveSolutionRequest("SELECT 1;", "Reflexão sobre 3NF")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(mockService).saveChallengeSolution("lab-01-ch-1", "SELECT 1;", "Reflexão sobre 3NF");
    }

    @Test
    @DisplayName("deleteChallengeSolution deve retornar 200 OK e delegar ao CatalogService")
    void shouldDeleteChallengeSolution() {
        CatalogService mockService = mock(CatalogService.class);
        CatalogController controller = new CatalogController(mockService);

        ResponseEntity<Void> response = controller.deleteChallengeSolution("lab-01-ch-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(mockService).deleteChallengeSolution("lab-01-ch-1");
    }
}

