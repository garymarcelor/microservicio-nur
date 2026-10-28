package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.planalimentariobase;

import com.nutrition.msnutritionplanner.domain.planbase.Dia;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizado;
import com.nutrition.msnutritionplanner.domain.shared.Hora;
import com.nutrition.msnutritionplanner.domain.shared.Orden;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.DiaJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.PlanAlimentarioJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.TiempoDeComidaJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapeo manual entre PlanAlimentarioJpaEntity (incluyendo sus entidades
 * internas Dia/TiempoDeComida) y los agregados de dominio
 * PlanAlimentarioBase / PlanAlimentarioPersonalizado.
 *
 * Se hace de forma manual (y no con ModelMapper generico) porque el
 * dominio usa Value Objects (Hora, Orden) y referencias por identidad a
 * Receta, mientras que la entidad JPA usa tipos primitivos y relaciones
 * @ManyToMany completas; ademas se requiere una estrategia de "merge"
 * (por id) para no perder identidades de fila al persistir.
 */
@Component
public class PlanAlimentarioMapper {

    // ---------- JPA -> Dominio ----------

    public PlanAlimentarioBase toDomainBase(PlanAlimentarioJpaEntity entity) {
        return PlanAlimentarioBase.reconstruir(
                entity.getId(), entity.getEstadoPlan(), entity.getFechaInicioVigencia(), entity.isActivo(), mapDiasToDomain(entity));
    }

    public PlanAlimentarioPersonalizado toDomainPersonalizado(PlanAlimentarioJpaEntity entity) {
        Integer idPlanBase = entity.getPlanBase() != null ? entity.getPlanBase().getId() : null;
        return PlanAlimentarioPersonalizado.reconstruir(
                entity.getId(), idPlanBase, entity.getDescripcionCambios(),
                entity.getEstadoPlan(), entity.getFechaInicioVigencia(), entity.isActivo(), mapDiasToDomain(entity));
    }

    private List<Dia> mapDiasToDomain(PlanAlimentarioJpaEntity entity) {
        List<Dia> dias = new ArrayList<>();
        for (DiaJpaEntity diaJpa : entity.getDias()) {
            List<TiempoDeComida> tiempos = new ArrayList<>();
            for (TiempoDeComidaJpaEntity tiempoJpa : diaJpa.getTiemposDeComida()) {
                List<Integer> recetasIds = tiempoJpa.getRecetas().stream()
                        .map(RecetaJpaEntity::getId)
                        .toList();
                tiempos.add(TiempoDeComida.reconstruir(tiempoJpa.getId(), Hora.of(tiempoJpa.getHora()), recetasIds));
            }
            dias.add(Dia.reconstruir(diaJpa.getId(), Orden.of(diaJpa.getNumOrder()), tiempos));
        }
        return dias;
    }

    // ---------- Dominio -> JPA (merge por id, conserva filas existentes) ----------

    public void copyScalarFieldsBase(PlanAlimentarioBase plan, PlanAlimentarioJpaEntity destino) {
        destino.setEstadoPlan(plan.getEstadoPlan());
        destino.setFechaInicioVigencia(plan.getFechaInicioVigencia());
        destino.setActivo(plan.isActivo());
        destino.setPlanBase(null);
    }

    public void copyScalarFieldsPersonalizado(PlanAlimentarioPersonalizado plan, PlanAlimentarioJpaEntity destino,
                                               PlanAlimentarioJpaEntity planBaseEntity) {
        destino.setEstadoPlan(plan.getEstadoPlan());
        destino.setFechaInicioVigencia(plan.getFechaInicioVigencia());
        destino.setDescripcionCambios(plan.getDescripcionCambios());
        destino.setActivo(plan.isActivo());
        destino.setPlanBase(planBaseEntity);
    }

    /**
     * Sincroniza (por id) la lista de dias/tiempos de comida del agregado de
     * dominio hacia la entidad JPA, agregando filas nuevas, actualizando las
     * existentes y eliminando (orphanRemoval) las que ya no esten presentes
     * en el dominio.
     */
    public void mergeDias(PlanAlimentarioJpaEntity planEntity, List<Dia> diasDominio,
                           Map<Integer, RecetaJpaEntity> recetasPorId) {

        Map<Integer, DiaJpaEntity> diasExistentesPorId = new HashMap<>();
        for (DiaJpaEntity d : planEntity.getDias()) {
            if (d.getId() != null) {
                diasExistentesPorId.put(d.getId(), d);
            }
        }

        List<DiaJpaEntity> nuevaListaDias = new ArrayList<>();
        for (Dia diaDominio : diasDominio) {
            DiaJpaEntity diaEntity = diaDominio.getId() != null ? diasExistentesPorId.get(diaDominio.getId()) : null;
            if (diaEntity == null) {
                diaEntity = new DiaJpaEntity();
            }
            diaEntity.setNumOrder(diaDominio.getNumOrden().valor());
            diaEntity.setNroTiemposComida(diaDominio.getNroTiemposComida());
            diaEntity.setPlan(planEntity);
            mergeTiempos(diaEntity, diaDominio.getTiemposDeComida(), recetasPorId);
            nuevaListaDias.add(diaEntity);
        }

        planEntity.getDias().clear();
        planEntity.getDias().addAll(nuevaListaDias);
    }

    private void mergeTiempos(DiaJpaEntity diaEntity, List<TiempoDeComida> tiemposDominio,
                               Map<Integer, RecetaJpaEntity> recetasPorId) {

        Map<Integer, TiempoDeComidaJpaEntity> existentesPorId = new HashMap<>();
        for (TiempoDeComidaJpaEntity t : diaEntity.getTiemposDeComida()) {
            if (t.getId() != null) {
                existentesPorId.put(t.getId(), t);
            }
        }

        List<TiempoDeComidaJpaEntity> nuevaLista = new ArrayList<>();
        for (TiempoDeComida tiempoDominio : tiemposDominio) {
            TiempoDeComidaJpaEntity tiempoEntity = tiempoDominio.getId() != null
                    ? existentesPorId.get(tiempoDominio.getId()) : null;
            if (tiempoEntity == null) {
                tiempoEntity = new TiempoDeComidaJpaEntity();
            }
            tiempoEntity.setHora(tiempoDominio.getHora().valor());
            tiempoEntity.setDia(diaEntity);
            List<RecetaJpaEntity> recetas = tiempoDominio.getRecetasIds().stream()
                    .map(recetasPorId::get)
                    .toList();
            if (tiempoEntity.getRecetas() == null) {
                tiempoEntity.setRecetas(new ArrayList<>());
            }
            tiempoEntity.getRecetas().clear();
            tiempoEntity.getRecetas().addAll(recetas);
            nuevaLista.add(tiempoEntity);
        }

        diaEntity.getTiemposDeComida().clear();
        diaEntity.getTiemposDeComida().addAll(nuevaLista);
    }
}
