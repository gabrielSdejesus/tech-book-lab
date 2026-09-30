package com.dataintensive.lab.domain;

public enum AssessmentLanguage {
    PT,
    EN;

    public static AssessmentLanguage from(String raw) {
        if (raw == null || raw.isBlank()) {
            return PT;
        }
        String clean = raw.trim().toLowerCase();
        if (clean.equals("pt") || clean.equals("pt-br") || clean.equals("pt_br")) {
            return PT;
        }
        if (clean.equals("en") || clean.equals("en-us") || clean.equals("en_us")) {
            return EN;
        }
        throw new IllegalArgumentException("Idioma '" + raw + "' não suportado. Idiomas válidos permitidos: 'pt', 'en'.");
    }
}
