package com.nutrition.msnutritionplanner.domain.exceptions;

/**
 * Excepcion base de dominio. Toda regla de negocio infringida dentro del
 * modelo de dominio debe lanzar una subclase de esta excepcion, nunca
 * excepciones tecnicas de infraestructura.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
