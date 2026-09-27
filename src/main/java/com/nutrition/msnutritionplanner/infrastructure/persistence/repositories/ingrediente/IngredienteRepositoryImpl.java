package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.ingrediente;

import com.nutrition.msnutritionplanner.domain.ingrediente.Ingrediente;
import com.nutrition.msnutritionplanner.domain.ingrediente.IngredienteRepository;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class IngredienteRepositoryImpl implements IngredienteRepository {

    private final IngredienteJpaRepository jpaRepository;
    private final IngredienteMapper mapper;

    public IngredienteRepositoryImpl(IngredienteJpaRepository jpaRepository, IngredienteMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Ingrediente guardar(Ingrediente ingrediente) {
        IngredienteJpaEntity entity;
        if (ingrediente.getId() != null) {
            entity = jpaRepository.findById(ingrediente.getId())
                    .orElseGet(() -> mapper.toJpa(ingrediente));
            mapper.copyToJpa(ingrediente, entity);
        } else {
            entity = mapper.toJpa(ingrediente);
        }
        IngredienteJpaEntity guardado = jpaRepository.save(entity);
        return mapper.toDomain(guardado);
    }

    @Override
    public Optional<Ingrediente> buscarPorId(Integer id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Ingrediente> listarActivos() {
        return jpaRepository.findByActivoTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ingrediente> buscarPorIds(List<Integer> ids) {
        return jpaRepository.findByIdInAndActivoTrue(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existePorId(Integer id) {
        return jpaRepository.existsByIdAndActivoTrue(id);
    }
}
