package com.nutrition.msnutritionplanner.domain.ingrediente;

import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.shared.AggregateRoot;
import com.nutrition.msnutritionplanner.domain.shared.TipoIngrediente;

/**
 * Agregado Raiz «Ingrediente».
 *
 * IMPORTANTE: los campos son privados y NO se exponen setters. El mapeo
 * hacia/desde la entidad de persistencia se realiza por acceso directo a
 * campos via ModelMapper (ver infrastructure.shared.ModelMapperConfig),
 * nunca por getters/setters de esta clase.
 */
public class Ingrediente extends AggregateRoot<Integer> {

    private String nombre;
    private String descripcion;
    private TipoIngrediente tipo;
    private boolean activo;

    /** Constructor reservado para el mapper de infraestructura (reflexion sobre campos). */
    protected Ingrediente() {
        super(null);
    }

    private Ingrediente(Integer id, String nombre, String descripcion, TipoIngrediente tipo, boolean activo) {
        super(id);
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.activo = activo;
        validar();
    }

    public static Ingrediente crear(String nombre, String descripcion, TipoIngrediente tipo) {
        return new Ingrediente(null, nombre, descripcion, tipo, true);
    }

    public static Ingrediente reconstruir(Integer id, String nombre, String descripcion,
                                           TipoIngrediente tipo, boolean activo) {
        return new Ingrediente(id, nombre, descripcion, tipo, activo);
    }

    private void validar() {
        if (nombre == null || nombre.isBlank()) {
            throw new DomainValidationException("El nombre del ingrediente es obligatorio");
        }
        if (tipo == null) {
            throw new DomainValidationException("El tipo del ingrediente es obligatorio");
        }
    }

    public void actualizarDatos(String nombre, String descripcion, TipoIngrediente tipo) {
        if (!activo) {
            throw new BusinessRuleException("No se puede modificar un ingrediente dado de baja");
        }
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        validar();
    }

    public void darDeBaja() {
        this.activo = false;
    }

    public void reactivar() {
        this.activo = true;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public TipoIngrediente getTipo() {
        return tipo;
    }

    public boolean isActivo() {
        return activo;
    }
}
