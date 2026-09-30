package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.DomainValidationException;

import java.util.regex.Pattern;

public record SessionId(String value) {

    private static final Pattern UUID_V4_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

    public static SessionId of(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new DomainValidationException("O header 'X-Session-Id' é obrigatório e deve ser um UUID v4 canônico válido de 36 caracteres.");
        }
        String trimmed = rawValue.trim();
        if (trimmed.length() != 36 || !UUID_V4_PATTERN.matcher(trimmed).matches()) {
            throw new DomainValidationException("O header 'X-Session-Id' possui formato inválido. Deve ser um UUID v4 canônico válido de 36 caracteres.");
        }
        return new SessionId(trimmed.toLowerCase());
    }

    @Override
    public String toString() {
        return value;
    }
}
