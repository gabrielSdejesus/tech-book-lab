package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionIdTest {

    @Test
    @DisplayName("Deve criar SessionId válido quando fornecido UUID v4 canônico")
    void shouldCreateSessionIdWithValidUuidV4() {
        String rawUuid = UUID.randomUUID().toString();
        SessionId sessionId = SessionId.of(rawUuid);

        assertThat(sessionId.value()).isEqualTo(rawUuid.toLowerCase());
        assertThat(sessionId.toString()).isEqualTo(rawUuid.toLowerCase());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "not-a-uuid",
            "../../etc/passwd",
            "12345678-1234-1234-1234-1234567890123", // 37 chars
            "12345678-1234-1234-1234-12345678901",  // 35 chars
            "g2345678-1234-4234-8234-123456789012", // 'g' inválido no hexa
            "'; DROP TABLE labs; --",
            "12345678_1234_4234_8234_123456789012"
    })
    @DisplayName("Deve rejeitar formatos maliciosos ou inválidos com DomainValidationException")
    void shouldRejectInvalidSessionIds(String invalidValue) {
        assertThatThrownBy(() -> SessionId.of(invalidValue))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("X-Session-Id");
    }

    @Test
    @DisplayName("Deve rejeitar SessionId nulo")
    void shouldRejectNullSessionId() {
        assertThatThrownBy(() -> SessionId.of(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("X-Session-Id");
    }
}
