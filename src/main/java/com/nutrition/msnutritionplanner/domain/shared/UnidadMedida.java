package com.nutrition.msnutritionplanner.domain.shared;

/**
 * Unidad de medida en la que se expresa la cantidad de un ingrediente
 * dentro de una receta (por porcion).
 *
 * Se agrego para cumplir el contrato de integracion bc2.RecetaCreada
 * (TareaActividad2-tabla-corregida.md), donde cada ingrediente viaja con
 * "cantidad" y "unidad" (p. ej. 180 GRAMOS, 10 MILILITROS).
 */
public enum UnidadMedida {
    GRAMOS,
    MILILITROS,
    UNIDADES
}
