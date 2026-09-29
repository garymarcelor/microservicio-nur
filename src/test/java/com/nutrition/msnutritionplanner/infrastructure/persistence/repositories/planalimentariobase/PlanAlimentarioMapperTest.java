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
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanAlimentarioMapperTest {

    private PlanAlimentarioMapper planAlimentarioMapper;

    @BeforeEach
    void setUp() {
        planAlimentarioMapper = new PlanAlimentarioMapper();
    }

    @Test
    void toDomainBase_ShouldMapToDomainBase() {
        PlanAlimentarioJpaEntity entity = new PlanAlimentarioJpaEntity();
        entity.setId(1);
        entity.setEstadoPlan(EstadoPlan.INICIADO);
        entity.setFechaInicioVigencia(LocalDate.now());
        entity.setActivo(true);
        entity.setDias(new ArrayList<>());

        DiaJpaEntity dia = new DiaJpaEntity();
        dia.setId(10);
        dia.setNumOrder(1);
        dia.setTiemposDeComida(new ArrayList<>());

        TiempoDeComidaJpaEntity tiempo = new TiempoDeComidaJpaEntity();
        tiempo.setId(100);
        tiempo.setHora(LocalTime.of(8, 0));
        tiempo.setRecetas(new ArrayList<>());

        RecetaJpaEntity receta = new RecetaJpaEntity();
        receta.setId(1000);
        tiempo.getRecetas().add(receta);

        dia.getTiemposDeComida().add(tiempo);
        entity.getDias().add(dia);

        PlanAlimentarioBase result = planAlimentarioMapper.toDomainBase(entity);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(EstadoPlan.INICIADO, result.getEstadoPlan());
        assertEquals(entity.getFechaInicioVigencia(), result.getFechaInicioVigencia());
        assertTrue(result.isActivo());
        assertEquals(1, result.getDias().size());
        assertEquals(10, result.getDias().get(0).getId());
        assertEquals(1, result.getDias().get(0).getNumOrden().valor());
        assertEquals(1, result.getDias().get(0).getTiemposDeComida().size());
        assertEquals(100, result.getDias().get(0).getTiemposDeComida().get(0).getId());
        assertEquals(LocalTime.of(8, 0), result.getDias().get(0).getTiemposDeComida().get(0).getHora().valor());
        assertEquals(1, result.getDias().get(0).getTiemposDeComida().get(0).getRecetasIds().size());
        assertTrue(result.getDias().get(0).getTiemposDeComida().get(0).getRecetasIds().contains(1000));
    }

    @Test
    void toDomainPersonalizado_ShouldMapToDomainPersonalizado() {
        PlanAlimentarioJpaEntity entity = new PlanAlimentarioJpaEntity();
        entity.setId(2);
        entity.setEstadoPlan(EstadoPlan.COMPLETO);
        entity.setFechaInicioVigencia(LocalDate.now());
        entity.setDescripcionCambios("Cambios");
        entity.setActivo(false);
        entity.setDias(new ArrayList<>());

        PlanAlimentarioJpaEntity baseEntity = new PlanAlimentarioJpaEntity();
        baseEntity.setId(1);
        entity.setPlanBase(baseEntity);

        PlanAlimentarioPersonalizado result = planAlimentarioMapper.toDomainPersonalizado(entity);

        assertNotNull(result);
        assertEquals(2, result.getId());
        assertEquals(1, result.getIdPlanBase());
        assertEquals(EstadoPlan.COMPLETO, result.getEstadoPlan());
        assertEquals(entity.getFechaInicioVigencia(), result.getFechaInicioVigencia());
        assertEquals("Cambios", result.getDescripcionCambios());
        assertFalse(result.isActivo());
        assertTrue(result.getDias().isEmpty());
    }

    @Test
    void copyScalarFieldsBase_ShouldCopyFields() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>());
        PlanAlimentarioJpaEntity destino = new PlanAlimentarioJpaEntity();
        
        planAlimentarioMapper.copyScalarFieldsBase(plan, destino);

        assertEquals(EstadoPlan.INICIADO, destino.getEstadoPlan());
        assertEquals(plan.getFechaInicioVigencia(), destino.getFechaInicioVigencia());
        assertTrue(destino.isActivo());
        assertNull(destino.getPlanBase());
    }

    @Test
    void copyScalarFieldsPersonalizado_ShouldCopyFields() {
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(1, 10, "Cambios", EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>());
        PlanAlimentarioJpaEntity destino = new PlanAlimentarioJpaEntity();
        PlanAlimentarioJpaEntity baseEntity = new PlanAlimentarioJpaEntity();
        baseEntity.setId(10);

        planAlimentarioMapper.copyScalarFieldsPersonalizado(plan, destino, baseEntity);

        assertEquals(EstadoPlan.INICIADO, destino.getEstadoPlan());
        assertEquals(plan.getFechaInicioVigencia(), destino.getFechaInicioVigencia());
        assertEquals("Cambios", destino.getDescripcionCambios());
        assertTrue(destino.isActivo());
        assertEquals(baseEntity, destino.getPlanBase());
    }

    @Test
    void mergeDias_ShouldMergeDiasAndTiemposDeComida() {
        PlanAlimentarioJpaEntity planEntity = new PlanAlimentarioJpaEntity();
        planEntity.setDias(new ArrayList<>());

        DiaJpaEntity diaExistente = new DiaJpaEntity();
        diaExistente.setId(1);
        diaExistente.setTiemposDeComida(new ArrayList<>());
        planEntity.getDias().add(diaExistente);

        TiempoDeComidaJpaEntity tiempoExistente = new TiempoDeComidaJpaEntity();
        tiempoExistente.setId(10);
        tiempoExistente.setRecetas(new ArrayList<>());
        diaExistente.getTiemposDeComida().add(tiempoExistente);

        List<TiempoDeComida> tiemposDominio = new ArrayList<>();
        tiemposDominio.add(TiempoDeComida.reconstruir(10, Hora.of(12, 0), List.of(100)));
        tiemposDominio.add(TiempoDeComida.reconstruir(null, Hora.of(18, 0), List.of(200)));

        List<Dia> diasDominio = new ArrayList<>();
        diasDominio.add(Dia.reconstruir(1, Orden.of(1), tiemposDominio));
        diasDominio.add(Dia.reconstruir(null, Orden.of(2), new ArrayList<>()));

        Map<Integer, RecetaJpaEntity> recetasPorId = new HashMap<>();
        RecetaJpaEntity r1 = new RecetaJpaEntity();
        r1.setId(100);
        RecetaJpaEntity r2 = new RecetaJpaEntity();
        r2.setId(200);
        recetasPorId.put(100, r1);
        recetasPorId.put(200, r2);

        planAlimentarioMapper.mergeDias(planEntity, diasDominio, recetasPorId);

        assertEquals(2, planEntity.getDias().size());
        
        DiaJpaEntity updatedDia1 = planEntity.getDias().get(0);
        assertEquals(1, updatedDia1.getNumOrder());
        assertEquals(2, updatedDia1.getTiemposDeComida().size());
        
        assertEquals(10, updatedDia1.getTiemposDeComida().get(0).getId());
        assertEquals(LocalTime.of(12, 0), updatedDia1.getTiemposDeComida().get(0).getHora());
        assertEquals(1, updatedDia1.getTiemposDeComida().get(0).getRecetas().size());
        assertEquals(100, updatedDia1.getTiemposDeComida().get(0).getRecetas().get(0).getId());

        assertNull(updatedDia1.getTiemposDeComida().get(1).getId());
        assertEquals(LocalTime.of(18, 0), updatedDia1.getTiemposDeComida().get(1).getHora());
        assertEquals(1, updatedDia1.getTiemposDeComida().get(1).getRecetas().size());
        assertEquals(200, updatedDia1.getTiemposDeComida().get(1).getRecetas().get(0).getId());

        DiaJpaEntity addedDia2 = planEntity.getDias().get(1);
        assertNull(addedDia2.getId());
        assertEquals(2, addedDia2.getNumOrder());
        assertEquals(0, addedDia2.getTiemposDeComida().size());
    }
}
