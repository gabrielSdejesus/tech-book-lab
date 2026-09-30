package com.dataintensive.lab.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Fábrica utilitária para testes de integração e concorrência do SQLite,
 * replicando os padrões declarados no application.yml através de variáveis
 * de ambiente e seus respectivos valores default.
 */
public final class SqliteTestDataSourceFactory {

    public static final String DEFAULT_POOL_NAME = "TblCatalogHikariPool";
    public static final String DEFAULT_DRIVER = "org.sqlite.JDBC";
    public static final int DEFAULT_MAX_POOL_SIZE = 10;
    public static final int DEFAULT_MIN_IDLE = 2;
    public static final long DEFAULT_CONNECTION_TIMEOUT = 10000L;
    public static final long DEFAULT_IDLE_TIMEOUT = 60000L;
    public static final long DEFAULT_MAX_LIFETIME = 1800000L;
    public static final String DEFAULT_JOURNAL_MODE = "WAL";
    public static final String DEFAULT_SYNCHRONOUS = "NORMAL";
    public static final int DEFAULT_BUSY_TIMEOUT = 5000;
    public static final String DEFAULT_FOREIGN_KEYS = "true";

    private SqliteTestDataSourceFactory() {}

    public static HikariConfig createConfig(String jdbcUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName(getEnvOrDefault("CATALOG_DB_DRIVER", DEFAULT_DRIVER));
        config.setPoolName(getEnvOrDefault("CATALOG_DB_POOL_NAME", DEFAULT_POOL_NAME));
        config.setMaximumPoolSize(Integer.parseInt(getEnvOrDefault("CATALOG_DB_MAX_POOL_SIZE", String.valueOf(DEFAULT_MAX_POOL_SIZE))));
        config.setMinimumIdle(Integer.parseInt(getEnvOrDefault("CATALOG_DB_MIN_IDLE", String.valueOf(DEFAULT_MIN_IDLE))));
        config.setConnectionTimeout(Long.parseLong(getEnvOrDefault("CATALOG_DB_CONNECTION_TIMEOUT", String.valueOf(DEFAULT_CONNECTION_TIMEOUT))));
        config.setIdleTimeout(Long.parseLong(getEnvOrDefault("CATALOG_DB_IDLE_TIMEOUT", String.valueOf(DEFAULT_IDLE_TIMEOUT))));
        config.setMaxLifetime(Long.parseLong(getEnvOrDefault("CATALOG_DB_MAX_LIFETIME", String.valueOf(DEFAULT_MAX_LIFETIME))));

        config.addDataSourceProperty("journal_mode", getEnvOrDefault("CATALOG_DB_JOURNAL_MODE", DEFAULT_JOURNAL_MODE));
        config.addDataSourceProperty("synchronous", getEnvOrDefault("CATALOG_DB_SYNCHRONOUS", DEFAULT_SYNCHRONOUS));
        config.addDataSourceProperty("busy_timeout", getEnvOrDefault("CATALOG_DB_BUSY_TIMEOUT", String.valueOf(DEFAULT_BUSY_TIMEOUT)));
        config.addDataSourceProperty("foreign_keys", getEnvOrDefault("CATALOG_DB_FOREIGN_KEYS", DEFAULT_FOREIGN_KEYS));
        return config;
    }

    public static HikariDataSource createDataSource(String jdbcUrl) {
        return new HikariDataSource(createConfig(jdbcUrl));
    }

    public static String getEnvOrDefault(String key, String defaultValue) {
        String env = System.getenv(key);
        return (env != null && !env.isBlank()) ? env : defaultValue;
    }
}
