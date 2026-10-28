package com.nutrition.msnutritionplanner.domain.shared;

/**
 * Marca a una entidad de dominio como raiz de agregado.
 * Solo las raices de agregado deben ser accedidas directamente por
 * repositorios / casos de uso; las entidades internas del agregado
 * (por ejemplo Dia, TiempoDeComida) solo se manipulan a traves de su raiz.
 */
public abstract class AggregateRoot<ID> {

    protected ID id;

    protected AggregateRoot(ID id) {
        this.id = id;
    }

    public ID getId() {
        return id;
    }
}
