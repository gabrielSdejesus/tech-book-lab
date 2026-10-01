package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.provisioning.dto.EngineHealthStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class InfraStatusService {

    private final LabEngineProperties engineProperties;

    public InfraStatusService(LabEngineProperties engineProperties) {
        this.engineProperties = engineProperties;
    }

    public Map<String, EngineHealthStatus> getInfraStatus() {
        Map<String, EngineHealthStatus> statuses = new LinkedHashMap<>();
        for (Map.Entry<String, LabEngineProperties.EngineConfig> entry : engineProperties.getConfigs().entrySet()) {
            String engineName = entry.getKey();
            LabEngineProperties.EngineConfig config = entry.getValue();
            boolean healthy = checkPortHealth(config.getDefaultPort());
            statuses.put(engineName, new EngineHealthStatus(
                    healthy,
                    config.getDefaultPort(),
                    config.getServiceName(),
                    healthy ? "UP" : "DOWN"
            ));
        }
        return statuses;
    }

    private boolean checkPortHealth(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 200);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
