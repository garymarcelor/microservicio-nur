package com.nutrition.msnutritionplanner.domain.planbase;

import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.shared.Hora;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Entidad interna «TiempoDeComida», parte del agregado PlanAlimentarioBase.
 * No se manipula directamente desde fuera del agregado: siempre a traves
 * de {@link Dia} / {@link PlanAlimentarioBase}.
 *
 * Referencia al agregado Receta unicamente por identidad (idsReceta), tal
 * como exige el limite de consistencia entre agregados en DDD.
 */
public class TiempoDeComida {

    private Integer id;
    private Hora hora;
    private final Set<Integer> recetasIds = new LinkedHashSet<>();

    protected TiempoDeComida() {
    }

    TiempoDeComida(Integer id, Hora hora, List<Integer> recetasIds) {
        this.id = id;
        this.hora = hora;
        if (recetasIds != null) {
            this.recetasIds.addAll(recetasIds);
        }
        validar();
    }

    static TiempoDeComida crear(Hora hora, List<Integer> recetasIds) {
        if (recetasIds == null || recetasIds.isEmpty()) {
            throw new DomainValidationException("Un tiempo de comida debe tener al menos una receta asignada");
        }
        return new TiempoDeComida(null, hora, recetasIds);
    }

    /**
     * Reconstruye una instancia a partir de datos persistidos. Uso exclusivo
     * de la capa de infraestructura (mappers de persistencia).
     */
    public static TiempoDeComida reconstruir(Integer id, Hora hora, List<Integer> recetasIds) {
        return new TiempoDeComida(id, hora, recetasIds);
    }

    private void validar() {
        if (hora == null) {
            throw new DomainValidationException("La hora del tiempo de comida es obligatoria");
        }
    }

    void agregarReceta(Integer idReceta) {
        if (idReceta == null) {
            throw new DomainValidationException("El id de la receta es obligatorio");
        }
        recetasIds.add(idReceta);
    }

    void quitarReceta(Integer idReceta) {
        if (recetasIds.size() <= 1) {
            throw new BusinessRuleException("Un tiempo de comida debe conservar al menos una receta");
        }
        recetasIds.remove(idReceta);
    }

    void reemplazarHora(Hora nuevaHora) {
        if (nuevaHora == null) {
            throw new DomainValidationException("La hora del tiempo de comida es obligatoria");
        }
        this.hora = nuevaHora;
    }

    public Integer getId() {
        return id;
    }

    public Hora getHora() {
        return hora;
    }

    public List<Integer> getRecetasIds() {
        return Collections.unmodifiableList(new ArrayList<>(recetasIds));
    }
}
