package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.Book;
import com.dataintensive.lab.domain.Lab;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CatalogService {

    private final CatalogRepository catalogRepository;

    public CatalogService(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    public List<Book> getAllBooks() {
        return catalogRepository.findAllBooks();
    }

    public Optional<Book> findBookById(String bookId) {
        return catalogRepository.findBookById(bookId);
    }

    public Optional<Lab> findLabById(String labId) {
        return catalogRepository.findLabById(labId);
    }
}
