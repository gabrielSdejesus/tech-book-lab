package com.dataintensive.lab.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.relational.core.dialect.AnsiDialect;
import org.springframework.data.relational.core.dialect.Dialect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class CatalogDatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(CatalogDatabaseConfig.class);

    @Bean
    public static BeanFactoryPostProcessor ensureDataDirBeanFactoryPostProcessor() {
        return beanFactory -> {
            try {
                Files.createDirectories(Path.of("data"));
                Files.createDirectories(Path.of("backend", "data"));
                log.info("Diretório 'data/' assegurado para inicialização do SQLite.");
            } catch (IOException e) {
                log.warn("Aviso ao assegurar diretório 'data/': {}", e.getMessage());
            }
        };
    }

    @Bean
    public Dialect jdbcDialect() {
        return AnsiDialect.INSTANCE;
    }
}
