package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.planalimentariobase;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.planbase.Dia;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBaseRepository;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.PlanAlimentarioJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.receta.RecetaJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class PlanAlimentarioBaseRepositoryImpl implements PlanAlimentarioBaseRepository {

    private final PlanAlimentarioJpaRepository jpaRepository;
    private final RecetaJpaRepository recetaJpaRepository;
    private final PlanAlimentarioMapper mapper;

    public PlanAlimentarioBaseRepositoryImpl(PlanAlimentarioJpaRepository jpaRepository,
            RecetaJpaRepository recetaJpaRepository,
            PlanAlimentarioMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.recetaJpaRepository = recetaJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PlanAlimentarioBase guardar(PlanAlimentarioBase plan) {
        PlanAlimentarioJpaEntity entity = plan.getId() != null
                ? jpaRepository.findById(plan.getId()).orElseGet(PlanAlimentarioJpaEntity::new)
                : new PlanAlimentarioJpaEntity();

        mapper.copyScalarFieldsBase(plan, entity);

        Map<Integer, RecetaJpaEntity> recetasPorId = resolverRecetas(plan.getDias());
        mapper.mergeDias(entity, plan.getDias(), recetasPorId);

        PlanAlimentarioJpaEntity guardado = jpaRepository.save(entity);
        return mapper.toDomainBase(guardado);
    }

    private Map<Integer, RecetaJpaEntity> resolverRecetas(List<Dia> dias) {
        List<Integer> idsRecetas = dias.stream()
                .flatMap(d -> d.getTiemposDeComida().stream())
                .flatMap(t -> t.getRecetasIds().stream())
                .distinct()
                .toList();

        if (idsRecetas.isEmpty()) {
            return Map.of();
        }

        List<RecetaJpaEntity> recetas = recetaJpaRepository.findByIdInAndActivoTrue(idsRecetas);
        if (recetas.size() != idsRecetas.size()) {
            throw new DomainValidationException(
                    "Una o mas recetas asignadas a un tiempo de comida no existen o estan dadas de baja");
        }
        return recetas.stream().collect(Collectors.toMap(RecetaJpaEntity::getId, Function.identity()));
    }

    @Override
    public Optional<PlanAlimentarioBase> buscarPorId(Integer id) {
        return jpaRepository.findById(id)
                .filter(e -> e.getPlanBase() == null)
                .map(mapper::toDomainBase);
    }

    @Override
    public List<PlanAlimentarioBase> listarActivos() {
        return jpaRepository.findByActivoTrueAndPlanBaseIsNull().stream().map(mapper::toDomainBase).toList();
    }

    @Override
    public boolean existePorId(Integer id) {
        return jpaRepository.existsByIdAndActivoTrueAndPlanBaseIsNull(id);
    }
}
