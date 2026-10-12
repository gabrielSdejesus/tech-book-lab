package com.dataintensive.lab.domain;

import java.util.List;

public record QuizQuestion(
    String id,
    String chapterId,
    int orderIndex,
    Difficulty difficulty,
    String prompt,
    String explanation,
    List<QuizOption> options,
    String selectedOptionId,
    Boolean isAnsweredCorrectly
) {}
