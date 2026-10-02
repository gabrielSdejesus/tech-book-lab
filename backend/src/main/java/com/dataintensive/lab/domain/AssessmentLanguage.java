package com.dataintensive.lab.domain;

public enum AssessmentLanguage {
    PT,
    EN;

    public static AssessmentLanguage from(String raw) {
        if (raw == null || raw.isBlank()) {
            return PT;
        }
        String clean = raw.trim().toLowerCase();
        if (clean.startsWith("en")) {
            return EN;
        }
        if (clean.startsWith("pt")) {
            return PT;
        }
        throw new IllegalArgumentException("Idioma '" + raw + "' não suportado. Idiomas válidos permitidos: 'pt', 'en'.");
    }
}
