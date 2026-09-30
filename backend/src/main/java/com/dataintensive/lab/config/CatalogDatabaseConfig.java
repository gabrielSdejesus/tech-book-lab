package com.dataintensive.lab.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class CatalogDatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(CatalogDatabaseConfig.class);

    @PostConstruct
    public void ensureDataDirectoryExists() {
        Path dataDir = Path.of("data");
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
                log.info("Diretório 'data/' criado com sucesso para o banco SQLite do catálogo.");
            } catch (IOException e) {
                log.warn("Não foi possível criar o diretório 'data/': {}", e.getMessage());
            }
        }
    }
}
