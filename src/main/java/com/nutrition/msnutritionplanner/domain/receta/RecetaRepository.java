package com.nutrition.msnutritionplanner.domain.receta;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado Receta.
 */
public interface RecetaRepository {

    Receta guardar(Receta receta);

    Optional<Receta> buscarPorId(Integer id);

    List<Receta> listarActivas();

    List<Receta> buscarPorIds(List<Integer> ids);

    boolean existePorId(Integer id);
}
