package com.nutrition.msnutritionplanner.domain.exceptions;

/**
 * Se lanza cuando un dato de entrada no cumple con las invariantes
 * basicas de una entidad o value object del dominio.
 */
public class DomainValidationException extends DomainException {
    public DomainValidationException(String message) {
        super(message);
    }
}
