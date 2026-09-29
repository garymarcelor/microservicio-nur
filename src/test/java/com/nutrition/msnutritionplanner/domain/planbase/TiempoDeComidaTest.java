package com.nutrition.msnutritionplanner.domain.planbase;

import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.shared.Hora;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Nota: al estar en el mismo paquete que TiempoDeComida, este test puede
 * usar sus metodos package-private (crear, agregarReceta, quitarReceta),
 * ya que fuera del agregado PlanAlimentarioBase esta entidad interna no
 * se manipula directamente.
 */
@DisplayName("Entidad interna TiempoDeComida")
class TiempoDeComidaTest {

    @Test
    @DisplayName("crear() construye el tiempo de comida con al menos una receta")
    void creaTiempoValido() {
        TiempoDeComida tiempo = TiempoDeComida.crear(Hora.of(7, 30), List.of(10, 20));

        assertThat(tiempo.getHora()).isEqualTo(Hora.of(7, 30));
        assertThat(tiempo.getRecetasIds()).containsExactly(10, 20);
        assertThat(tiempo.getId()).isNull();
    }

    @Test
    @DisplayName("crear() sin recetas lanza DomainValidationException")
    void fallaSinRecetas() {
        assertThatThrownBy(() -> TiempoDeComida.crear(Hora.of(7, 30), List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("receta");
    }

    @Test
    @DisplayName("agregarReceta() suma un nuevo id de receta")
    void agregaReceta() {
        TiempoDeComida tiempo = TiempoDeComida.crear(Hora.of(7, 30), List.of(10));

        tiempo.agregarReceta(20);

        assertThat(tiempo.getRecetasIds()).containsExactly(10, 20);
    }

    @Test
    @DisplayName("quitarReceta() no permite dejar el tiempo de comida sin recetas")
    void noPermiteQuitarLaUltimaReceta() {
        TiempoDeComida tiempo = TiempoDeComida.crear(Hora.of(7, 30), List.of(10));

        assertThatThrownBy(() -> tiempo.quitarReceta(10))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("reemplazarHora() actualiza la hora del tiempo de comida")
    void reemplazaHora() {
        TiempoDeComida tiempo = TiempoDeComida.crear(Hora.of(7, 30), List.of(10));

        tiempo.reemplazarHora(Hora.of(8, 0));

        assertThat(tiempo.getHora()).isEqualTo(Hora.of(8, 0));
    }
}
