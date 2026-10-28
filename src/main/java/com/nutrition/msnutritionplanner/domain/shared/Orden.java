package com.nutrition.msnutritionplanner.domain.shared;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;

import java.util.Objects;

/**
 * Value Object «Order» del modelo de dominio — representa la posicion
 * (1..N) de un Dia dentro de un Plan Alimentario, o de cualquier otro
 * elemento que deba mantenerse ordenado.
 */
public final class Orden implements ValueObject {

    private final int numero;

    private Orden(int numero) {
        this.numero = numero;
    }

    public static Orden of(int numero) {
        if (numero <= 0) {
            throw new DomainValidationException("El numero de orden debe ser mayor a cero");
        }
        return new Orden(numero);
    }

    public int valor() {
        return numero;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Orden other)) return false;
        return numero == other.numero;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return String.valueOf(numero);
    }
}
