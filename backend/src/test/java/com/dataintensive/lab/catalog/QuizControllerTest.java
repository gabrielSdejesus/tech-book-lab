package com.dataintensive.lab.catalog;

import com.dataintensive.lab.catalog.dto.QuizQuestionResult;
import com.dataintensive.lab.catalog.dto.QuizSubmissionRequest;
import com.dataintensive.lab.catalog.dto.QuizSubmissionResponse;
import com.dataintensive.lab.domain.ChapterQuiz;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class QuizControllerTest {

    private CatalogService catalogService;
    private QuizController controller;

    @BeforeEach
    void setUp() {
        catalogService = mock(CatalogService.class);
        controller = new QuizController(catalogService);
    }

    @Test
    @DisplayName("GET /api/chapters/{chapterId}/quiz deve retornar 200 OK quando quiz existir")
    void shouldReturnChapterQuizWhenFound() {
        var quiz = new ChapterQuiz("ddia-cap-03", List.of(), 6, 2, 2);
        when(catalogService.getChapterQuiz("ddia-cap-03", "pt")).thenReturn(Optional.of(quiz));

        ResponseEntity<ChapterQuiz> response = controller.getChapterQuiz("ddia-cap-03", null, "pt");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(quiz);
        verify(catalogService).getChapterQuiz("ddia-cap-03", "pt");
    }

    @Test
    @DisplayName("GET /api/books/{bookId}/chapters/{chapterId}/quiz deve delegar igualmente para getChapterQuiz")
    void shouldReturnChapterQuizWithBookPath() {
        var quiz = new ChapterQuiz("ddia-cap-03", List.of(), 6, 2, 2);
        when(catalogService.getChapterQuiz("ddia-cap-03", "en")).thenReturn(Optional.of(quiz));

        ResponseEntity<ChapterQuiz> response = controller.getBookChapterQuiz("ddia", "ddia-cap-03", null, "en");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(quiz);
        verify(catalogService).getChapterQuiz("ddia-cap-03", "en");
    }

    @Test
    @DisplayName("GET /api/chapters/{chapterId}/quiz deve retornar 404 Not Found quando quiz não existir")
    void shouldReturn404WhenQuizNotFound() {
        when(catalogService.getChapterQuiz("desconhecido", "pt")).thenReturn(Optional.empty());

        ResponseEntity<ChapterQuiz> response = controller.getChapterQuiz("desconhecido", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("POST /api/chapters/{chapterId}/quiz/submit deve submeter respostas e retornar resultado com score")
    void shouldSubmitQuizAndReturnResult() {
        var request = new QuizSubmissionRequest(Map.of("q-1", "opt-1"));
        var result = new QuizQuestionResult("q-1", "opt-1", "opt-1", true, "Gabarito A");
        var submissionResponse = new QuizSubmissionResponse("ddia-cap-03", 1, 1, 100.0, List.of(result));

        when(catalogService.submitChapterQuiz("ddia-cap-03", request)).thenReturn(submissionResponse);

        ResponseEntity<QuizSubmissionResponse> response = controller.submitQuiz("ddia-cap-03", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(submissionResponse);
        verify(catalogService).submitChapterQuiz("ddia-cap-03", request);
    }
}
