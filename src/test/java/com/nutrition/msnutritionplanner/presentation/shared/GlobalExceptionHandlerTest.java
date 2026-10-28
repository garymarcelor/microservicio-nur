package com.nutrition.msnutritionplanner.presentation.shared;

import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/test-uri");
    }

    @Test
    @DisplayName("manejarNoEncontrado() retorna 404")
    void manejarNoEncontrado() {
        EntityNotFoundException ex = new EntityNotFoundException("Entidad", 1);
        ResponseEntity<ApiError> response = handler.manejarNoEncontrado(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().path()).isEqualTo("/test-uri");
    }

    @Test
    @DisplayName("manejarReglaDeNegocio() con BusinessRuleException retorna 400")
    void manejarBusinessRuleException() {
        BusinessRuleException ex = new BusinessRuleException("Regla rota");
        ResponseEntity<ApiError> response = handler.manejarReglaDeNegocio(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Regla rota");
    }

    @Test
    @DisplayName("manejarReglaDeNegocio() con DomainValidationException retorna 400")
    void manejarDomainValidationException() {
        DomainValidationException ex = new DomainValidationException("Validacion fallida");
        ResponseEntity<ApiError> response = handler.manejarReglaDeNegocio(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Validacion fallida");
    }

    @Test
    @DisplayName("manejarGenerico() retorna 500")
    void manejarGenerico() {
        Exception ex = new Exception("Error inesperado");
        ResponseEntity<ApiError> response = handler.manejarGenerico(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Ocurrio un error inesperado");
    }
}
