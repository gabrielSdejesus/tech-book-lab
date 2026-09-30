package com.dataintensive.lab.config;

import com.dataintensive.lab.domain.DomainValidationException;
import com.dataintensive.lab.domain.QueryExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Um ou mais campos da requisição são inválidos."
        );
        problemDetail.setTitle("Erro de validação sintática");
        problemDetail.setType(URI.create("urn:problem:validation-error"));

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() != null ? error.getDefaultMessage() : "Campo inválido"
                ))
                .toList();

        problemDetail.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "O corpo da requisição é nulo ou contém formato JSON inválido."
        );
        problemDetail.setTitle("Requisição JSON inválida");
        problemDetail.setType(URI.create("urn:problem:malformed-json"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(DomainValidationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Regra de negócio violada");
        problemDetail.setType(URI.create("https://api.dataintensive.lab/errors/validation"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(com.dataintensive.lab.provisioning.LabNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleLabNotFound(com.dataintensive.lab.provisioning.LabNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Recurso Não Encontrado");
        problemDetail.setType(URI.create("https://api.dataintensive.lab/errors/not-found"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(com.dataintensive.lab.provisioning.SessionExpiredException.class)
    public ResponseEntity<ProblemDetail> handleSessionExpired(com.dataintensive.lab.provisioning.SessionExpiredException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.GONE,
                ex.getMessage()
        );
        problemDetail.setTitle("Sessão Expirada");
        problemDetail.setType(URI.create("https://api.dataintensive.lab/errors/session-expired"));
        return ResponseEntity.status(HttpStatus.GONE).body(problemDetail);
    }

    @ExceptionHandler(com.dataintensive.lab.provisioning.LabEnvironmentNotReadyException.class)
    public ResponseEntity<ProblemDetail> handleLabNotReady(com.dataintensive.lab.provisioning.LabEnvironmentNotReadyException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("Ambiente Não Pronto");
        problemDetail.setType(URI.create("https://api.dataintensive.lab/errors/environment-not-ready"));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(QueryExecutionException.class)
    public ResponseEntity<ProblemDetail> handleQueryExecution(QueryExecutionException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Erro na execução da consulta");
        problemDetail.setType(URI.create("urn:problem:query-execution-error"));
        problemDetail.setProperty("executionTimeMs", ex.getExecutionTimeMs());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
}
