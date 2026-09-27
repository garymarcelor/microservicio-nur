package com.nutrition.msnutritionplanner.domain.receta;

import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.shared.AggregateRoot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Agregado Raiz «Receta».
 *
 * Una receta pertenece a su propio agregado y solo referencia a los
 * agregados Ingrediente por identidad (idIngrediente), respetando el
 * limite de consistencia entre agregados que exige DDD.
 */
public class Receta extends AggregateRoot<Integer> {

    private String nombre;
    private String instruccion;
    private boolean activo;
    private final Set<Integer> ingredientesIds = new LinkedHashSet<>();

    protected Receta() {
        super(null);
    }

    private Receta(Integer id, String nombre, String instruccion, boolean activo, List<Integer> ingredientesIds) {
        super(id);
        this.nombre = nombre;
        this.instruccion = instruccion;
        this.activo = activo;
        if (ingredientesIds != null) {
            this.ingredientesIds.addAll(ingredientesIds);
        }
        validar();
    }

    public static Receta crear(String nombre, String instruccion, List<Integer> ingredientesIds) {
        if (ingredientesIds == null || ingredientesIds.isEmpty()) {
            throw new DomainValidationException("Una receta debe tener al menos un ingrediente");
        }
        return new Receta(null, nombre, instruccion, true, ingredientesIds);
    }

    public static Receta reconstruir(Integer id, String nombre, String instruccion,
                                      boolean activo, List<Integer> ingredientesIds) {
        return new Receta(id, nombre, instruccion, activo, ingredientesIds);
    }

    private void validar() {
        if (nombre == null || nombre.isBlank()) {
            throw new DomainValidationException("El nombre de la receta es obligatorio");
        }
        if (instruccion == null || instruccion.isBlank()) {
            throw new DomainValidationException("La instruccion de la receta es obligatoria");
        }
    }

    public void actualizarDatos(String nombre, String instruccion) {
        asegurarActiva();
        this.nombre = nombre;
        this.instruccion = instruccion;
        validar();
    }

    public void agregarIngrediente(Integer idIngrediente) {
        asegurarActiva();
        if (idIngrediente == null) {
            throw new DomainValidationException("El id del ingrediente es obligatorio");
        }
        ingredientesIds.add(idIngrediente);
    }

    public void quitarIngrediente(Integer idIngrediente) {
        asegurarActiva();
        if (ingredientesIds.size() <= 1) {
            throw new BusinessRuleException("Una receta debe conservar al menos un ingrediente");
        }
        ingredientesIds.remove(idIngrediente);
    }

    public void darDeBaja() {
        this.activo = false;
    }

    public void reactivar() {
        this.activo = true;
    }

    private void asegurarActiva() {
        if (!activo) {
            throw new BusinessRuleException("No se puede modificar una receta dada de baja");
        }
    }

    public String getNombre() {
        return nombre;
    }

    public String getInstruccion() {
        return instruccion;
    }

    public boolean isActivo() {
        return activo;
    }

    public List<Integer> getIngredientesIds() {
        return Collections.unmodifiableList(new ArrayList<>(ingredientesIds));
    }
}
