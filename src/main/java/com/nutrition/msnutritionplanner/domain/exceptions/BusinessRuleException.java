package com.nutrition.msnutritionplanner.domain.exceptions;

/**
 * Se lanza cuando una operacion viola una regla de negocio del
 * contexto delimitado de Planes Nutricionales y Recetas.
 */
public class BusinessRuleException extends DomainException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
