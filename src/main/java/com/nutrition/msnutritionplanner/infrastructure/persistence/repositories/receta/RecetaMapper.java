package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.receta;

import com.nutrition.msnutritionplanner.domain.receta.IngredienteCantidad;
import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaIngredienteJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapeo Receta (dominio) <-> RecetaJpaEntity.
 *
 * Actividad 2: el mapeo pasa a ser MANUAL (antes ModelMapper). Motivo: la
 * receta ahora tiene {@code porcionGramos} y una coleccion de ingredientes
 * con cantidad/unidad. En el dominio es un Map y en la entidad una lista de
 * filas puente; dejar que ModelMapper las emparejara por nombre
 * (ambas se llaman "ingredientes") podria mezclar tipos incompatibles.
 */
@Component
public class RecetaMapper {

    /** Convierte la entidad JPA (con sus filas puente ya cargadas) al agregado de dominio. */
    public Receta toDomain(RecetaJpaEntity entity) {
        List<IngredienteCantidad> ingredientes = entity.getIngredientes().stream()
                .map(f -> new IngredienteCantidad(f.getIngrediente().getId(), f.getCantidad(), f.getUnidad()))
                .toList();
        return Receta.reconstruir(entity.getId(), entity.getNombre(), entity.getInstruccion(),
                entity.getPorcionGramos(), entity.isActivo(), ingredientes);
    }

    /** Copia los campos escalares del dominio hacia la entidad JPA destino. */
    public void copyScalarFields(Receta receta, RecetaJpaEntity destino) {
        destino.setNombre(receta.getNombre());
        destino.setInstruccion(receta.getInstruccion());
        destino.setPorcionGramos(receta.getPorcionGramos());
        destino.setActivo(receta.isActivo());
    }

    /**
     * Sincroniza las filas puente de la entidad con los ingredientes del
     * dominio: actualiza cantidad/unidad de las existentes, agrega las nuevas
     * y elimina (orphanRemoval) las que ya no estan.
     *
     * @param ingredientesPorId entidades Ingrediente ya resueltas desde la BD
     */
    public void mergeIngredientes(RecetaJpaEntity destino, List<IngredienteCantidad> ingredientes,
                                  Map<Integer, IngredienteJpaEntity> ingredientesPorId) {
        Map<Integer, RecetaIngredienteJpaEntity> existentes = new HashMap<>();
        for (RecetaIngredienteJpaEntity fila : destino.getIngredientes()) {
            existentes.put(fila.getIngrediente().getId(), fila);
        }

        List<RecetaIngredienteJpaEntity> nuevaLista = new java.util.ArrayList<>();
        for (IngredienteCantidad ic : ingredientes) {
            RecetaIngredienteJpaEntity fila = existentes.get(ic.idIngrediente());
            if (fila == null) {
                fila = new RecetaIngredienteJpaEntity();
                fila.setReceta(destino);
                fila.setIngrediente(ingredientesPorId.get(ic.idIngrediente()));
            }
            fila.setCantidad(ic.cantidad());
            fila.setUnidad(ic.unidad());
            nuevaLista.add(fila);
        }
        destino.getIngredientes().clear();
        destino.getIngredientes().addAll(nuevaLista);
    }
}
