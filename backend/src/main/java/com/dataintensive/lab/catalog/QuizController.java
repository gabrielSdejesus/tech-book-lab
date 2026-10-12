package com.dataintensive.lab.catalog;

import com.dataintensive.lab.catalog.dto.QuizSubmissionRequest;
import com.dataintensive.lab.catalog.dto.QuizSubmissionResponse;
import com.dataintensive.lab.domain.ChapterQuiz;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class QuizController {

    private final CatalogService catalogService;

    public QuizController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping({"/chapters/{chapterId}/quiz", "/books/{bookId}/chapters/{chapterId}/quiz"})
    public ResponseEntity<ChapterQuiz> getChapterQuiz(
            @PathVariable String chapterId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        String locale = CatalogController.resolveLocale(langParam, acceptLanguage);
        return catalogService.getChapterQuiz(chapterId, locale)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/books/{bookId}/chapters/{chapterId}/quiz")
    public ResponseEntity<ChapterQuiz> getBookChapterQuiz(
            @PathVariable String bookId,
            @PathVariable String chapterId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        return getChapterQuiz(chapterId, acceptLanguage, langParam);
    }

    @PostMapping("/chapters/{chapterId}/quiz/submit")
    public ResponseEntity<QuizSubmissionResponse> submitQuiz(
            @PathVariable String chapterId,
            @RequestBody QuizSubmissionRequest request) {
        QuizSubmissionResponse response = catalogService.submitChapterQuiz(chapterId, request);
        return ResponseEntity.ok(response);
    }
}
