package com.dataintensive.lab.query;

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
    public ResponseEntity<QueryResult> executeQuery(@RequestBody QueryRequest request) {
        QueryResult result = queryExecutionService.execute(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset/{labId}")
    public ResponseEntity<QueryResult> resetLab(@PathVariable String labId) {
        QueryResult result = queryExecutionService.resetLab(labId);
        return ResponseEntity.ok(result);
    }
}
