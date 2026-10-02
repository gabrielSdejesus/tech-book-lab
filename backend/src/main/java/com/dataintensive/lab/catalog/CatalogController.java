package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.Book;
import com.dataintensive.lab.domain.Lab;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/books")
    public List<Book> getAllBooks(
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        String locale = resolveLocale(langParam, acceptLanguage);
        return catalogService.getAllBooks(locale);
    }

    @GetMapping("/books/{bookId}")
    public ResponseEntity<Book> getBookById(
            @PathVariable String bookId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        String locale = resolveLocale(langParam, acceptLanguage);
        return catalogService.findBookById(bookId, locale)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/labs/{labId}")
    public ResponseEntity<Lab> getLabById(
            @PathVariable String labId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        String locale = resolveLocale(langParam, acceptLanguage);
        return catalogService.findLabById(labId, locale)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping({"/catalog/challenges/{challengeId}/solution", "/challenges/{challengeId}/solution"})
    public ResponseEntity<Void> saveChallengeSolution(
            @PathVariable String challengeId,
            @RequestBody SaveSolutionRequest request) {
        catalogService.saveChallengeSolution(challengeId, request != null ? request.code() : "");
        return ResponseEntity.ok().build();
    }

    @DeleteMapping({"/catalog/challenges/{challengeId}/solution", "/challenges/{challengeId}/solution"})
    public ResponseEntity<Void> deleteChallengeSolution(@PathVariable String challengeId) {
        catalogService.deleteChallengeSolution(challengeId);
        return ResponseEntity.ok().build();
    }

    public record SaveSolutionRequest(String code) {}

    static String resolveLocale(String langParam, String acceptLanguage) {
        if (langParam != null && !langParam.isBlank()) {
            String clean = langParam.trim().toLowerCase();
            if (clean.startsWith("en")) return "en";
            if (clean.startsWith("pt")) return "pt";
        }
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            String clean = acceptLanguage.trim().toLowerCase();
            int enIdx = clean.indexOf("en");
            int ptIdx = clean.indexOf("pt");
            if (enIdx >= 0 && (ptIdx < 0 || enIdx < ptIdx)) {
                return "en";
            }
            if (ptIdx >= 0) {
                return "pt";
            }
        }
        return "pt";
    }
}
