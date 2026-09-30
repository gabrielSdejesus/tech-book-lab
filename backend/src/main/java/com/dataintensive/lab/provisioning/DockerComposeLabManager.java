package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    public static String resolveContainerName(SessionId sessionId, String labId, EngineType engine) {
        String rawSession = (sessionId != null && sessionId.value() != null) ? sessionId.value() : "default";
        String userHash = rawSession.replaceAll("-", "").toLowerCase();
        if (userHash.length() > 8) {
            userHash = userHash.substring(0, 8);
        }
        String cleanLab = (labId != null ? labId : "lab").replaceAll("[^a-zA-Z0-9]", "-").toLowerCase();
        String engineSuffix = engine != null ? "-" + engine.name().toLowerCase() : "";
        return "user-" + userHash + "-" + cleanLab + engineSuffix;
    }

    public static List<String> buildRunCommand(String containerName, EngineType engine) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("-d");
        command.add("--name");
        command.add(containerName);
        command.add("--security-opt=no-new-privileges:true");
        command.add("--cap-drop=ALL");
        command.add("--cap-add=CHOWN");
        command.add("--cap-add=SETUID");
        command.add("--cap-add=SETGID");
        command.add("--cap-add=DAC_OVERRIDE");
        command.add("--pids-limit=100");
        command.add("--memory=512m");

        if (engine == EngineType.POSTGRES) {
            command.add("-p");
            command.add("0:5432");
            command.add("-e");
            command.add("POSTGRES_USER=postgres");
            command.add("-e");
            command.add("POSTGRES_PASSWORD=postgrespassword");
            command.add("-e");
            command.add("POSTGRES_DB=tbl_lab");
            command.add("postgres:16-alpine");
        } else if (engine == EngineType.NEO4J) {
            command.add("-p");
            command.add("0:7687");
            command.add("-p");
            command.add("0:7474");
            command.add("-e");
            command.add("NEO4J_AUTH=neo4j/tblpassword");
            command.add("neo4j:5-community");
        } else {
            throw new IllegalArgumentException("Motor não suportado: " + engine);
        }

        return command;
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
    public int startIsolatedContainer(String containerName, EngineType engine) {
        int defaultPort = (engine == EngineType.POSTGRES) ? 5432 : 7687;
        try {
            String inspectState = executeCommandAndCapture(List.of("docker", "inspect", "-f", "{{.State.Running}}", containerName)).trim();
            if ("true".equalsIgnoreCase(inspectState)) {
                log.info("Contêiner {} já está em execução. Obtendo porta alocada...", containerName);
                return resolveContainerPort(containerName, (engine == EngineType.POSTGRES) ? 5432 : 7687, defaultPort);
            } else if ("false".equalsIgnoreCase(inspectState)) {
                log.info("Contêiner {} existe mas está parado. Iniciando...", containerName);
                executeCommand(List.of("docker", "start", containerName));
                return resolveContainerPort(containerName, (engine == EngineType.POSTGRES) ? 5432 : 7687, defaultPort);
            }

            log.info("Provisionando novo contêiner isolado seguro {}", containerName);
            List<String> runCmd = buildRunCommand(containerName, engine);
            executeCommand(runCmd);
            return resolveContainerPort(containerName, (engine == EngineType.POSTGRES) ? 5432 : 7687, defaultPort);
        } catch (Exception e) {
            log.warn("Falha ao gerenciar contêiner isolado via docker cli (utilizando porta padrão {}): {}", defaultPort, e.getMessage());
            return defaultPort;
        }
    }

    @Override
    public void stopIsolatedContainer(String containerName) {
        try {
            log.info("Encerrando contêiner isolado {}...", containerName);
            executeCommand(List.of("docker", "stop", containerName));
        } catch (Exception e) {
            log.warn("Aviso ao parar contêiner isolado {}: {}", containerName, e.getMessage());
        }
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

    private int resolveContainerPort(String containerName, int internalPort, int defaultPort) {
        try {
            String output = executeCommandAndCapture(List.of("docker", "port", containerName, String.valueOf(internalPort)));
            if (output != null && !output.isBlank()) {
                String[] lines = output.split("\\r?\\n");
                for (String line : lines) {
                    int lastColon = line.lastIndexOf(':');
                    if (lastColon >= 0 && lastColon < line.length() - 1) {
                        String portStr = line.substring(lastColon + 1).trim();
                        return Integer.parseInt(portStr);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Não foi possível mapear porta dinâmica para {} (usando {}): {}", containerName, defaultPort, e.getMessage());
        }
        return defaultPort;
    }

    private String resolveServiceName(EngineType engine) {
        String service = SERVICE_MAPPING.get(engine);
        if (service == null) {
            throw new IllegalArgumentException("Motor não suportado para provisionamento: " + engine);
        }
        return service;
    }

    private void executeComposeCommand(List<String> subArgs) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("compose");
        command.add("-f");
        command.add(composeFilePath);
        command.addAll(subArgs);
        executeCommand(command);
    }

    private void executeCommand(List<String> command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Comando docker atingiu timeout de 30s: {}", command);
            }
        } catch (Exception e) {
            log.warn("Aviso ao executar comando docker: {}. Erro: {}", command, e.getMessage());
        }
    }

    private String executeCommandAndCapture(List<String> command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }
            return output;
        } catch (Exception e) {
            return "";
        }
    }
}
