package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.Book;
import com.dataintensive.lab.domain.Lab;

import java.util.List;
import java.util.Optional;

public interface CatalogRepository {
    List<Book> findAllBooks();
    default List<Book> findAllBooks(String locale) {
        return findAllBooks();
    }

    Optional<Book> findBookById(String bookId);
    default Optional<Book> findBookById(String bookId, String locale) {
        return findBookById(bookId);
    }

    Optional<Lab> findLabById(String labIdOrSlug);
    default Optional<Lab> findLabById(String labIdOrSlug, String locale) {
        return findLabById(labIdOrSlug);
    }

    void saveChallengeSolution(String challengeId, String code);

    void deleteChallengeSolution(String challengeId);
}
