package com.nutrition.msnutritionplanner.domain.planbase;

import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.shared.Hora;
import com.nutrition.msnutritionplanner.domain.shared.Orden;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entidad interna «Dia», parte del agregado PlanAlimentarioBase.
 * Contiene, en orden, los tiempos de comida programados para ese dia
 * del plan.
 */
public class Dia {

    private Integer id;
    private Orden numOrden;
    private final List<TiempoDeComida> tiemposDeComida = new ArrayList<>();

    protected Dia() {
    }

    Dia(Integer id, Orden numOrden, List<TiempoDeComida> tiemposDeComida) {
        this.id = id;
        this.numOrden = numOrden;
        if (tiemposDeComida != null) {
            this.tiemposDeComida.addAll(tiemposDeComida);
        }
        validar();
    }

    static Dia crear(Orden numOrden) {
        return new Dia(null, numOrden, new ArrayList<>());
    }

    /**
     * Reconstruye una instancia a partir de datos persistidos. Uso exclusivo
     * de la capa de infraestructura (mappers de persistencia).
     */
    public static Dia reconstruir(Integer id, Orden numOrden, List<TiempoDeComida> tiemposDeComida) {
        return new Dia(id, numOrden, tiemposDeComida);
    }

    private void validar() {
        if (numOrden == null) {
            throw new DomainValidationException("El dia debe tener un numero de orden");
        }
    }

    TiempoDeComida agregarTiempoDeComida(Hora hora, List<Integer> recetasIds) {
        boolean horaDuplicada = tiemposDeComida.stream().anyMatch(t -> t.getHora().equals(hora));
        if (horaDuplicada) {
            throw new BusinessRuleException("Ya existe un tiempo de comida programado a las " + hora + " para este dia");
        }
        TiempoDeComida tiempo = TiempoDeComida.crear(hora, recetasIds);
        tiemposDeComida.add(tiempo);
        return tiempo;
    }

    void quitarTiempoDeComida(Integer idTiempo) {
        if (tiemposDeComida.size() <= 1) {
            throw new BusinessRuleException("El dia debe conservar al menos un tiempo de comida");
        }
        boolean removed = tiemposDeComida.removeIf(t -> t.getId() != null && t.getId().equals(idTiempo));
        if (!removed) {
            throw new DomainValidationException("El tiempo de comida indicado no pertenece a este dia");
        }
    }

    TiempoDeComida obtenerTiempoDeComida(Integer idTiempo) {
        return tiemposDeComida.stream()
                .filter(t -> t.getId() != null && t.getId().equals(idTiempo))
                .findFirst()
                .orElseThrow(() -> new DomainValidationException("El tiempo de comida indicado no pertenece a este dia"));
    }

    void reordenar(Orden nuevoOrden) {
        if (nuevoOrden == null) {
            throw new DomainValidationException("El numero de orden es obligatorio");
        }
        this.numOrden = nuevoOrden;
    }

    public Integer getId() {
        return id;
    }

    public Orden getNumOrden() {
        return numOrden;
    }

    public int getNroTiemposComida() {
        return tiemposDeComida.size();
    }

    public List<TiempoDeComida> getTiemposDeComida() {
        return Collections.unmodifiableList(tiemposDeComida);
    }
}
