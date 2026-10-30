package com.nutrition.msnutritionplanner.application.planpersonalizado;

import com.nutrition.msnutritionplanner.application.planbase.dto.TiempoDeComidaRequest;
import com.nutrition.msnutritionplanner.application.planpersonalizado.dto.CrearPlanPersonalizadoRequest;
import com.nutrition.msnutritionplanner.application.shared.event.IntegrationEventPublisher;
import com.nutrition.msnutritionplanner.application.planpersonalizado.dto.PlanPersonalizadoResponse;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBaseRepository;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizado;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizadoRepository;
import com.nutrition.msnutritionplanner.domain.receta.RecetaRepository;
import com.nutrition.msnutritionplanner.domain.shared.Hora;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso del agregado PlanAlimentarioPersonalizado. Corresponde a
 * una nueva version de un plan (base o previamente personalizado) que se
 * genera cuando, dentro del servicio de Catering, el paciente o el
 * profesional requieren modificar la planificacion original.
 *
 * Actividad 2:
 *  - al COMPLETAR el plan se registra bc2.PlanPersonalizadoCreado (outbox);
 *  - al consumir bc3.SuscripcionCancelada se desactiva el plan
 *    ({@link #desactivarPorSuscripcionCancelada(Integer, String)}).
 */
@Service
@Transactional
public class PlanAlimentarioPersonalizadoUseCases {

    private static final Logger log = LoggerFactory.getLogger(PlanAlimentarioPersonalizadoUseCases.class);

    private final PlanAlimentarioPersonalizadoRepository planPersonalizadoRepository;
    private final PlanAlimentarioBaseRepository planBaseRepository;
    private final RecetaRepository recetaRepository;
    private final IntegrationEventPublisher eventPublisher;

    public PlanAlimentarioPersonalizadoUseCases(PlanAlimentarioPersonalizadoRepository planPersonalizadoRepository,
                                                 PlanAlimentarioBaseRepository planBaseRepository,
                                                 RecetaRepository recetaRepository,
                                                 IntegrationEventPublisher eventPublisher) {
        this.planPersonalizadoRepository = planPersonalizadoRepository;
        this.planBaseRepository = planBaseRepository;
        this.recetaRepository = recetaRepository;
        this.eventPublisher = eventPublisher;
    }

    public PlanPersonalizadoResponse crear(CrearPlanPersonalizadoRequest request) {
        PlanAlimentarioBase planOrigen = planBaseRepository.buscarPorId(request.idPlanBase())
                .orElseThrow(() -> new EntityNotFoundException("PlanAlimentarioBase", request.idPlanBase()));
        PlanAlimentarioPersonalizado nuevo = PlanAlimentarioPersonalizado.crearDesde(
                planOrigen, request.idPaciente(), request.descripcionCambios());
        return PlanPersonalizadoResponse.desde(planPersonalizadoRepository.guardar(nuevo));
    }

    public PlanPersonalizadoResponse agregarDia(Integer idPlan) {
        PlanAlimentarioPersonalizado plan = obtenerOFallar(idPlan);
        plan.agregarDia();
        return PlanPersonalizadoResponse.desde(planPersonalizadoRepository.guardar(plan));
    }

    public PlanPersonalizadoResponse quitarDia(Integer idPlan, Integer idDia) {
        PlanAlimentarioPersonalizado plan = obtenerOFallar(idPlan);
        plan.quitarDia(idDia);
        return PlanPersonalizadoResponse.desde(planPersonalizadoRepository.guardar(plan));
    }

    public PlanPersonalizadoResponse agregarTiempoDeComida(Integer idPlan, Integer idDia, TiempoDeComidaRequest request) {
        validarRecetasExisten(request.recetasIds());
        PlanAlimentarioPersonalizado plan = obtenerOFallar(idPlan);
        plan.agregarTiempoDeComida(idDia, Hora.of(request.hora()), request.recetasIds());
        return PlanPersonalizadoResponse.desde(planPersonalizadoRepository.guardar(plan));
    }

    public PlanPersonalizadoResponse quitarTiempoDeComida(Integer idPlan, Integer idDia, Integer idTiempo) {
        PlanAlimentarioPersonalizado plan = obtenerOFallar(idPlan);
        plan.quitarTiempoDeComida(idDia, idTiempo);
        return PlanPersonalizadoResponse.desde(planPersonalizadoRepository.guardar(plan));
    }

    public PlanPersonalizadoResponse completar(Integer idPlan) {
        PlanAlimentarioPersonalizado plan = obtenerOFallar(idPlan);
        plan.completar();
        PlanAlimentarioPersonalizado guardado = planPersonalizadoRepository.guardar(plan);
        // Evento de integracion -> tabla outbox (misma transaccion que el cambio de estado)
        eventPublisher.planPersonalizadoCreado(guardado);
        return PlanPersonalizadoResponse.desde(guardado);
    }

    public PlanPersonalizadoResponse descartar(Integer idPlan) {
        PlanAlimentarioPersonalizado plan = obtenerOFallar(idPlan);
        plan.descartar();
        return PlanPersonalizadoResponse.desde(planPersonalizadoRepository.guardar(plan));
    }

    public void eliminar(Integer idPlan) {
        PlanAlimentarioPersonalizado plan = obtenerOFallar(idPlan);
        plan.darDeBaja();
        planPersonalizadoRepository.guardar(plan);
    }

    /**
     * Reaccion al evento de integracion bc3.SuscripcionCancelada (BC3 -> BC2):
     * "BC2 desactiva el plan personalizado".
     *
     * Es idempotente y tolerante: si el plan no existe, no es personalizado o
     * ya esta inactivo, solo se registra en el log (reintentar no cambiaria
     * el resultado y bloquearia la particion de Kafka).
     *
     * @param idPlan    id interno del plan (ya traducido desde "PLAN-205")
     * @param idPaciente paciente informado por Catering (solo para verificacion/log)
     * @return true si el plan se desactivo en esta llamada
     */
    public boolean desactivarPorSuscripcionCancelada(Integer idPlan, String idPaciente) {
        var encontrado = planPersonalizadoRepository.buscarPorId(idPlan);
        if (encontrado.isEmpty()) {
            log.warn("SuscripcionCancelada: el plan personalizado {} no existe en BC2; se ignora", idPlan);
            return false;
        }
        PlanAlimentarioPersonalizado plan = encontrado.get();
        if (plan.getIdPaciente() != null && idPaciente != null && !plan.getIdPaciente().equals(idPaciente)) {
            log.warn("SuscripcionCancelada: el plan {} pertenece a {} y no a {}; se desactiva igualmente",
                    idPlan, plan.getIdPaciente(), idPaciente);
        }
        if (!plan.isActivo()) {
            log.info("SuscripcionCancelada: el plan {} ya estaba inactivo", idPlan);
            return false;
        }
        plan.darDeBaja();
        planPersonalizadoRepository.guardar(plan);
        return true;
    }

    @Transactional(readOnly = true)
    public PlanPersonalizadoResponse obtener(Integer idPlan) {
        return PlanPersonalizadoResponse.desde(obtenerOFallar(idPlan));
    }

    @Transactional(readOnly = true)
    public List<PlanPersonalizadoResponse> listarPorPlanBase(Integer idPlanBase) {
        return planPersonalizadoRepository.listarActivosPorPlanBase(idPlanBase).stream()
                .map(PlanPersonalizadoResponse::desde)
                .toList();
    }

    private void validarRecetasExisten(List<Integer> ids) {
        List<Integer> distintos = ids.stream().distinct().toList();
        if (recetaRepository.buscarPorIds(distintos).size() != distintos.size()) {
            throw new DomainValidationException("Una o mas recetas indicadas no existen o estan dadas de baja");
        }
    }

    private PlanAlimentarioPersonalizado obtenerOFallar(Integer id) {
        return planPersonalizadoRepository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("PlanAlimentarioPersonalizado", id));
    }
}
