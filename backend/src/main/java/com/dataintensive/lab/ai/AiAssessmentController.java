package com.dataintensive.lab.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiAssessmentController {

    private final AiAssessmentService aiAssessmentService;

    public AiAssessmentController(AiAssessmentService aiAssessmentService) {
        this.aiAssessmentService = aiAssessmentService;
    }

    @PostMapping("/test-connection")
    public ResponseEntity<AiTestConnectionResponse> testConnection(@RequestBody AiTestConnectionRequest request) {
        AiTestConnectionResponse response = aiAssessmentService.testConnection(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assess")
    public ResponseEntity<AiAssessmentResponse> assess(@RequestBody AiAssessmentRequest request) {
        AiAssessmentResponse response = aiAssessmentService.assess(request);
        return ResponseEntity.ok(response);
    }
}
