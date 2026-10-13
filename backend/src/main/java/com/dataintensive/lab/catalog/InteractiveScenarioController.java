package com.dataintensive.lab.catalog;

import com.dataintensive.lab.ai.AiAssessmentResponse;
import com.dataintensive.lab.ai.AiAssessmentService;
import com.dataintensive.lab.ai.ScenarioAssessmentRequest;
import com.dataintensive.lab.catalog.dto.ScenarioSolutionRequest;
import com.dataintensive.lab.domain.DomainValidationException;
import com.dataintensive.lab.domain.InteractiveScenario;
import com.dataintensive.lab.simulation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class InteractiveScenarioController {

    private final CatalogService catalogService;
    private final ArchitectureStressSimulator stressSimulator;
    private final AiAssessmentService aiAssessmentService;

    public InteractiveScenarioController(
            CatalogService catalogService,
            ArchitectureStressSimulator stressSimulator,
            AiAssessmentService aiAssessmentService) {
        this.catalogService = catalogService;
        this.stressSimulator = stressSimulator;
        this.aiAssessmentService = aiAssessmentService;
    }

    @GetMapping("/labs/{labId}/scenarios")
    public ResponseEntity<List<InteractiveScenario>> getScenariosByLabId(
            @PathVariable String labId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        String locale = CatalogController.resolveLocale(langParam, acceptLanguage);
        List<InteractiveScenario> scenarios = catalogService.getScenariosByLabId(labId, locale);
        return ResponseEntity.ok(scenarios);
    }

    @PutMapping("/scenarios/{scenarioId}/solution")
    public ResponseEntity<Void> saveScenarioSolution(
            @PathVariable String scenarioId,
            @RequestBody ScenarioSolutionRequest request) {
        catalogService.saveScenarioSolution(scenarioId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/scenarios/{scenarioId}/stress-test")
    public ResponseEntity<StressTestResponse> stressTest(
            @PathVariable String scenarioId,
            @RequestBody(required = false) StressTestRequest request,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        String locale = CatalogController.resolveLocale(langParam, acceptLanguage);
        InteractiveScenario scenario = catalogService.getScenarioById(scenarioId, locale)
                .orElseThrow(() -> new DomainValidationException("Cenário não encontrado com id: " + scenarioId));

        String userState = (request != null && request.userStateJson() != null && !request.userStateJson().isBlank())
                ? request.userStateJson()
                : (scenario.savedUserStateJson() != null && !scenario.savedUserStateJson().isBlank()
                ? scenario.savedUserStateJson()
                : scenario.initialStateJson());

        Map<String, Object> overrides = request != null ? request.workloadOverrides() : Map.of();

        var model = stressSimulator.parseModel(userState);
        var workload = stressSimulator.parseWorkload(scenario.constraintsJson(), overrides);
        var result = stressSimulator.simulate(model, workload);

        StressTestResponse response = new StressTestResponse(
                scenarioId,
                result.estimatedLatencyP50Ms(),
                result.estimatedLatencyP99Ms(),
                result.estimatedThroughputOps(),
                result.lockContentionIndex(),
                result.slaMet(),
                workload.targetSlaP99Ms(),
                result.violations(),
                result.recommendations()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/scenarios/{scenarioId}/assess")
    public ResponseEntity<AiAssessmentResponse> assessScenario(
            @PathVariable String scenarioId,
            @RequestBody ScenarioAssessmentRequest request) {
        AiAssessmentResponse response = aiAssessmentService.assessScenario(scenarioId, request);
        return ResponseEntity.ok(response);
    }
}
