package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.planalimentariobase;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.planbase.Dia;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;
import com.nutrition.msnutritionplanner.domain.shared.Hora;
import com.nutrition.msnutritionplanner.domain.shared.Orden;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.PlanAlimentarioJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.receta.RecetaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanAlimentarioBaseRepositoryImpl")
class PlanAlimentarioBaseRepositoryImplTest {

    @Mock
    private PlanAlimentarioJpaRepository jpaRepository;

    @Mock
    private RecetaJpaRepository recetaJpaRepository;

    @Mock
    private PlanAlimentarioMapper mapper;

    private PlanAlimentarioBaseRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new PlanAlimentarioBaseRepositoryImpl(jpaRepository, recetaJpaRepository, mapper);
    }

    @Test
    @DisplayName("guardar() sin recetas en dias guarda el plan base")
    void guardarSinRecetas() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>());
        PlanAlimentarioJpaEntity entity = new PlanAlimentarioJpaEntity();
        entity.setId(1);

        when(jpaRepository.findById(1)).thenReturn(Optional.of(entity));
        doNothing().when(mapper).copyScalarFieldsBase(plan, entity);
        doNothing().when(mapper).mergeDias(eq(entity), eq(plan.getDias()), any());
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomainBase(entity)).thenReturn(plan);

        PlanAlimentarioBase res = repository.guardar(plan);

        assertThat(res.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("guardar() con recetas valida existencia en BD")
    void guardarConRecetas() {
        TiempoDeComida t1 = TiempoDeComida.reconstruir(100, Hora.of(LocalTime.of(8, 0)), List.of(10));
        Dia d1 = Dia.reconstruir(1000, Orden.of(1), new ArrayList<>(List.of(t1)));
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>(List.of(d1)));

        PlanAlimentarioJpaEntity entity = new PlanAlimentarioJpaEntity();
        RecetaJpaEntity recetaJpa = new RecetaJpaEntity();
        recetaJpa.setId(10);

        when(jpaRepository.findById(1)).thenReturn(Optional.of(entity));
        when(recetaJpaRepository.findByIdInAndActivoTrue(List.of(10))).thenReturn(List.of(recetaJpa));
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomainBase(entity)).thenReturn(plan);

        PlanAlimentarioBase res = repository.guardar(plan);

        assertThat(res.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("guardar() con recetas inexistentes lanza DomainValidationException")
    void guardarRecetasInexistentes() {
        TiempoDeComida t1 = TiempoDeComida.reconstruir(100, Hora.of(LocalTime.of(8, 0)), List.of(99));
        Dia d1 = Dia.reconstruir(1000, Orden.of(1), new ArrayList<>(List.of(t1)));
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>(List.of(d1)));

        PlanAlimentarioJpaEntity entity = new PlanAlimentarioJpaEntity();
        when(jpaRepository.findById(1)).thenReturn(Optional.of(entity));
        when(recetaJpaRepository.findByIdInAndActivoTrue(List.of(99))).thenReturn(List.of());

        assertThatThrownBy(() -> repository.guardar(plan))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("buscarPorId() retorna plan base solo si planBase es null")
    void buscarPorId() {
        PlanAlimentarioJpaEntity entity = new PlanAlimentarioJpaEntity();
        entity.setId(1);
        entity.setPlanBase(null);
        PlanAlimentarioBase domain = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>());

        when(jpaRepository.findById(1)).thenReturn(Optional.of(entity));
        when(mapper.toDomainBase(entity)).thenReturn(domain);

        Optional<PlanAlimentarioBase> res = repository.buscarPorId(1);

        assertThat(res).isPresent();
    }

    @Test
    @DisplayName("listarActivos() retorna lista de planes base")
    void listarActivos() {
        PlanAlimentarioJpaEntity entity = new PlanAlimentarioJpaEntity();
        PlanAlimentarioBase domain = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>());

        when(jpaRepository.findByActivoTrueAndPlanBaseIsNull()).thenReturn(List.of(entity));
        when(mapper.toDomainBase(entity)).thenReturn(domain);

        List<PlanAlimentarioBase> res = repository.listarActivos();

        assertThat(res).hasSize(1);
    }

    @Test
    @DisplayName("existePorId() verifica existencia")
    void existePorId() {
        when(jpaRepository.existsByIdAndActivoTrueAndPlanBaseIsNull(1)).thenReturn(true);

        assertThat(repository.existePorId(1)).isTrue();
    }
}
