package com.nutrition.msnutritionplanner.domain.ingrediente;

import java.util.List;
import java.util.Optional;

/**
 * Puerto (interfaz) de persistencia del agregado Ingrediente.
 * La implementacion concreta vive en la capa de infraestructura.
 */
public interface IngredienteRepository {

    Ingrediente guardar(Ingrediente ingrediente);

    Optional<Ingrediente> buscarPorId(Integer id);

    List<Ingrediente> listarActivos();

    List<Ingrediente> buscarPorIds(List<Integer> ids);

    boolean existePorId(Integer id);
}
