package com.dataintensive.lab.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
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

    public static final String DEFAULT_POOL_NAME = "TblCatalogHikariPool";
    public static final int DEFAULT_MAX_POOL_SIZE = 10;
    public static final int DEFAULT_MIN_IDLE = 2;
    public static final long DEFAULT_CONNECTION_TIMEOUT_MS = 10000L;
    public static final long DEFAULT_IDLE_TIMEOUT_MS = 60000L;
    public static final long DEFAULT_MAX_LIFETIME_MS = 1800000L;
    public static final int DEFAULT_BUSY_TIMEOUT_MS = 5000;

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
    public static BeanPostProcessor sqliteHikariDataSourceBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof HikariDataSource hikariDataSource) {
                    String jdbcUrl = hikariDataSource.getJdbcUrl();
                    if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:sqlite:")) {
                        log.info("Aplicando pragmas de concorrência e resiliência ao HikariDataSource do SQLite...");
                        applySqlitePragmas(hikariDataSource);
                        if (hikariDataSource.getPoolName() == null || hikariDataSource.getPoolName().startsWith("HikariPool-")) {
                            hikariDataSource.setPoolName(DEFAULT_POOL_NAME);
                        }
                    }
                }
                return bean;
            }
        };
    }

    public static void applySqlitePragmas(HikariConfig config) {
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("busy_timeout", String.valueOf(DEFAULT_BUSY_TIMEOUT_MS));
        config.addDataSourceProperty("foreign_keys", "true");
    }

    public static HikariConfig createSqliteHikariConfig(String jdbcUrl) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(DEFAULT_POOL_NAME);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl(jdbcUrl);
        config.setMaximumPoolSize(DEFAULT_MAX_POOL_SIZE);
        config.setMinimumIdle(DEFAULT_MIN_IDLE);
        config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT_MS);
        config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT_MS);
        config.setMaxLifetime(DEFAULT_MAX_LIFETIME_MS);
        applySqlitePragmas(config);
        return config;
    }

    public static HikariDataSource createSqliteDataSource(String jdbcUrl) {
        return new HikariDataSource(createSqliteHikariConfig(jdbcUrl));
    }

    @Bean
    public Dialect jdbcDialect() {
        return AnsiDialect.INSTANCE;
    }
}

