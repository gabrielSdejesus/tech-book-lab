package com.dataintensive.lab.domain;

import java.util.List;

public record ChapterQuiz(
    String chapterId,
    List<QuizQuestion> questions,
    int totalQuestions,
    int totalAnswered,
    int totalCorrect
) {}
