package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.planalimentariopersonalizado;

import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizado;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.PlanAlimentarioJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.planalimentariobase.PlanAlimentarioJpaRepository;
import com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.planalimentariobase.PlanAlimentarioMapper;
import com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.receta.RecetaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanAlimentarioPersonalizadoRepositoryImpl")
class PlanAlimentarioPersonalizadoRepositoryImplTest {

    @Mock
    private PlanAlimentarioJpaRepository jpaRepository;

    @Mock
    private RecetaJpaRepository recetaJpaRepository;

    @Mock
    private PlanAlimentarioMapper mapper;

    private PlanAlimentarioPersonalizadoRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new PlanAlimentarioPersonalizadoRepositoryImpl(jpaRepository, recetaJpaRepository, mapper);
    }

    @Test
    @DisplayName("guardar() persiste y retorna el plan personalizado")
    void guardar() {
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "paciente-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>());
        PlanAlimentarioJpaEntity entity = new PlanAlimentarioJpaEntity();
        PlanAlimentarioJpaEntity planBaseEntity = new PlanAlimentarioJpaEntity();
        planBaseEntity.setId(1);

        when(jpaRepository.findById(10)).thenReturn(Optional.of(entity));
        when(jpaRepository.findById(1)).thenReturn(Optional.of(planBaseEntity));
        doNothing().when(mapper).copyScalarFieldsPersonalizado(plan, entity, planBaseEntity);
        doNothing().when(mapper).mergeDias(eq(entity), eq(plan.getDias()), any());
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomainPersonalizado(entity)).thenReturn(plan);

        PlanAlimentarioPersonalizado res = repository.guardar(plan);

        assertThat(res.getId()).isEqualTo(10);
    }

    @Test
    @DisplayName("guardar() lanza EntityNotFoundException si el plan base no existe")
    void guardarPlanBaseInexistente() {
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 99, "paciente-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>());
        when(jpaRepository.findById(10)).thenReturn(Optional.of(new PlanAlimentarioJpaEntity()));
        when(jpaRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repository.guardar(plan))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("buscarPorId() retorna el plan si planBase no es null")
    void buscarPorId() {
        PlanAlimentarioJpaEntity entity = new PlanAlimentarioJpaEntity();
        PlanAlimentarioJpaEntity planBaseEntity = new PlanAlimentarioJpaEntity();
        planBaseEntity.setId(1);
        entity.setPlanBase(planBaseEntity);

        PlanAlimentarioPersonalizado domain = PlanAlimentarioPersonalizado.reconstruir(10, 1, "paciente-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>());

        when(jpaRepository.findById(10)).thenReturn(Optional.of(entity));
        when(mapper.toDomainPersonalizado(entity)).thenReturn(domain);

        Optional<PlanAlimentarioPersonalizado> res = repository.buscarPorId(10);

        assertThat(res).isPresent();
    }

    @Test
    @DisplayName("listarActivosPorPlanBase() retorna lista de planes personalizados")
    void listarActivosPorPlanBase() {
        PlanAlimentarioJpaEntity entity = new PlanAlimentarioJpaEntity();
        PlanAlimentarioPersonalizado domain = PlanAlimentarioPersonalizado.reconstruir(10, 1, "paciente-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>());

        when(jpaRepository.findByActivoTrueAndPlanBase_Id(1)).thenReturn(List.of(entity));
        when(mapper.toDomainPersonalizado(entity)).thenReturn(domain);

        List<PlanAlimentarioPersonalizado> res = repository.listarActivosPorPlanBase(1);

        assertThat(res).hasSize(1);
    }

    @Test
    @DisplayName("existePorId() verifica existencia de plan personalizado")
    void existePorId() {
        when(jpaRepository.existsByIdAndActivoTrueAndPlanBaseIsNotNull(10)).thenReturn(true);

        assertThat(repository.existePorId(10)).isTrue();
    }
}
