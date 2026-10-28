package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.ingrediente;

import com.nutrition.msnutritionplanner.domain.ingrediente.Ingrediente;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * Mapeo Ingrediente (dominio) &lt;-&gt; IngredienteJpaEntity (persistencia).
 * Se realiza mediante ModelMapper con acceso directo a campos privados
 * (ver ModelMapperConfig), sin depender de getters/setters de la entidad
 * de dominio.
 */
@Component
public class IngredienteMapper {

    private final ModelMapper modelMapper;

    public IngredienteMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public IngredienteJpaEntity toJpa(Ingrediente ingrediente) {
        return modelMapper.map(ingrediente, IngredienteJpaEntity.class);
    }

    public void copyToJpa(Ingrediente ingrediente, IngredienteJpaEntity destino) {
        modelMapper.map(ingrediente, destino);
    }

    public Ingrediente toDomain(IngredienteJpaEntity entity) {
        return modelMapper.map(entity, Ingrediente.class);
    }
}
