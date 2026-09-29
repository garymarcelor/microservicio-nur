package com.nutrition.msnutritionplanner.domain.shared;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Value Object Orden")
class OrdenTest {

    @Test
    @DisplayName("of(n) crea el orden cuando n es mayor a cero")
    void creaOrdenValido() {
        assertThat(Orden.of(1).valor()).isEqualTo(1);
        assertThat(Orden.of(15).valor()).isEqualTo(15);
    }

    @Test
    @DisplayName("of(0) lanza DomainValidationException")
    void fallaConCero() {
        assertThatThrownBy(() -> Orden.of(0))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("of(negativo) lanza DomainValidationException")
    void fallaConNegativo() {
        assertThatThrownBy(() -> Orden.of(-3))
                .isInstanceOf(DomainValidationException.class);
    }
}
