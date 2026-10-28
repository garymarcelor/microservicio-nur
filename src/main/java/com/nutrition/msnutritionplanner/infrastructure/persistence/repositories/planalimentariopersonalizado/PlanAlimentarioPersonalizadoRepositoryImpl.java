package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.planalimentariopersonalizado;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.planbase.Dia;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizado;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizadoRepository;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.PlanAlimentarioJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.planalimentariobase.PlanAlimentarioJpaRepository;
import com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.planalimentariobase.PlanAlimentarioMapper;
import com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.receta.RecetaJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class PlanAlimentarioPersonalizadoRepositoryImpl implements PlanAlimentarioPersonalizadoRepository {

    private final PlanAlimentarioJpaRepository jpaRepository;
    private final RecetaJpaRepository recetaJpaRepository;
    private final PlanAlimentarioMapper mapper;

    public PlanAlimentarioPersonalizadoRepositoryImpl(PlanAlimentarioJpaRepository jpaRepository,
                                                        RecetaJpaRepository recetaJpaRepository,
                                                        PlanAlimentarioMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.recetaJpaRepository = recetaJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PlanAlimentarioPersonalizado guardar(PlanAlimentarioPersonalizado plan) {
        PlanAlimentarioJpaEntity entity = plan.getId() != null
                ? jpaRepository.findById(plan.getId()).orElseGet(PlanAlimentarioJpaEntity::new)
                : new PlanAlimentarioJpaEntity();

        PlanAlimentarioJpaEntity planBaseEntity = jpaRepository.findById(plan.getIdPlanBase())
                .orElseThrow(() -> new EntityNotFoundException("PlanAlimentarioBase", plan.getIdPlanBase()));

        mapper.copyScalarFieldsPersonalizado(plan, entity, planBaseEntity);

        Map<Integer, RecetaJpaEntity> recetasPorId = resolverRecetas(plan.getDias());
        mapper.mergeDias(entity, plan.getDias(), recetasPorId);

        PlanAlimentarioJpaEntity guardado = jpaRepository.save(entity);
        return mapper.toDomainPersonalizado(guardado);
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
            throw new DomainValidationException("Una o mas recetas asignadas a un tiempo de comida no existen o estan dadas de baja");
        }
        return recetas.stream().collect(Collectors.toMap(RecetaJpaEntity::getId, Function.identity()));
    }

    @Override
    public Optional<PlanAlimentarioPersonalizado> buscarPorId(Integer id) {
        return jpaRepository.findById(id)
                .filter(e -> e.getPlanBase() != null)
                .map(mapper::toDomainPersonalizado);
    }

    @Override
    public List<PlanAlimentarioPersonalizado> listarActivosPorPlanBase(Integer idPlanBase) {
        return jpaRepository.findByActivoTrueAndPlanBase_Id(idPlanBase).stream()
                .map(mapper::toDomainPersonalizado)
                .toList();
    }

    @Override
    public boolean existePorId(Integer id) {
        return jpaRepository.existsByIdAndActivoTrueAndPlanBaseIsNotNull(id);
    }
}
