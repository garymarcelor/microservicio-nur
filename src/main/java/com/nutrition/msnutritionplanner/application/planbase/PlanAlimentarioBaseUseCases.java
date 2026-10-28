package com.nutrition.msnutritionplanner.application.planbase;

import com.nutrition.msnutritionplanner.application.planbase.dto.CrearPlanBaseRequest;
import com.nutrition.msnutritionplanner.application.planbase.dto.PlanBaseResponse;
import com.nutrition.msnutritionplanner.application.planbase.dto.TiempoDeComidaRequest;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBaseRepository;
import com.nutrition.msnutritionplanner.domain.receta.RecetaRepository;
import com.nutrition.msnutritionplanner.domain.shared.Hora;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso del agregado PlanAlimentarioBase. Corresponde al servicio
 * de "Asesoramiento Nutricional" del caso de estudio: el profesional en
 * nutricion elabora un plan alimentario de 15 dias con sus tiempos de
 * comida y recetas asignadas.
 */
@Service
@Transactional
public class PlanAlimentarioBaseUseCases {

    private final PlanAlimentarioBaseRepository planRepository;
    private final RecetaRepository recetaRepository;

    public PlanAlimentarioBaseUseCases(PlanAlimentarioBaseRepository planRepository, RecetaRepository recetaRepository) {
        this.planRepository = planRepository;
        this.recetaRepository = recetaRepository;
    }

    public PlanBaseResponse crear(CrearPlanBaseRequest request) {
        PlanAlimentarioBase plan = PlanAlimentarioBase.crear(request.fechaInicioVigencia());
        return PlanBaseResponse.desde(planRepository.guardar(plan));
    }

    public PlanBaseResponse agregarDia(Integer idPlan) {
        PlanAlimentarioBase plan = obtenerOFallar(idPlan);
        plan.agregarDia();
        return PlanBaseResponse.desde(planRepository.guardar(plan));
    }

    public PlanBaseResponse quitarDia(Integer idPlan, Integer idDia) {
        PlanAlimentarioBase plan = obtenerOFallar(idPlan);
        plan.quitarDia(idDia);
        return PlanBaseResponse.desde(planRepository.guardar(plan));
    }

    public PlanBaseResponse agregarTiempoDeComida(Integer idPlan, Integer idDia, TiempoDeComidaRequest request) {
        validarRecetasExisten(request.recetasIds());
        PlanAlimentarioBase plan = obtenerOFallar(idPlan);
        plan.agregarTiempoDeComida(idDia, Hora.of(request.hora()), request.recetasIds());
        return PlanBaseResponse.desde(planRepository.guardar(plan));
    }

    public PlanBaseResponse quitarTiempoDeComida(Integer idPlan, Integer idDia, Integer idTiempo) {
        PlanAlimentarioBase plan = obtenerOFallar(idPlan);
        plan.quitarTiempoDeComida(idDia, idTiempo);
        return PlanBaseResponse.desde(planRepository.guardar(plan));
    }

    public PlanBaseResponse completar(Integer idPlan) {
        PlanAlimentarioBase plan = obtenerOFallar(idPlan);
        plan.completar();
        return PlanBaseResponse.desde(planRepository.guardar(plan));
    }

    public PlanBaseResponse descartar(Integer idPlan) {
        PlanAlimentarioBase plan = obtenerOFallar(idPlan);
        plan.descartar();
        return PlanBaseResponse.desde(planRepository.guardar(plan));
    }

    public void eliminar(Integer idPlan) {
        PlanAlimentarioBase plan = obtenerOFallar(idPlan);
        plan.darDeBaja();
        planRepository.guardar(plan);
    }

    @Transactional(readOnly = true)
    public PlanBaseResponse obtener(Integer idPlan) {
        return PlanBaseResponse.desde(obtenerOFallar(idPlan));
    }

    @Transactional(readOnly = true)
    public List<PlanBaseResponse> listar() {
        return planRepository.listarActivos().stream().map(PlanBaseResponse::desde).toList();
    }

    private void validarRecetasExisten(List<Integer> ids) {
        List<Integer> distintos = ids.stream().distinct().toList();
        if (recetaRepository.buscarPorIds(distintos).size() != distintos.size()) {
            throw new DomainValidationException("Una o mas recetas indicadas no existen o estan dadas de baja");
        }
    }

    private PlanAlimentarioBase obtenerOFallar(Integer id) {
        return planRepository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("PlanAlimentarioBase", id));
    }
}
