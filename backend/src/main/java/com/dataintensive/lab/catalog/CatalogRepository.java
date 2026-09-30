package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.Book;
import com.dataintensive.lab.domain.Lab;

import java.util.List;
import java.util.Optional;

public interface CatalogRepository {
    List<Book> findAllBooks();
    Optional<Book> findBookById(String bookId);
    Optional<Lab> findLabById(String labIdOrSlug);
}
