package com.dataintensive.lab.catalog;

import com.dataintensive.lab.catalog.dto.ScenarioSolutionRequest;
import com.dataintensive.lab.domain.Difficulty;
import com.dataintensive.lab.domain.InteractiveScenario;
import com.dataintensive.lab.domain.ScenarioType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InteractiveScenarioControllerTest {

    private CatalogService catalogService;
    private com.dataintensive.lab.simulation.ArchitectureStressSimulator stressSimulator;
    private com.dataintensive.lab.ai.AiAssessmentService aiAssessmentService;
    private InteractiveScenarioController controller;

    @BeforeEach
    void setUp() {
        catalogService = mock(CatalogService.class);
        stressSimulator = mock(com.dataintensive.lab.simulation.ArchitectureStressSimulator.class);
        aiAssessmentService = mock(com.dataintensive.lab.ai.AiAssessmentService.class);
        controller = new InteractiveScenarioController(catalogService, stressSimulator, aiAssessmentService);
    }

    @Test
    @DisplayName("GET /api/labs/{labId}/scenarios deve retornar lista de cenários com locale resolvido")
    void shouldReturnScenariosForLab() {
        var scenario = new InteractiveScenario(
                "sc-1", "lab-1", ScenarioType.CANVAS_MODELING, Difficulty.EASY, 1,
                "Modelagem de Particionamento", "Desc", "Briefing", "Gabarito",
                "{}", "{}", "{}", "{}", "{}", "IN_PROGRESS"
        );
        when(catalogService.getScenariosByLabId("lab-1", "pt")).thenReturn(List.of(scenario));

        ResponseEntity<List<InteractiveScenario>> response = controller.getScenariosByLabId("lab-1", null, "pt");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(scenario);
        verify(catalogService).getScenariosByLabId("lab-1", "pt");
    }

    @Test
    @DisplayName("PUT /api/scenarios/{scenarioId}/solution deve salvar o estado do usuário e reflexão")
    void shouldSaveScenarioSolution() {
        var request = new ScenarioSolutionRequest("{\"nodes\":[{\"id\":\"n1\"}]}", "Minha justificativa de arquitetura");

        ResponseEntity<Void> response = controller.saveScenarioSolution("sc-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(catalogService).saveScenarioSolution("sc-1", request);
    }

    @Test
    @DisplayName("POST /api/scenarios/{scenarioId}/stress-test deve executar simulação e retornar métricas de SLA")
    void shouldExecuteStressTestAndReturnMetrics() {
        var scenario = new InteractiveScenario(
                "sc-1", "lab-1", ScenarioType.CANVAS_MODELING, Difficulty.EASY, 1,
                "Modelagem", "Desc", "Briefing", "Gabarito",
                "{\"targetOpsPerSec\": 10000}", "{}", "{}", "{}", "{}", "IN_PROGRESS"
        );
        when(catalogService.getScenarioById("sc-1", "pt")).thenReturn(java.util.Optional.of(scenario));

        var req = new com.dataintensive.lab.simulation.StressTestRequest("{\"nodes\":[]}", Map.of("targetOpsPerSec", 15000));
        var simResult = new com.dataintensive.lab.simulation.StressSimulationResult(2.5, 8.0, 15000.0, 0.05, true, List.of(), List.of("Boa arquitetura"));

        when(stressSimulator.parseModel(any())).thenReturn(mock(com.dataintensive.lab.simulation.ArchitectureModel.class));
        when(stressSimulator.parseWorkload(any(), any())).thenReturn(mock(com.dataintensive.lab.simulation.StressWorkload.class));
        when(stressSimulator.simulate(any(), any())).thenReturn(simResult);

        ResponseEntity<com.dataintensive.lab.simulation.StressTestResponse> response = controller.stressTest("sc-1", req, null, "pt");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().estimatedLatencyP50Ms()).isEqualTo(2.5);
        assertThat(response.getBody().slaMet()).isTrue();
    }

    @Test
    @DisplayName("POST /api/scenarios/{scenarioId}/assess deve delegar ao AiAssessmentService e retornar avaliação socrática")
    void shouldAssessScenarioWithAiTutor() {
        var req = new com.dataintensive.lab.ai.ScenarioAssessmentRequest("{\"nodes\":[]}", "Minha reflexão", null, "heuristic", null, "pt");
        var aiResponse = new com.dataintensive.lab.ai.AiAssessmentResponse("APPROVED", "Excelente análise!", "Trade-off consistente", "Alta eficiência", List.of(), "rules-engine-v1");

        when(aiAssessmentService.assessScenario("sc-1", req)).thenReturn(aiResponse);

        ResponseEntity<com.dataintensive.lab.ai.AiAssessmentResponse> response = controller.assessScenario("sc-1", req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(aiResponse);
        verify(aiAssessmentService).assessScenario("sc-1", req);
    }
}
