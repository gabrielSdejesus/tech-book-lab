package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.provisioning.dto.EngineHealthStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InfraStatusController.class)
class InfraStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InfraStatusService infraStatusService;

    @Test
    @DisplayName("GET /api/infra/status - Deve retornar status dos motores com HTTP 200 OK")
    void shouldReturnInfraStatusWithStatus200() throws Exception {
        Map<String, EngineHealthStatus> mockStatuses = new LinkedHashMap<>();
        mockStatuses.put("postgres", new EngineHealthStatus(true, 5432, "postgres", "UP"));
        mockStatuses.put("neo4j", new EngineHealthStatus(false, 7687, "neo4j", "DOWN"));

        when(infraStatusService.getInfraStatus()).thenReturn(mockStatuses);

        mockMvc.perform(get("/api/infra/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postgres.healthy", is(true)))
                .andExpect(jsonPath("$.postgres.port", is(5432)))
                .andExpect(jsonPath("$.postgres.serviceName", is("postgres")))
                .andExpect(jsonPath("$.postgres.status", is("UP")))
                .andExpect(jsonPath("$.neo4j.healthy", is(false)))
                .andExpect(jsonPath("$.neo4j.port", is(7687)))
                .andExpect(jsonPath("$.neo4j.serviceName", is("neo4j")))
                .andExpect(jsonPath("$.neo4j.status", is("DOWN")));
    }
}
