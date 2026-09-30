package com.dataintensive.lab.query;

import com.dataintensive.lab.domain.EngineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QueryRequest(
    @NotBlank(message = "A consulta SQL/Cypher é obrigatória")
    @Size(max = 10000, message = "A consulta não pode exceder 10.000 caracteres")
    String query,

    @NotNull(message = "O tipo de motor de banco (engineType) é obrigatório")
    EngineType engineType,

    @NotBlank(message = "O identificador do laboratório (labId) é obrigatório")
    String labId
) {}
