package com.dataintensive.lab.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    static class TestCorsRegistry extends CorsRegistry {
        public Map<String, CorsConfiguration> exposedConfigurations() {
            return super.getCorsConfigurations();
        }
    }

    @Test
    @DisplayName("Deve configurar mapeamentos de CORS para /api/** com origens, métodos e cabeçalhos permitidos")
    void shouldConfigureCorsMappingsCorrectly() {
        CorsConfig corsConfig = new CorsConfig();
        TestCorsRegistry registry = new TestCorsRegistry();

        corsConfig.addCorsMappings(registry);

        Map<String, CorsConfiguration> configurations = registry.exposedConfigurations();

        assertThat(configurations).containsKey("/api/**");
        CorsConfiguration config = configurations.get("/api/**");
        assertThat(config.getAllowedOrigins()).containsExactly("*");
        assertThat(config.getAllowedMethods()).containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(config.getAllowedHeaders()).containsExactly("*");
    }
}
