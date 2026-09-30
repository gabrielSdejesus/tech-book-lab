package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class DockerComposeLabManager implements LabContainerManager {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeLabManager.class);

    private static final Map<EngineType, String> SERVICE_MAPPING = Map.of(
            EngineType.POSTGRES, "postgres",
            EngineType.NEO4J, "neo4j"
    );

    private final String composeFilePath;

    public DockerComposeLabManager() {
        // Resolve compose file relative to project root
        Path directPath = Path.of("infra", "docker-compose.yml");
        Path parentPath = Path.of("..", "infra", "docker-compose.yml");
        if (Files.exists(directPath)) {
            this.composeFilePath = directPath.toString();
        } else if (Files.exists(parentPath)) {
            this.composeFilePath = parentPath.toString();
        } else {
            this.composeFilePath = "infra/docker-compose.yml";
        }
    }

    @Override
    public void startEngine(EngineType engine) {
        String service = resolveServiceName(engine);
        log.info("Provisionando contêiner sob demanda para motor {} (serviço: {})...", engine, service);
        executeComposeCommand(List.of("up", "-d", service));
    }

    @Override
    public void stopEngine(EngineType engine) {
        String service = resolveServiceName(engine);
        log.info("Encerrando contêiner de laboratório para motor {} (serviço: {})...", engine, service);
        executeComposeCommand(List.of("stop", service));
    }

    @Override
    public boolean isEngineHealthy(EngineType engine, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String resolveServiceName(EngineType engine) {
        String service = SERVICE_MAPPING.get(engine);
        if (service == null) {
            throw new IllegalArgumentException("Motor não suportado para provisionamento: " + engine);
        }
        return service;
    }

    private void executeComposeCommand(List<String> subArgs) {
        List<String> command = new java.util.ArrayList<>();
        command.add("docker");
        command.add("compose");
        command.add("-f");
        command.add(composeFilePath);
        command.addAll(subArgs);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Comando docker compose atingiu timeout de 30s: {}", command);
            }
        } catch (Exception e) {
            log.warn("Aviso ao executar comando docker compose: {}. Erro: {}", command, e.getMessage());
        }
    }
}
