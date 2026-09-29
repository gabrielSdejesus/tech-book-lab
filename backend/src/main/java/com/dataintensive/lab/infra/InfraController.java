package com.dataintensive.lab.infra;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/infra")
@CrossOrigin(origins = "*")
public class InfraController {

    private final InfraService infraService;

    public InfraController(InfraService infraService) {
        this.infraService = infraService;
    }

    @GetMapping("/status")
    public ResponseEntity<InfraStatus> getStatus() {
        return ResponseEntity.ok(infraService.checkStatus());
    }
}
