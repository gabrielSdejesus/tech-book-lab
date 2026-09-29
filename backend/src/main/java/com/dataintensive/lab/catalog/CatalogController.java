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
    public List<Book> getAllBooks() {
        return catalogService.getAllBooks();
    }

    @GetMapping("/books/{bookId}")
    public ResponseEntity<Book> getBookById(@PathVariable String bookId) {
        return catalogService.findBookById(bookId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/labs/{labId}")
    public ResponseEntity<Lab> getLabById(@PathVariable String labId) {
        return catalogService.findLabById(labId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
