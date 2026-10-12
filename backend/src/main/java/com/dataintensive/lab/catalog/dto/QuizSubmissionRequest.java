package com.dataintensive.lab.catalog.dto;

import java.util.Map;

public record QuizSubmissionRequest(
    Map<String, String> answers
) {}
