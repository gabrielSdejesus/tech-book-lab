package com.dataintensive.lab.domain;

import java.util.List;

public record Chapter(
    String id,
    int number,
    String title,
    String subtitle,
    String summary,
    List<Lab> labs
) {}
