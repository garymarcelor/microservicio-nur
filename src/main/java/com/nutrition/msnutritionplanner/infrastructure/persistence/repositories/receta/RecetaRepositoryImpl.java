package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.receta;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.domain.receta.RecetaRepository;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.ingrediente.IngredienteJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RecetaRepositoryImpl implements RecetaRepository {

    private final RecetaJpaRepository jpaRepository;
    private final IngredienteJpaRepository ingredienteJpaRepository;
    private final RecetaMapper mapper;

    public RecetaRepositoryImpl(RecetaJpaRepository jpaRepository,
                                 IngredienteJpaRepository ingredienteJpaRepository,
                                 RecetaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.ingredienteJpaRepository = ingredienteJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Receta guardar(Receta receta) {
        RecetaJpaEntity entity = receta.getId() != null
                ? jpaRepository.findById(receta.getId()).orElseGet(RecetaJpaEntity::new)
                : new RecetaJpaEntity();

        mapper.copyScalarFields(receta, entity);

        List<IngredienteJpaEntity> ingredientes = ingredienteJpaRepository.findByIdInAndActivoTrue(receta.getIngredientesIds());
        if (ingredientes.size() != receta.getIngredientesIds().size()) {
            throw new DomainValidationException("Uno o mas ingredientes indicados no existen o estan dados de baja");
        }
        entity.setIngredientes(ingredientes);

        RecetaJpaEntity guardado = jpaRepository.save(entity);
        return mapper.toDomain(guardado);
    }

    @Override
    public Optional<Receta> buscarPorId(Integer id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Receta> listarActivas() {
        return jpaRepository.findByActivoTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Receta> buscarPorIds(List<Integer> ids) {
        return jpaRepository.findByIdInAndActivoTrue(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existePorId(Integer id) {
        return jpaRepository.existsByIdAndActivoTrue(id);
    }
}
