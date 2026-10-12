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
    private InteractiveScenarioController controller;

    @BeforeEach
    void setUp() {
        catalogService = mock(CatalogService.class);
        controller = new InteractiveScenarioController(catalogService);
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
}
