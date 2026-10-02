package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final LabEngineProperties DEFAULT_PROPERTIES = new LabEngineProperties();

    private final LabEngineProperties engineProperties;
    private final String composeFilePath;

    public DockerComposeLabManager() {
        this(DEFAULT_PROPERTIES);
    }

    @Autowired
    public DockerComposeLabManager(LabEngineProperties engineProperties) {
        this.engineProperties = engineProperties != null ? engineProperties : DEFAULT_PROPERTIES;
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

    public static String resolveContainerName(EngineType engine) {
        if (engine == null) {
            return "tbl-lab-unknown";
        }
        return "tbl-lab-" + engine.name().toLowerCase();
    }

    public static String resolveContainerName(SessionId sessionId, String labId, EngineType engine) {
        return resolveContainerName(engine);
    }

    public static List<String> buildRunCommand(String containerName, EngineType engine) {
        return buildRunCommand(containerName, engine, DEFAULT_PROPERTIES);
    }

    public static List<String> buildRunCommand(String containerName, EngineType engine, LabEngineProperties properties) {
        LabEngineProperties.EngineConfig config = properties.getConfig(engine.name());
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

        for (String p : config.getPorts()) {
            command.add("-p");
            command.add(p);
        }

        for (Map.Entry<String, String> e : config.getEnv().entrySet()) {
            command.add("-e");
            command.add(e.getKey() + "=" + e.getValue());
        }

        command.add(config.getImage());
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
        LabEngineProperties.EngineConfig config = engineProperties.getConfig(engine.name());
        int defaultPort = config.getDefaultPort();
        try {
            String inspectState = executeCommandAndCapture(List.of("docker", "inspect", "-f", "{{.State.Running}}", containerName)).trim();
            if ("true".equalsIgnoreCase(inspectState)) {
                log.info("Contêiner {} já está em execução. Obtendo porta alocada...", containerName);
                return resolveContainerPort(containerName, defaultPort, defaultPort);
            } else if ("false".equalsIgnoreCase(inspectState)) {
                log.info("Contêiner {} existe mas está parado. Iniciando...", containerName);
                executeCommand(List.of("docker", "start", containerName));
                return resolveContainerPort(containerName, defaultPort, defaultPort);
            }

            log.info("Provisionando novo contêiner isolado seguro {}", containerName);
            List<String> runCmd = buildRunCommand(containerName, engine, engineProperties);
            executeCommand(runCmd);
            return resolveContainerPort(containerName, defaultPort, defaultPort);
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
        LabEngineProperties.EngineConfig config = engineProperties.getConfig(engine.name());
        return config.getServiceName();
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
