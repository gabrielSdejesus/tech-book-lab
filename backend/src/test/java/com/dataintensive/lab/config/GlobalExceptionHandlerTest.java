package com.dataintensive.lab.config;

import com.dataintensive.lab.domain.DomainValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
        bindingResult.addError(new FieldError("target", "code", "O código da solução é obrigatório"));

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
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).containsEntry("field", "code")
                .containsEntry("message", "O código da solução é obrigatório");
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
}
