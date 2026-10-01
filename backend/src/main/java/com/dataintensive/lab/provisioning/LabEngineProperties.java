package com.dataintensive.lab.provisioning;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "lab.engines")
public class LabEngineProperties {

    public static class EngineConfig {
        private String serviceName;
        private int defaultPort;
        private String image;
        private List<String> ports = new ArrayList<>();
        private Map<String, String> env = new LinkedHashMap<>();

        public EngineConfig() {}

        public EngineConfig(String serviceName, int defaultPort, String image, List<String> ports, Map<String, String> env) {
            this.serviceName = serviceName;
            this.defaultPort = defaultPort;
            this.image = image;
            this.ports = ports;
            this.env = env;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public int getDefaultPort() {
            return defaultPort;
        }

        public void setDefaultPort(int defaultPort) {
            this.defaultPort = defaultPort;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public List<String> getPorts() {
            return ports;
        }

        public void setPorts(List<String> ports) {
            this.ports = ports;
        }

        public Map<String, String> getEnv() {
            return env;
        }

        public void setEnv(Map<String, String> env) {
            this.env = env;
        }
    }

    private Map<String, EngineConfig> configs = new LinkedHashMap<>();

    public LabEngineProperties() {
        configs.put("postgres", new EngineConfig(
                "postgres",
                5432,
                "postgres:16-alpine",
                List.of("0:5432"),
                Map.of(
                        "POSTGRES_USER", "postgres",
                        "POSTGRES_PASSWORD", "postgrespassword",
                        "POSTGRES_DB", "tbl_lab"
                )
        ));
        configs.put("neo4j", new EngineConfig(
                "neo4j",
                7687,
                "neo4j:5-community",
                List.of("0:7687", "0:7474"),
                Map.of("NEO4J_AUTH", "neo4j/tblpassword")
        ));
    }

    public Map<String, EngineConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, EngineConfig> configs) {
        this.configs = configs;
    }

    public EngineConfig getConfig(String engineName) {
        if (engineName == null) return null;
        EngineConfig cfg = configs.get(engineName.toLowerCase());
        if (cfg == null) {
            throw new IllegalArgumentException("Configuração do motor não encontrada para: " + engineName);
        }
        return cfg;
    }
}
