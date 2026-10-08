package com.dataintensive.lab.query;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "*")
public class QueryController {

    private final QueryExecutionService queryExecutionService;

    public QueryController(QueryExecutionService queryExecutionService) {
        this.queryExecutionService = queryExecutionService;
    }

    @PostMapping("/execute")
    public ResponseEntity<QueryResult> executeQuery(@Valid @RequestBody QueryRequest request) {
        QueryResult result = queryExecutionService.execute(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset/{labId}")
    public ResponseEntity<QueryResult> resetLab(@PathVariable String labId) {
        QueryResult result = queryExecutionService.resetLab(labId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/cancel")
    public ResponseEntity<java.util.Map<String, Object>> cancelQuery() {
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Consulta cancelada com sucesso"
        ));
    }
}
