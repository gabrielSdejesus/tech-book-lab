package com.dataintensive.lab.query;

import com.dataintensive.lab.config.GlobalExceptionHandler;
import com.dataintensive.lab.domain.EngineType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueryController.class)
@Import(GlobalExceptionHandler.class)
class QueryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QueryExecutionService queryExecutionService;

    @Test
    @DisplayName("POST /api/query/execute - Deve retornar HTTP 200 com colunas e linhas em caso de sucesso")
    void shouldExecuteQuerySuccessfullyWithStatus200() throws Exception {
        QueryResult mockResult = QueryResult.ok(
                List.of("id", "nome"),
                List.of(Map.of("id", 1, "nome", "Postgres User")),
                42
        );
        when(queryExecutionService.execute(any(QueryRequest.class))).thenReturn(mockResult);

        QueryRequest request = new QueryRequest("SELECT id, nome FROM usuarios;", EngineType.POSTGRES, "ddia-cap-03-lab-01");

        mockMvc.perform(post("/api/query/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.columns", hasSize(2)))
                .andExpect(jsonPath("$.columns[0]", is("id")))
                .andExpect(jsonPath("$.columns[1]", is("nome")))
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].nome", is("Postgres User")))
                .andExpect(jsonPath("$.executionTimeMs", is(42)));
    }

    @Test
    @DisplayName("POST /api/query/reset/{labId} - Deve resetar esquema do laboratório e retornar HTTP 200")
    void shouldResetLabSchemaSuccessfullyWithStatus200() throws Exception {
        QueryResult mockResult = QueryResult.update(0, 15);
        when(queryExecutionService.resetLab("ddia-cap-03-lab-01")).thenReturn(mockResult);

        mockMvc.perform(post("/api/query/reset/{labId}", "ddia-cap-03-lab-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.rowCount", is(0)));
    }
}
