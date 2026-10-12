package com.dataintensive.lab.domain;

public record QuizOption(
    String id,
    String questionId,
    String optionLetter,
    String text,
    boolean isCorrect
) {}
