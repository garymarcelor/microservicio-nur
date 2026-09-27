package com.nutrition.msnutritionplanner.domain.planpersonalizado;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.planbase.Dia;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;

import java.time.LocalDate;
import java.util.List;

/**
 * Agregado Raiz «PlanAlimentarioPersonalizado».
 *
 * Especializa a {@link PlanAlimentarioBase} (ver modelo de dominio:
 * relacion de generalizacion "Agregado: PlanPersonalizado" -&gt;
 * "Agregado Plan Base"). Representa una nueva version de un plan
 * alimentario, derivada de un plan base o de otro plan personalizado
 * previo, contratada dentro del servicio de Catering (planes de 15 o
 * 30 dias) cuando el paciente requiere cambios sobre el plan original.
 */
public class PlanAlimentarioPersonalizado extends PlanAlimentarioBase {

    private Integer idPlanBase;
    private String descripcionCambios;

    protected PlanAlimentarioPersonalizado() {
        super();
    }

    private PlanAlimentarioPersonalizado(Integer id, Integer idPlanBase, String descripcionCambios,
                                          EstadoPlan estadoPlan, LocalDate fechaInicioVigencia,
                                          boolean activo, List<Dia> dias) {
        super(id, estadoPlan, fechaInicioVigencia, activo, dias);
        this.idPlanBase = idPlanBase;
        this.descripcionCambios = descripcionCambios;
        validarPersonalizado();
    }

    /**
     * Crea una nueva version personalizada a partir de un plan base (o de
     * otro plan personalizado) existente, copiando su calendario de dias
     * como punto de partida para luego ser editado.
     */
    public static PlanAlimentarioPersonalizado crearDesde(PlanAlimentarioBase planOrigen, String descripcionCambios) {
        if (planOrigen == null || planOrigen.getId() == null) {
            throw new DomainValidationException("Se requiere un plan de origen persistido para crear una version personalizada");
        }
        if (planOrigen.getEstadoPlan() != EstadoPlan.COMPLETO) {
            throw new DomainValidationException("Solo se puede personalizar un plan que se encuentre COMPLETO");
        }
        return new PlanAlimentarioPersonalizado(
                null, planOrigen.getId(), descripcionCambios, EstadoPlan.INICIADO,
                planOrigen.getFechaInicioVigencia(), true, null);
    }

    public static PlanAlimentarioPersonalizado reconstruir(Integer id, Integer idPlanBase, String descripcionCambios,
                                                             EstadoPlan estadoPlan, LocalDate fechaInicioVigencia,
                                                             boolean activo, List<Dia> dias) {
        return new PlanAlimentarioPersonalizado(id, idPlanBase, descripcionCambios, estadoPlan, fechaInicioVigencia, activo, dias);
    }

    private void validarPersonalizado() {
        if (idPlanBase == null) {
            throw new DomainValidationException("Un plan personalizado debe referenciar a un plan de origen (idPlanBase)");
        }
    }

    public void actualizarDescripcionCambios(String descripcionCambios) {
        asegurarEditable();
        this.descripcionCambios = descripcionCambios;
    }

    public Integer getIdPlanBase() {
        return idPlanBase;
    }

    public String getDescripcionCambios() {
        return descripcionCambios;
    }
}
