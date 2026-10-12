package com.dataintensive.lab.catalog;

import com.dataintensive.lab.catalog.dto.ScenarioSolutionRequest;
import com.dataintensive.lab.domain.InteractiveScenario;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class InteractiveScenarioController {

    private final CatalogService catalogService;

    public InteractiveScenarioController(CatalogService catalogService) {
        this.catalogService = catalogService;
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
}
