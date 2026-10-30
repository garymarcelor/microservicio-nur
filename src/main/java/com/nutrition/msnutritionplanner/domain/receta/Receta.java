package com.nutrition.msnutritionplanner.domain.receta;

import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.shared.AggregateRoot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agregado Raiz «Receta».
 *
 * Una receta pertenece a su propio agregado y solo referencia a los
 * agregados Ingrediente por identidad (idIngrediente), respetando el
 * limite de consistencia entre agregados que exige DDD.
 *
 * EXTENSION (Actividad 2 - eventos de integracion): para cumplir el contrato
 * bc2.RecetaCreada la receta ahora conoce:
 *  - porcionGramos: peso de UNA porcion servida (tambien viaja en los
 *    eventos bc2.PlanBaseCreado / bc2.PlanPersonalizadoCreado).
 *  - por cada ingrediente, su cantidad y unidad por porcion
 *    (Value Object {@link IngredienteCantidad}).
 */
public class Receta extends AggregateRoot<Integer> {

    private String nombre;
    private String instruccion;
    private Integer porcionGramos;
    private boolean activo;
    /** Ingredientes indexados por id (mantiene el orden de insercion y evita duplicados). */
    private final Map<Integer, IngredienteCantidad> ingredientes = new LinkedHashMap<>();

    protected Receta() {
        super(null);
    }

    private Receta(Integer id, String nombre, String instruccion, Integer porcionGramos,
                   boolean activo, List<IngredienteCantidad> ingredientes) {
        super(id);
        this.nombre = nombre;
        this.instruccion = instruccion;
        this.porcionGramos = porcionGramos;
        this.activo = activo;
        if (ingredientes != null) {
            ingredientes.forEach(i -> this.ingredientes.put(i.idIngrediente(), i));
        }
        validar();
    }

    /** Crea una receta nueva (activa) con su porcion y la cantidad de cada ingrediente. */
    public static Receta crear(String nombre, String instruccion, Integer porcionGramos,
                               List<IngredienteCantidad> ingredientes) {
        if (ingredientes == null || ingredientes.isEmpty()) {
            throw new DomainValidationException("Una receta debe tener al menos un ingrediente");
        }
        return new Receta(null, nombre, instruccion, porcionGramos, true, ingredientes);
    }

    /** Reconstruye la receta desde persistencia (no aplica reglas de creacion). */
    public static Receta reconstruir(Integer id, String nombre, String instruccion, Integer porcionGramos,
                                     boolean activo, List<IngredienteCantidad> ingredientes) {
        return new Receta(id, nombre, instruccion, porcionGramos, activo, ingredientes);
    }

    private void validar() {
        if (nombre == null || nombre.isBlank()) {
            throw new DomainValidationException("El nombre de la receta es obligatorio");
        }
        if (instruccion == null || instruccion.isBlank()) {
            throw new DomainValidationException("La instruccion de la receta es obligatoria");
        }
        if (porcionGramos == null || porcionGramos <= 0) {
            throw new DomainValidationException("La porcion en gramos de la receta debe ser mayor a cero");
        }
    }

    public void actualizarDatos(String nombre, String instruccion, Integer porcionGramos) {
        asegurarActiva();
        this.nombre = nombre;
        this.instruccion = instruccion;
        this.porcionGramos = porcionGramos;
        validar();
    }

    /**
     * Agrega un ingrediente o, si ya existe, actualiza su cantidad/unidad
     * (una receta no puede tener el mismo ingrediente dos veces).
     */
    public void agregarIngrediente(IngredienteCantidad ingrediente) {
        asegurarActiva();
        if (ingrediente == null) {
            throw new DomainValidationException("El ingrediente es obligatorio");
        }
        ingredientes.put(ingrediente.idIngrediente(), ingrediente);
    }

    public void quitarIngrediente(Integer idIngrediente) {
        asegurarActiva();
        if (ingredientes.size() <= 1) {
            throw new BusinessRuleException("Una receta debe conservar al menos un ingrediente");
        }
        ingredientes.remove(idIngrediente);
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

    public Integer getPorcionGramos() {
        return porcionGramos;
    }

    public boolean isActivo() {
        return activo;
    }

    /** Ingredientes con su cantidad y unidad por porcion. */
    public List<IngredienteCantidad> getIngredientes() {
        return Collections.unmodifiableList(new ArrayList<>(ingredientes.values()));
    }

    /** Solo los ids de los ingredientes (referencia por identidad). */
    public List<Integer> getIngredientesIds() {
        return List.copyOf(ingredientes.keySet());
    }
}
