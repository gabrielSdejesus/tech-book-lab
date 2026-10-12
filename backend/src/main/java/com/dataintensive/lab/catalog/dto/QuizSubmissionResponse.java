package com.dataintensive.lab.catalog.dto;

import java.util.List;

public record QuizSubmissionResponse(
    String chapterId,
    int totalQuestions,
    int correctAnswers,
    double scorePercentage,
    List<QuizQuestionResult> results
) {}
