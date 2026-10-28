package com.nutrition.msnutritionplanner.domain.shared;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Value Object «Hora» — representa la hora del dia en la que se debe
 * servir un tiempo de comida.
 */
public final class Hora implements ValueObject {

    private final LocalTime hora;

    private Hora(LocalTime hora) {
        this.hora = hora;
    }

    public static Hora of(LocalTime hora) {
        if (hora == null) {
            throw new DomainValidationException("La hora del tiempo de comida es obligatoria");
        }
        return new Hora(hora);
    }

    public static Hora of(int horas, int minutos) {
        return of(LocalTime.of(horas, minutos));
    }

    public LocalTime valor() {
        return hora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hora other)) return false;
        return Objects.equals(hora, other.hora);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hora);
    }

    @Override
    public String toString() {
        return hora.toString();
    }
}
