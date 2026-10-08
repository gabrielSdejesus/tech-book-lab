package com.dataintensive.lab.config;

import com.dataintensive.lab.domain.DomainValidationException;
import com.dataintensive.lab.domain.QueryExecutionException;
import com.dataintensive.lab.provisioning.LabNotFoundException;
import com.dataintensive.lab.provisioning.SessionExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentNotValidException com status 400 e lista de erros nos properties")
    @SuppressWarnings("unchecked")
    void shouldHandleMethodArgumentNotValidException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "query", "A consulta SQL/Cypher é obrigatória"));
        bindingResult.addError(new FieldError("target", "engineType", "O tipo de motor é obrigatório"));

        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ProblemDetail> response = handler.handleValidationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getTitle()).isEqualTo("Erro de validação sintática");
        assertThat(body.getDetail()).isEqualTo("Um ou mais campos da requisição são inválidos.");
        assertThat(body.getType()).isEqualTo(URI.create("urn:problem:validation-error"));

        List<Map<String, String>> errors = (List<Map<String, String>>) body.getProperties().get("errors");
        assertThat(errors).hasSize(2);
        assertThat(errors.get(0)).containsEntry("field", "query")
                .containsEntry("message", "A consulta SQL/Cypher é obrigatória");
        assertThat(errors.get(1)).containsEntry("field", "engineType")
                .containsEntry("message", "O tipo de motor é obrigatório");
    }

    @Test
    @DisplayName("Deve tratar HttpMessageNotReadableException com status 400 e URI urn:problem:malformed-json")
    void shouldHandleHttpMessageNotReadableException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Malformed JSON",
                new MockHttpInputMessage(new byte[0])
        );

        ResponseEntity<ProblemDetail> response = handler.handleMessageNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getTitle()).isEqualTo("Requisição JSON inválida");
        assertThat(body.getDetail()).isEqualTo("O corpo da requisição é nulo ou contém formato JSON inválido.");
        assertThat(body.getType()).isEqualTo(URI.create("urn:problem:malformed-json"));
    }

    @Test
    @DisplayName("Deve tratar DomainValidationException com status 400 e repassar mensagem de erro")
    void shouldHandleDomainValidationException() {
        DomainValidationException ex = new DomainValidationException("Regra de negócio customizada violada.");

        ResponseEntity<ProblemDetail> response = handler.handleDomainValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getTitle()).isEqualTo("Regra de negócio violada");
        assertThat(body.getDetail()).isEqualTo("Regra de negócio customizada violada.");
        assertThat(body.getType()).isEqualTo(URI.create("https://api.dataintensive.lab/errors/validation"));
    }

    @Test
    @DisplayName("Deve tratar LabNotFoundException com status 404 NOT_FOUND")
    void shouldHandleLabNotFoundException() {
        LabNotFoundException ex = new LabNotFoundException("lab-inexistente");

        ResponseEntity<ProblemDetail> response = handler.handleLabNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getTitle()).isEqualTo("Recurso Não Encontrado");
        assertThat(body.getDetail()).isEqualTo("Laboratório 'lab-inexistente' não foi encontrado no catálogo técnico.");
        assertThat(body.getType()).isEqualTo(URI.create("https://api.dataintensive.lab/errors/not-found"));
    }

    @Test
    @DisplayName("Deve tratar SessionExpiredException com status 410 GONE")
    void shouldHandleSessionExpiredException() {
        SessionExpiredException ex = new SessionExpiredException("sess-123");

        ResponseEntity<ProblemDetail> response = handler.handleSessionExpired(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(410);
        assertThat(body.getTitle()).isEqualTo("Sessão Expirada");
        assertThat(body.getDetail()).isEqualTo("Sessão 'sess-123' expirada ou não possui ambiente ativo.");
        assertThat(body.getType()).isEqualTo(URI.create("https://api.dataintensive.lab/errors/session-expired"));
    }

    @Test
    @DisplayName("Deve tratar QueryExecutionException com status 400 e propriedade executionTimeMs")
    void shouldHandleQueryExecutionException() {
        QueryExecutionException ex = new QueryExecutionException("relation \"tabela_inexistente\" does not exist", 85L);

        ResponseEntity<ProblemDetail> response = handler.handleQueryExecution(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getTitle()).isEqualTo("Erro na execução da consulta");
        assertThat(body.getDetail()).isEqualTo("relation \"tabela_inexistente\" does not exist");
        assertThat(body.getType()).isEqualTo(URI.create("urn:problem:query-execution-error"));
        assertThat(body.getProperties()).containsEntry("executionTimeMs", 85L);
    }
}
