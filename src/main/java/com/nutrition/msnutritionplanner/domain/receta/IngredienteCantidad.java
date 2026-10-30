package com.nutrition.msnutritionplanner.domain.receta;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.shared.UnidadMedida;
import com.nutrition.msnutritionplanner.domain.shared.ValueObject;

import java.math.BigDecimal;

/**
 * Value Object «IngredienteCantidad»: cuanto de un ingrediente (referenciado
 * por identidad) lleva UNA porcion de la receta.
 *
 * Es inmutable y se valida al construirse. Se agrego para que la receta
 * pueda publicar el evento bc2.RecetaCreada con la cantidad y unidad de cada
 * ingrediente, que es lo que cocina (BC4) necesita para producir.
 */
public record IngredienteCantidad(Integer idIngrediente, BigDecimal cantidad, UnidadMedida unidad) implements ValueObject {

    public IngredienteCantidad {
        if (idIngrediente == null) {
            throw new DomainValidationException("El id del ingrediente es obligatorio");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new DomainValidationException("La cantidad del ingrediente debe ser mayor a cero");
        }
        if (unidad == null) {
            throw new DomainValidationException("La unidad de medida del ingrediente es obligatoria");
        }
    }
}
