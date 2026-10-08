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
        if (!isContainerRunning(engine)) {
            return false;
        }
        return isPortOpen(port);
    }

    public boolean isContainerRunning(EngineType engine) {
        String containerName = resolveContainerName(engine);
        List<String> command = List.of("docker", "inspect", "-f", "{{.State.Running}}", containerName);
        try {
            Process process = new ProcessBuilder(command).start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            if (process.exitValue() != 0) {
                return false;
            }
            String output = new String(process.getInputStream().readAllBytes()).trim();
            return "true".equalsIgnoreCase(output);
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
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
            } else {
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    String output = new String(process.getInputStream().readAllBytes());
                    log.warn("Comando docker retornou código {}: {}. Saída: {}", exitCode, command, output);
                }
            }
        } catch (Exception e) {
            log.warn("Aviso ao executar comando docker: {}. Erro: {}", command, e.getMessage());
        }
    }
}

