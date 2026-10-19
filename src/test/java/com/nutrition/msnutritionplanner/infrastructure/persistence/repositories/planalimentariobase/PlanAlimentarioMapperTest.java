package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.planalimentariobase;

import com.nutrition.msnutritionplanner.domain.planbase.Dia;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizado;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;

import com.nutrition.msnutritionplanner.domain.shared.Hora;
import com.nutrition.msnutritionplanner.domain.shared.Orden;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.DiaJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.PlanAlimentarioJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.TiempoDeComidaJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlanAlimentarioMapper")
class PlanAlimentarioMapperTest {

    private PlanAlimentarioMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PlanAlimentarioMapper();
    }

    @Test
    @DisplayName("toDomainBase() mapea entidad JPA a PlanAlimentarioBase")
    void toDomainBase() {
        RecetaJpaEntity recetaJpa = new RecetaJpaEntity();
        recetaJpa.setId(10);

        TiempoDeComidaJpaEntity tiempoJpa = new TiempoDeComidaJpaEntity();
        tiempoJpa.setId(100);
        tiempoJpa.setHora(LocalTime.of(8, 0));
        tiempoJpa.setRecetas(List.of(recetaJpa));

        DiaJpaEntity diaJpa = new DiaJpaEntity();
        diaJpa.setId(1000);
        diaJpa.setNumOrder(1);
        diaJpa.setTiemposDeComida(List.of(tiempoJpa));

        PlanAlimentarioJpaEntity entity = new PlanAlimentarioJpaEntity();
        entity.setId(1);
        entity.setEstadoPlan(EstadoPlan.INICIADO);
        entity.setFechaInicioVigencia(LocalDate.now());
        entity.setActivo(true);
        entity.setDias(List.of(diaJpa));

        PlanAlimentarioBase domain = mapper.toDomainBase(entity);

        assertThat(domain.getId()).isEqualTo(1);
        assertThat(domain.getDias()).hasSize(1);
        assertThat(domain.getDias().get(0).getTiemposDeComida().get(0).getRecetasIds()).containsExactly(10);
    }

    @Test
    @DisplayName("toDomainPersonalizado() mapea entidad JPA a PlanAlimentarioPersonalizado")
    void toDomainPersonalizado() {
        PlanAlimentarioJpaEntity planBaseJpa = new PlanAlimentarioJpaEntity();
        planBaseJpa.setId(1);

        PlanAlimentarioJpaEntity entity = new PlanAlimentarioJpaEntity();
        entity.setId(10);
        entity.setPlanBase(planBaseJpa);
        entity.setDescripcionCambios("Modificado");
        entity.setEstadoPlan(EstadoPlan.INICIADO);
        entity.setFechaInicioVigencia(LocalDate.now());
        entity.setActivo(true);
        entity.setDias(new ArrayList<>());

        PlanAlimentarioPersonalizado domain = mapper.toDomainPersonalizado(entity);

        assertThat(domain.getId()).isEqualTo(10);
        assertThat(domain.getIdPlanBase()).isEqualTo(1);
        assertThat(domain.getDescripcionCambios()).isEqualTo("Modificado");
    }

    @Test
    @DisplayName("copyScalarFieldsBase() copia campos escalares del plan base")
    void copyScalarFieldsBase() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.COMPLETO, LocalDate.now(), true, new ArrayList<>());
        PlanAlimentarioJpaEntity destino = new PlanAlimentarioJpaEntity();

        mapper.copyScalarFieldsBase(plan, destino);

        assertThat(destino.getEstadoPlan()).isEqualTo(EstadoPlan.COMPLETO);
        assertThat(destino.isActivo()).isTrue();
    }

    @Test
    @DisplayName("copyScalarFieldsPersonalizado() copia campos escalares del plan personalizado")
    void copyScalarFieldsPersonalizado() {
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "Cambio", EstadoPlan.COMPLETO, LocalDate.now(), true, new ArrayList<>());
        PlanAlimentarioJpaEntity destino = new PlanAlimentarioJpaEntity();
        PlanAlimentarioJpaEntity planBase = new PlanAlimentarioJpaEntity();
        planBase.setId(1);

        mapper.copyScalarFieldsPersonalizado(plan, destino, planBase);

        assertThat(destino.getEstadoPlan()).isEqualTo(EstadoPlan.COMPLETO);
        assertThat(destino.getDescripcionCambios()).isEqualTo("Cambio");
        assertThat(destino.getPlanBase()).isEqualTo(planBase);
    }

    @Test
    @DisplayName("mergeDias() realiza merge entre dias de dominio y entidad JPA")
    void mergeDias() {
        TiempoDeComida t1 = TiempoDeComida.reconstruir(100, Hora.of(LocalTime.of(8, 0)), List.of(10));
        Dia d1 = Dia.reconstruir(1000, Orden.of(1), new ArrayList<>(List.of(t1)));
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>(List.of(d1)));

        PlanAlimentarioJpaEntity planEntity = new PlanAlimentarioJpaEntity();
        planEntity.setDias(new ArrayList<>());

        RecetaJpaEntity recetaJpa = new RecetaJpaEntity();
        recetaJpa.setId(10);
        Map<Integer, RecetaJpaEntity> recetasMap = new HashMap<>();
        recetasMap.put(10, recetaJpa);

        mapper.mergeDias(planEntity, plan.getDias(), recetasMap);

        assertThat(planEntity.getDias()).hasSize(1);
        assertThat(planEntity.getDias().get(0).getTiemposDeComida()).hasSize(1);
        assertThat(planEntity.getDias().get(0).getTiemposDeComida().get(0).getRecetas()).containsExactly(recetaJpa);
    }
}
