package com.nutrition.msnutritionplanner.domain.shared;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Value Object Hora")
class HoraTest {

    @Test
    @DisplayName("of(LocalTime) crea la hora cuando el valor es valido")
    void creaHoraValida() {
        Hora hora = Hora.of(LocalTime.of(7, 30));

        assertThat(hora.valor()).isEqualTo(LocalTime.of(7, 30));
    }

    @Test
    @DisplayName("of(horas, minutos) es equivalente a of(LocalTime)")
    void creaHoraConHorasYMinutos() {
        Hora hora = Hora.of(19, 45);

        assertThat(hora).isEqualTo(Hora.of(LocalTime.of(19, 45)));
    }

    @Test
    @DisplayName("of(null) lanza DomainValidationException")
    void fallaConHoraNula() {
        assertThatThrownBy(() -> Hora.of(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("obligatoria");
    }

    @Test
    @DisplayName("dos horas con el mismo valor son iguales (comparacion por valor)")
    void igualdadPorValor() {
        Hora a = Hora.of(8, 0);
        Hora b = Hora.of(8, 0);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
