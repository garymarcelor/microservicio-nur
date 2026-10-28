package com.nutrition.msnutritionplanner.domain.planbase;

import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.shared.AggregateRoot;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;
import com.nutrition.msnutritionplanner.domain.shared.Hora;
import com.nutrition.msnutritionplanner.domain.shared.Orden;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agregado Raiz «PlanAlimentarioBase».
 *
 * Corresponde al plan de alimentacion de 15 dias elaborado por el
 * profesional en nutricion en base a las necesidades del paciente
 * (servicio de Asesoramiento Nutricional). Es la raiz de consistencia
 * para sus Dias y TiemposDeComida.
 */
public class PlanAlimentarioBase extends AggregateRoot<Integer> {

    protected EstadoPlan estadoPlan;
    protected LocalDate fechaInicioVigencia;
    protected boolean activo;
    protected final List<Dia> dias = new ArrayList<>();

    protected PlanAlimentarioBase() {
        super(null);
    }

    protected PlanAlimentarioBase(Integer id, EstadoPlan estadoPlan, LocalDate fechaInicioVigencia,
                                   boolean activo, List<Dia> dias) {
        super(id);
        this.estadoPlan = estadoPlan;
        this.fechaInicioVigencia = fechaInicioVigencia;
        this.activo = activo;
        if (dias != null) {
            this.dias.addAll(dias);
        }
        validar();
    }

    public static PlanAlimentarioBase crear(LocalDate fechaInicioVigencia) {
        return new PlanAlimentarioBase(null, EstadoPlan.INICIADO, fechaInicioVigencia, true, new ArrayList<>());
    }

    public static PlanAlimentarioBase reconstruir(Integer id, EstadoPlan estadoPlan,
                                                    LocalDate fechaInicioVigencia, boolean activo, List<Dia> dias) {
        return new PlanAlimentarioBase(id, estadoPlan, fechaInicioVigencia, activo, dias);
    }

    protected void validar() {
        if (estadoPlan == null) {
            throw new DomainValidationException("El estado del plan es obligatorio");
        }
        if (fechaInicioVigencia == null) {
            throw new DomainValidationException("La fecha de inicio de vigencia es obligatoria");
        }
    }

    public Dia agregarDia() {
        asegurarEditable();
        Orden siguienteOrden = Orden.of(dias.size() + 1);
        Dia dia = Dia.crear(siguienteOrden);
        dias.add(dia);
        return dia;
    }

    public void agregarTiempoDeComida(Integer idDia, Hora hora, List<Integer> recetasIds) {
        asegurarEditable();
        Dia dia = obtenerDia(idDia);
        dia.agregarTiempoDeComida(hora, recetasIds);
    }

    public void quitarTiempoDeComida(Integer idDia, Integer idTiempo) {
        asegurarEditable();
        Dia dia = obtenerDia(idDia);
        dia.quitarTiempoDeComida(idTiempo);
    }

    public void quitarDia(Integer idDia) {
        asegurarEditable();
        if (dias.size() <= 1) {
            throw new BusinessRuleException("El plan debe conservar al menos un dia");
        }
        boolean removed = dias.removeIf(d -> d.getId() != null && d.getId().equals(idDia));
        if (!removed) {
            throw new DomainValidationException("El dia indicado no pertenece a este plan");
        }
        reordenarDias();
    }

    private void reordenarDias() {
        for (int i = 0; i < dias.size(); i++) {
            dias.get(i).reordenar(Orden.of(i + 1));
        }
    }

    public Dia obtenerDia(Integer idDia) {
        return dias.stream()
                .filter(d -> d.getId() != null && d.getId().equals(idDia))
                .findFirst()
                .orElseThrow(() -> new DomainValidationException("El dia indicado no pertenece a este plan"));
    }

    public void completar() {
        if (estadoPlan != EstadoPlan.INICIADO) {
            throw new BusinessRuleException("Solo un plan INICIADO puede marcarse como COMPLETO");
        }
        if (dias.isEmpty()) {
            throw new BusinessRuleException("El plan debe tener al menos un dia para poder completarse");
        }
        this.estadoPlan = EstadoPlan.COMPLETO;
    }

    public void descartar() {
        if (estadoPlan == EstadoPlan.DESCARTADO) {
            throw new BusinessRuleException("El plan ya se encuentra descartado");
        }
        this.estadoPlan = EstadoPlan.DESCARTADO;
    }

    protected void asegurarEditable() {
        if (!activo) {
            throw new BusinessRuleException("No se puede modificar un plan dado de baja");
        }
        if (estadoPlan != EstadoPlan.INICIADO) {
            throw new BusinessRuleException("Solo se puede modificar un plan en estado INICIADO");
        }
    }

    public void darDeBaja() {
        this.activo = false;
    }

    public void reactivar() {
        this.activo = true;
    }

    public boolean isActivo() {
        return activo;
    }

    public EstadoPlan getEstadoPlan() {
        return estadoPlan;
    }

    public LocalDate getFechaInicioVigencia() {
        return fechaInicioVigencia;
    }

    public List<Dia> getDias() {
        return Collections.unmodifiableList(dias);
    }
}
