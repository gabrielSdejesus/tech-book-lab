package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.catalog.CatalogRepository;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LabProvisioningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LabContainerManager containerManager;

    @MockBean
    private CatalogRepository catalogRepository;

    private final String validSessionId = UUID.randomUUID().toString();
    private final String validLabId = "ddia-cap-03-lab-01";
    private final Lab mockLab = new Lab(
            validLabId,
            1,
            "relacional-vs-documentos",
            "Modelagem Relacional",
            "Sumário",
            List.of(),
            EngineType.POSTGRES,
            "tbl_lab",
            "SELECT 1;",
            List.of()
    );

    @Test
    @DisplayName("POST /api/lab/{labId}/provision deve retornar 400 ProblemDetail quando X-Session-Id estiver ausente")
    void shouldReturn400WhenSessionIdMissing() throws Exception {
        mockMvc.perform(post("/api/lab/{labId}/provision", validLabId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.dataintensive.lab/errors/validation"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("POST /api/lab/{labId}/provision deve retornar 400 ProblemDetail quando X-Session-Id for inválido")
    void shouldReturn400WhenSessionIdInvalid() throws Exception {
        mockMvc.perform(post("/api/lab/{labId}/provision", validLabId)
                        .header("X-Session-Id", "malicious-input-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.dataintensive.lab/errors/validation"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("UUID v4")));
    }

    @Test
    @DisplayName("POST /api/lab/{labId}/provision deve retornar 404 ProblemDetail quando labId não existir")
    void shouldReturn404WhenLabNotFound() throws Exception {
        when(catalogRepository.findLabById("lab-desconhecido")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/lab/{labId}/provision", "lab-desconhecido")
                        .header("X-Session-Id", validSessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.dataintensive.lab/errors/not-found"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("lab-desconhecido")));
    }

    @Test
    @DisplayName("POST /api/lab/{labId}/provision deve retornar 200/202 com status do provisionamento")
    void shouldProvisionSuccessfully() throws Exception {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        mockMvc.perform(post("/api/lab/{labId}/provision", validLabId)
                        .header("X-Session-Id", validSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labId").value(validLabId))
                .andExpect(jsonPath("$.engineType").value("POSTGRES"))
                .andExpect(jsonPath("$.containerName").value("tbl-lab-postgres"))
                .andExpect(jsonPath("$.allocatedPort").value(5432))
                .andExpect(jsonPath("$.heartbeatIntervalSeconds").value(30));
    }

    @Test
    @DisplayName("GET /api/lab/{labId}/status deve retornar estado do ambiente")
    void shouldGetStatusSuccessfully() throws Exception {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        // Provisiona primeiro
        mockMvc.perform(post("/api/lab/{labId}/provision", validLabId)
                .header("X-Session-Id", validSessionId));

        // Consulta status
        mockMvc.perform(get("/api/lab/{labId}/status", validLabId)
                        .header("X-Session-Id", validSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labId").value(validLabId))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.containerName").value("tbl-lab-postgres"))
                .andExpect(jsonPath("$.allocatedPort").value(5432));
    }

    @Test
    @DisplayName("POST /api/lab/{labId}/heartbeat deve retornar ACK e renovar TTL")
    void shouldRenewHeartbeat() throws Exception {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        mockMvc.perform(post("/api/lab/{labId}/provision", validLabId)
                .header("X-Session-Id", validSessionId));

        mockMvc.perform(post("/api/lab/{labId}/heartbeat", validLabId)
                        .header("X-Session-Id", validSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACK"))
                .andExpect(jsonPath("$.ttlRemainingSeconds").value(900));
    }

    @Test
    @DisplayName("POST /api/lab/{labId}/teardown deve encerrar o ambiente e retornar STOPPED")
    void shouldTeardownEnvironment() throws Exception {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        mockMvc.perform(post("/api/lab/{labId}/provision", validLabId)
                .header("X-Session-Id", validSessionId));

        mockMvc.perform(post("/api/lab/{labId}/teardown", validLabId)
                        .header("X-Session-Id", validSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOPPED"));
    }

    @Test
    @DisplayName("POST /api/lab/{labId}/provision com challengeId deve provisionar motor da tarefa e retornar dados correspondentes")
    void shouldProvisionWithChallengeId() throws Exception {
        when(catalogRepository.findLabById(validLabId)).thenReturn(Optional.of(mockLab));
        when(containerManager.isEngineHealthy(EngineType.POSTGRES, 5432)).thenReturn(true);

        mockMvc.perform(post("/api/lab/{labId}/provision", validLabId)
                        .param("challengeId", "lab-01-ch-1")
                        .header("X-Session-Id", validSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labId").value(validLabId))
                .andExpect(jsonPath("$.challengeId").value("lab-01-ch-1"))
                .andExpect(jsonPath("$.engineType").value("POSTGRES"))
                .andExpect(jsonPath("$.status").value("READY"));
    }
}
