package com.dataintensive.lab.provisioning;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "lab.engines")
public class LabEngineProperties {

    public static class EngineConfig {
        private String serviceName;
        private int defaultPort;
        private String containerName;

        public EngineConfig() {}

        public EngineConfig(String serviceName, int defaultPort) {
            this(serviceName, defaultPort, "tbl-lab-" + serviceName);
        }

        public EngineConfig(String serviceName, int defaultPort, String containerName) {
            this.serviceName = serviceName;
            this.defaultPort = defaultPort;
            this.containerName = containerName;
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

        public String getContainerName() {
            return containerName != null ? containerName : (serviceName != null ? "tbl-lab-" + serviceName : "tbl-lab-unknown");
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }
    }

    private Map<String, EngineConfig> configs = new LinkedHashMap<>();

    public LabEngineProperties() {
        configs.put("postgres", new EngineConfig("postgres", 5432, "tbl-lab-postgres"));
        configs.put("neo4j", new EngineConfig("neo4j", 7687, "tbl-lab-neo4j"));
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
