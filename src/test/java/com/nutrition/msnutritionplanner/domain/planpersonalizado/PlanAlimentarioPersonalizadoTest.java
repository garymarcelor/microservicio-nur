package com.nutrition.msnutritionplanner.domain.planpersonalizado;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agregado Raiz PlanAlimentarioPersonalizado")
class PlanAlimentarioPersonalizadoTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 9, 1);

    @Test
    @DisplayName("crearDesde() genera una version INICIADA a partir de un plan base COMPLETO y persistido")
    void creaPersonalizadoValido() {
        PlanAlimentarioBase base = PlanAlimentarioBase.reconstruir(1, EstadoPlan.COMPLETO, FECHA, true, List.of());

        PlanAlimentarioPersonalizado personalizado = PlanAlimentarioPersonalizado.crearDesde(base, "pac-1", "Cambio de dieta");

        assertThat(personalizado.getIdPlanBase()).isEqualTo(1);
        assertThat(personalizado.getIdPaciente()).isEqualTo("pac-1");
        assertThat(personalizado.getDescripcionCambios()).isEqualTo("Cambio de dieta");
        assertThat(personalizado.getEstadoPlan()).isEqualTo(EstadoPlan.INICIADO);
        assertThat(personalizado.getFechaInicioVigencia()).isEqualTo(FECHA);
    }

    @Test
    @DisplayName("crearDesde() con un plan de origen sin persistir (sin id) lanza DomainValidationException")
    void fallaConPlanOrigenSinPersistir() {
        PlanAlimentarioBase base = PlanAlimentarioBase.crear(FECHA); // sin id

        assertThatThrownBy(() -> PlanAlimentarioPersonalizado.crearDesde(base, "pac-1", "cambio"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("persistido");
    }

    @Test
    @DisplayName("crearDesde() con un plan de origen que no esta COMPLETO lanza DomainValidationException")
    void fallaConPlanOrigenNoCompleto() {
        PlanAlimentarioBase base = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, FECHA, true, List.of());

        assertThatThrownBy(() -> PlanAlimentarioPersonalizado.crearDesde(base, "pac-1", "cambio"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("COMPLETO");
    }

    @Test
    @DisplayName("crearDesde() con idPaciente en blanco lanza DomainValidationException")
    void fallaConIdPacienteEnBlanco() {
        PlanAlimentarioBase base = PlanAlimentarioBase.reconstruir(1, EstadoPlan.COMPLETO, FECHA, true, List.of());

        assertThatThrownBy(() -> PlanAlimentarioPersonalizado.crearDesde(base, " ", "cambio"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("reconstruir() sin idPlanBase lanza DomainValidationException")
    void fallaReconstruirSinPlanBase() {
        assertThatThrownBy(() -> PlanAlimentarioPersonalizado.reconstruir(
                5, null, "pac-1", "cambio", EstadoPlan.INICIADO, FECHA, true, List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("idPlanBase");
    }

    @Test
    @DisplayName("actualizarDescripcionCambios() modifica el texto en un plan editable")
    void actualizaDescripcion() {
        PlanAlimentarioBase base = PlanAlimentarioBase.reconstruir(1, EstadoPlan.COMPLETO, FECHA, true, List.of());
        PlanAlimentarioPersonalizado personalizado = PlanAlimentarioPersonalizado.crearDesde(base, "pac-1", "version inicial");

        personalizado.actualizarDescripcionCambios("version corregida");

        assertThat(personalizado.getDescripcionCambios()).isEqualTo("version corregida");
    }

    @Test
    @DisplayName("PlanAlimentarioPersonalizado hereda el comportamiento de PlanAlimentarioBase (agregarDia)")
    void heredaComportamientoDePlanBase() {
        PlanAlimentarioBase base = PlanAlimentarioBase.reconstruir(1, EstadoPlan.COMPLETO, FECHA, true, List.of());
        PlanAlimentarioPersonalizado personalizado = PlanAlimentarioPersonalizado.crearDesde(base, "pac-1", "cambio");

        personalizado.agregarDia();

        assertThat(personalizado.getDias()).hasSize(1);
        assertThat(personalizado).isInstanceOf(PlanAlimentarioBase.class);
    }
}
