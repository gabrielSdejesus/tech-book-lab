package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.DomainValidationException;
import com.dataintensive.lab.provisioning.dto.LabHeartbeatResponse;
import com.dataintensive.lab.provisioning.dto.LabProvisionResponse;
import com.dataintensive.lab.provisioning.dto.LabStatusResponse;
import com.dataintensive.lab.provisioning.dto.LabTeardownResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/lab")
public class LabProvisioningController {

    private static final Pattern LAB_ID_PATTERN = Pattern.compile("^[a-z0-9-]+$");

    private final LabProvisioningService provisioningService;
    private final LabProvisioningProperties provisioningProperties;

    public LabProvisioningController(LabProvisioningService provisioningService, LabProvisioningProperties provisioningProperties) {
        this.provisioningService = provisioningService;
        this.provisioningProperties = provisioningProperties;
    }

    @PostMapping("/{labId}/provision")
    public ResponseEntity<LabProvisionResponse> provisionLab(
            @PathVariable String labId,
            @RequestParam(required = false) String challengeId,
            @RequestHeader(value = "X-Session-Id", required = false) String rawSessionId) {

        SessionId sessionId = SessionId.of(rawSessionId);
        validateLabId(labId);

        LabSession session = provisioningService.provisionLab(sessionId, labId, challengeId);

        LabProvisionResponse response = new LabProvisionResponse(
                session.labId(),
                session.challengeId(),
                session.containerName(),
                session.engineType(),
                session.status(),
                session.status() == LabEnvironmentStatus.READY
                        ? "Ambiente de laboratório já está ativo e pronto para uso."
                        : "Inicializando contêiner sob demanda...",
                session.allocatedPort(),
                session.status() == LabEnvironmentStatus.READY ? 0 : 5,
                provisioningProperties != null ? provisioningProperties.getHeartbeatIntervalSeconds() : 30
        );

        HttpStatus httpStatus = session.status() == LabEnvironmentStatus.READY ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(httpStatus).body(response);
    }

    @GetMapping("/{labId}/status")
    public ResponseEntity<LabStatusResponse> getLabStatus(
            @PathVariable String labId,
            @RequestParam(required = false) String challengeId,
            @RequestHeader(value = "X-Session-Id", required = false) String rawSessionId) {

        SessionId sessionId = SessionId.of(rawSessionId);
        validateLabId(labId);

        LabSession session = provisioningService.getLabStatus(sessionId, labId, challengeId);
        long uptime = Duration.between(session.lastHeartbeatAt(), Instant.now()).toSeconds();

        LabStatusResponse response = new LabStatusResponse(
                session.labId(),
                session.challengeId(),
                session.containerName(),
                session.engineType(),
                session.status(),
                session.allocatedPort(),
                Math.max(0, uptime),
                session.lastHeartbeatAt().toEpochMilli(),
                session.errorMessage()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{labId}/heartbeat")
    public ResponseEntity<LabHeartbeatResponse> heartbeat(
            @PathVariable String labId,
            @RequestHeader(value = "X-Session-Id", required = false) String rawSessionId) {

        SessionId sessionId = SessionId.of(rawSessionId);
        validateLabId(labId);

        LabHeartbeatResult result = provisioningService.heartbeat(sessionId, labId);

        LabHeartbeatResponse response = new LabHeartbeatResponse(
                result.status(),
                result.labId(),
                result.ttlRemainingSeconds(),
                result.lastHeartbeatAt().toEpochMilli()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{labId}/teardown")
    public ResponseEntity<LabTeardownResponse> teardown(
            @PathVariable String labId,
            @RequestHeader(value = "X-Session-Id", required = false) String rawSessionId) {

        SessionId sessionId = SessionId.of(rawSessionId);
        validateLabId(labId);

        LabSession session = provisioningService.teardown(sessionId, labId);

        LabTeardownResponse response = new LabTeardownResponse(
                session.labId(),
                session.status(),
                "Ambiente isolado de laboratório encerrado com sucesso. Recursos liberados."
        );

        return ResponseEntity.ok(response);
    }

    private void validateLabId(String labId) {
        if (labId == null || labId.isBlank()) {
            throw new DomainValidationException("O campo 'labId' não pode ser vazio.");
        }
        String trimmed = labId.trim();
        if (trimmed.length() < 3 || trimmed.length() > 60 || !LAB_ID_PATTERN.matcher(trimmed).matches()) {
            throw new DomainValidationException("O campo 'labId' possui formato inválido. Deve conter apenas caracteres minúsculos alfanuméricos e hífens, entre 3 e 60 caracteres.");
        }
    }
}
