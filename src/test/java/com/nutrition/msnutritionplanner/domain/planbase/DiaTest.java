package com.nutrition.msnutritionplanner.domain.planbase;

import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.shared.Hora;
import com.nutrition.msnutritionplanner.domain.shared.Orden;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Entidad interna Dia")
class DiaTest {

    @Test
    @DisplayName("crear() construye un dia sin tiempos de comida")
    void creaDiaValido() {
        Dia dia = Dia.crear(Orden.of(1));

        assertThat(dia.getNumOrden()).isEqualTo(Orden.of(1));
        assertThat(dia.getNroTiemposComida()).isZero();
        assertThat(dia.getTiemposDeComida()).isEmpty();
    }

    @Test
    @DisplayName("agregarTiempoDeComida() suma un tiempo de comida al dia")
    void agregaTiempoDeComida() {
        Dia dia = Dia.crear(Orden.of(1));

        dia.agregarTiempoDeComida(Hora.of(7, 30), List.of(1));

        assertThat(dia.getNroTiemposComida()).isEqualTo(1);
        assertThat(dia.getTiemposDeComida().get(0).getHora()).isEqualTo(Hora.of(7, 30));
    }

    @Test
    @DisplayName("agregarTiempoDeComida() rechaza una hora ya programada en el mismo dia")
    void noPermiteHorasDuplicadas() {
        Dia dia = Dia.crear(Orden.of(1));
        dia.agregarTiempoDeComida(Hora.of(7, 30), List.of(1));

        assertThatThrownBy(() -> dia.agregarTiempoDeComida(Hora.of(7, 30), List.of(2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ya existe un tiempo de comida");
    }

    @Test
    @DisplayName("quitarTiempoDeComida() no permite dejar el dia sin tiempos de comida")
    void noPermiteQuitarElUltimoTiempo() {
        Dia dia = Dia.crear(Orden.of(1));
        TiempoDeComida tiempo = dia.agregarTiempoDeComida(Hora.of(7, 30), List.of(1));

        // El tiempo aun no tiene id (no persistido); simulamos el caso limite
        // igualmente cubierto por la regla de negocio.
        assertThatThrownBy(() -> dia.quitarTiempoDeComida(tiempo.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("al menos un tiempo de comida");
    }

    @Test
    @DisplayName("quitarTiempoDeComida() con un id que no pertenece al dia lanza DomainValidationException")
    void fallaAlQuitarTiempoInexistente() {
        Dia dia = Dia.crear(Orden.of(1));
        dia.agregarTiempoDeComida(Hora.of(7, 30), List.of(1));
        dia.agregarTiempoDeComida(Hora.of(13, 0), List.of(2));

        assertThatThrownBy(() -> dia.quitarTiempoDeComida(9999))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("reordenar() cambia el numero de orden del dia")
    void reordena() {
        Dia dia = Dia.crear(Orden.of(1));

        dia.reordenar(Orden.of(5));

        assertThat(dia.getNumOrden()).isEqualTo(Orden.of(5));
    }
}
