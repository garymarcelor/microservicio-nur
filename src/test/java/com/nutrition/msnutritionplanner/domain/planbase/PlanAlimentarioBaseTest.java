package com.nutrition.msnutritionplanner.domain.planbase;

import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;
import com.nutrition.msnutritionplanner.domain.shared.Hora;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agregado Raiz PlanAlimentarioBase")
class PlanAlimentarioBaseTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 9, 1);

    @Test
    @DisplayName("crear() construye un plan INICIADO, activo y sin dias")
    void creaPlanValido() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.crear(FECHA);

        assertThat(plan.getEstadoPlan()).isEqualTo(EstadoPlan.INICIADO);
        assertThat(plan.getFechaInicioVigencia()).isEqualTo(FECHA);
        assertThat(plan.isActivo()).isTrue();
        assertThat(plan.getDias()).isEmpty();
    }

    @Test
    @DisplayName("crear() sin fecha lanza DomainValidationException")
    void fallaSinFecha() {
        assertThatThrownBy(() -> PlanAlimentarioBase.crear(null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("agregarDia() asigna el orden de forma secuencial (1, 2, 3...)")
    void agregaDiasEnOrden() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.crear(FECHA);

        Dia dia1 = plan.agregarDia();
        Dia dia2 = plan.agregarDia();
        Dia dia3 = plan.agregarDia();

        assertThat(dia1.getNumOrden().valor()).isEqualTo(1);
        assertThat(dia2.getNumOrden().valor()).isEqualTo(2);
        assertThat(dia3.getNumOrden().valor()).isEqualTo(3);
        assertThat(plan.getDias()).hasSize(3);
    }

    @Test
    @DisplayName("agregarTiempoDeComida() delega en el Dia indicado")
    void agregaTiempoDeComidaAUnDia() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.crear(FECHA);
        Dia dia = plan.agregarDia();

        // El id del dia aun es null (no persistido); se simula reconstruyendo
        // el plan con un id de dia conocido para probar la delegacion.
        PlanAlimentarioBase reconstruido = PlanAlimentarioBase.reconstruir(
                1, EstadoPlan.INICIADO, FECHA, true, List.of(Dia.reconstruir(100, dia.getNumOrden(), List.of())));

        reconstruido.agregarTiempoDeComida(100, Hora.of(7, 30), List.of(1));

        assertThat(reconstruido.obtenerDia(100).getNroTiemposComida()).isEqualTo(1);
    }

    @Test
    @DisplayName("quitarDia() reordena los dias restantes")
    void quitarDiaReordena() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(
                1, EstadoPlan.INICIADO, FECHA, true, List.of(
                        Dia.reconstruir(10, com.nutrition.msnutritionplanner.domain.shared.Orden.of(1), List.of()),
                        Dia.reconstruir(20, com.nutrition.msnutritionplanner.domain.shared.Orden.of(2), List.of()),
                        Dia.reconstruir(30, com.nutrition.msnutritionplanner.domain.shared.Orden.of(3), List.of())));

        plan.quitarDia(20);

        assertThat(plan.getDias()).hasSize(2);
        assertThat(plan.getDias().get(0).getId()).isEqualTo(10);
        assertThat(plan.getDias().get(0).getNumOrden().valor()).isEqualTo(1);
        assertThat(plan.getDias().get(1).getId()).isEqualTo(30);
        assertThat(plan.getDias().get(1).getNumOrden().valor()).isEqualTo(2);
    }

    @Test
    @DisplayName("quitarDia() no permite dejar el plan sin dias")
    void noPermiteQuitarElUltimoDia() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.crear(FECHA);
        plan.agregarDia();

        Integer idUnicoDia = plan.getDias().get(0).getId(); // null, pero sirve para el caso

        assertThatThrownBy(() -> plan.quitarDia(idUnicoDia))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("al menos un dia");
    }

    @Test
    @DisplayName("completar() requiere estado INICIADO y al menos un dia")
    void completarRequiereDiasYEstadoIniciado() {
        PlanAlimentarioBase sinDias = PlanAlimentarioBase.crear(FECHA);
        assertThatThrownBy(sinDias::completar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("al menos un dia");

        PlanAlimentarioBase conDias = PlanAlimentarioBase.crear(FECHA);
        conDias.agregarDia();
        conDias.completar();
        assertThat(conDias.getEstadoPlan()).isEqualTo(EstadoPlan.COMPLETO);

        assertThatThrownBy(conDias::completar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("INICIADO");
    }

    @Test
    @DisplayName("descartar() cambia el estado a DESCARTADO, incluso desde COMPLETO")
    void descartaPlan() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.crear(FECHA);
        plan.agregarDia();
        plan.completar();

        plan.descartar();

        assertThat(plan.getEstadoPlan()).isEqualTo(EstadoPlan.DESCARTADO);
    }

    @Test
    @DisplayName("descartar() sobre un plan ya descartado lanza BusinessRuleException")
    void noPermiteDescartarDosVeces() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.crear(FECHA);
        plan.agregarDia();
        plan.descartar();

        assertThatThrownBy(plan::descartar)
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("un plan COMPLETO ya no admite agregar dias (solo editable en INICIADO)")
    void noPermiteEditarPlanNoIniciado() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.crear(FECHA);
        plan.agregarDia();
        plan.completar();

        assertThatThrownBy(plan::agregarDia)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("INICIADO");
    }

    @Test
    @DisplayName("darDeBaja() desactiva el plan e impide seguir editandolo")
    void daDeBajaElPlan() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.crear(FECHA);

        plan.darDeBaja();

        assertThat(plan.isActivo()).isFalse();
        assertThatThrownBy(plan::agregarDia)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("dado de baja");
    }
}
