package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.AssessmentLanguage;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiAssessmentController {

    private final AiAssessmentService aiAssessmentService;

    public AiAssessmentController(AiAssessmentService aiAssessmentService) {
        this.aiAssessmentService = aiAssessmentService;
    }

    @PostMapping("/test-connection")
    public ResponseEntity<AiTestConnectionResponse> testConnection(@Valid @RequestBody AiTestConnectionRequest request) {
        AiTestConnectionResponse response = aiAssessmentService.testConnection(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assess")
    public ResponseEntity<?> assess(@Valid @RequestBody AiAssessmentRequest request) {
        try {
            AssessmentLanguage.from(request.language());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "BAD_REQUEST",
                    "message", e.getMessage()
            ));
        }

        AiAssessmentResponse response = aiAssessmentService.assess(request);
        return ResponseEntity.ok(response);
    }
}

