package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.provisioning.dto.EngineHealthStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/infra")
@CrossOrigin(origins = "*")
public class InfraStatusController {

    private final InfraStatusService infraStatusService;

    public InfraStatusController(InfraStatusService infraStatusService) {
        this.infraStatusService = infraStatusService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, EngineHealthStatus>> getStatus() {
        return ResponseEntity.ok(infraStatusService.getInfraStatus());
    }
}
