package com.nutrition.msnutritionplanner.domain.exceptions;

/**
 * Se lanza cuando una entidad de dominio solicitada no existe
 * (o fue dada de baja logicamente).
 */
public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String entidad, Object id) {
        super("No se encontro " + entidad + " con id: " + id);
    }
}
