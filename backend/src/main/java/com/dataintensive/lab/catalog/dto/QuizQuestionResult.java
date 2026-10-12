package com.dataintensive.lab.catalog.dto;

public record QuizQuestionResult(
    String questionId,
    String selectedOptionId,
    String correctOptionId,
    boolean isCorrect,
    String explanation
) {}
