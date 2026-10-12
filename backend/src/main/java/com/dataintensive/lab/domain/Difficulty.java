package com.dataintensive.lab.domain;

public enum Difficulty {
    EASY,
    MEDIUM,
    HARD;

    public static Difficulty fromString(String val) {
        if (val == null) return MEDIUM;
        try {
            return Difficulty.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
