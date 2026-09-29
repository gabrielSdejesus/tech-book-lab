package com.dataintensive.lab.domain;

import java.util.List;

public record Book(
    String id,
    String title,
    String author,
    String tagLine,
    String coverColor,
    String description,
    List<Chapter> chapters
) {}
