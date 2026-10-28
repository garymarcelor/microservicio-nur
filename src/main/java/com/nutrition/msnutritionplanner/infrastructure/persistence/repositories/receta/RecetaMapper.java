package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.receta;

import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapeo Receta (dominio) &lt;-&gt; RecetaJpaEntity.
 * Los campos escalares (id, nombre, instruccion, activo) se mapean con
 * ModelMapper por acceso directo a campos. La asociacion hacia el
 * agregado Ingrediente (por identidad) se resuelve de forma manual,
 * ya que el dominio solo conserva los ids y la entidad JPA conserva
 * las referencias completas (@ManyToMany).
 */
@Component
public class RecetaMapper {

    private final ModelMapper modelMapper;

    public RecetaMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    /**
     * Convierte la entidad JPA (ya con sus ingredientes cargados) al
     * agregado de dominio.
     */
    public Receta toDomain(RecetaJpaEntity entity) {
        List<Integer> ingredientesIds = entity.getIngredientes().stream()
                .map(IngredienteJpaEntity::getId)
                .toList();
        return Receta.reconstruir(entity.getId(), entity.getNombre(), entity.getInstruccion(),
                entity.isActivo(), ingredientesIds);
    }

    /**
     * Copia los campos escalares del dominio hacia la entidad JPA destino.
     * La lista de ingredientes debe resolverse aparte (requiere consultar
     * IngredienteJpaRepository), por eso no se incluye aqui.
     */
    public void copyScalarFields(Receta receta, RecetaJpaEntity destino) {
        modelMapper.map(receta, destino);
    }
}
