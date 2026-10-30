package com.nutrition.msnutritionplanner.infrastructure.messaging.in;

import com.nutrition.msnutritionplanner.application.planpersonalizado.PlanAlimentarioPersonalizadoUseCases;
import com.nutrition.msnutritionplanner.application.shared.event.CorrelationContext;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.EventosEntrada.AntecedenteClinicoRegistrado;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.EventosEntrada.EvaluacionRealizada;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.EventosEntrada.NutricionistaRegistrado;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.EventosEntrada.PacienteRegistrado;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.EventosEntrada.ProximaEvaluacionProgramada;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.EventosEntrada.Sobre;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.EventosEntrada.SuscripcionCancelada;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.AntecedenteClinicoRefJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.AntecedenteClinicoRefRepository;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.EvaluacionRefJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.EvaluacionRefRepository;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.InboxEventJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.InboxEventRepository;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.NutricionistaRefJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.NutricionistaRefRepository;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.PacienteRefJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.PacienteRefRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Procesa los eventos de integracion CONSUMIDOS por BC2 (los listeners de
 * Kafka solo reciben el mensaje y delegan aqui).
 *
 * Todo ocurre en UNA transaccion: deduplicar por inbox + aplicar el efecto +
 * registrar el evento como procesado. Si algo falla, se revierte todo y Kafka
 * reintenta el mensaje; si sale bien, el eventId queda en inbox_event y una
 * repeticion posterior se descarta.
 *
 * Eventos soportados:
 *  - bc1.PacienteRegistrado           -> upsert paciente_ref
 *  - bc1.AntecedenteClinicoRegistrado -> upsert antecedente_clinico_ref
 *  - bc1.NutricionistaRegistrado      -> upsert nutricionista_ref
 *  - bc1.EvaluacionRealizada          -> evaluacion_ref (+ marca ajuste de plan pendiente)
 *  - bc1.ProximaEvaluacionProgramada  -> actualiza paciente_ref
 *  - bc3.SuscripcionCancelada         -> desactiva el plan personalizado (caso de uso)
 */
@Component
public class ProcesadorEventosEntrada {

    public static final String PACIENTE_REGISTRADO = "bc1.PacienteRegistrado";
    public static final String ANTECEDENTE_REGISTRADO = "bc1.AntecedenteClinicoRegistrado";
    public static final String NUTRICIONISTA_REGISTRADO = "bc1.NutricionistaRegistrado";
    public static final String EVALUACION_REALIZADA = "bc1.EvaluacionRealizada";
    public static final String PROXIMA_EVALUACION = "bc1.ProximaEvaluacionProgramada";
    public static final String SUSCRIPCION_CANCELADA = "bc3.SuscripcionCancelada";

    /** Que paso con el mensaje. */
    public enum Resultado { PROCESADO, DUPLICADO, IGNORADO }

    private static final Logger log = LoggerFactory.getLogger(ProcesadorEventosEntrada.class);

    private final JsonMapper jsonMapper;
    private final Clock reloj;
    private final InboxEventRepository inbox;
    private final PacienteRefRepository pacientes;
    private final NutricionistaRefRepository nutricionistas;
    private final AntecedenteClinicoRefRepository antecedentes;
    private final EvaluacionRefRepository evaluaciones;
    private final PlanAlimentarioPersonalizadoUseCases planesPersonalizados;

    public ProcesadorEventosEntrada(JsonMapper jsonMapper, Clock reloj, InboxEventRepository inbox,
                                    PacienteRefRepository pacientes, NutricionistaRefRepository nutricionistas,
                                    AntecedenteClinicoRefRepository antecedentes, EvaluacionRefRepository evaluaciones,
                                    PlanAlimentarioPersonalizadoUseCases planesPersonalizados) {
        this.jsonMapper = jsonMapper;
        this.reloj = reloj;
        this.inbox = inbox;
        this.pacientes = pacientes;
        this.nutricionistas = nutricionistas;
        this.antecedentes = antecedentes;
        this.evaluaciones = evaluaciones;
        this.planesPersonalizados = planesPersonalizados;
    }

    /** Lee solo el nombre del evento (el listener lo usa si el mensaje no trae la cabecera eventName). */
    public String nombreDe(String json) {
        return leerSobre(json).eventName();
    }

    /**
     * @throws IllegalArgumentException si el JSON esta mal formado o le faltan campos obligatorios
     *                                  (reintentar no lo arreglaria: el listener lo descarta).
     */
    @Transactional
    public Resultado procesar(String json) {
        Sobre sobre = leerSobre(json);
        if (sobre.eventId() == null || sobre.eventName() == null || sobre.data() == null) {
            throw new IllegalArgumentException("El sobre del evento no trae eventId/eventName/data");
        }
        if (inbox.existsById(sobre.eventId())) {
            log.info("[inbox] evento duplicado {} eventId={}: se descarta", sobre.eventName(), sobre.eventId());
            return Resultado.DUPLICADO;
        }

        // El correlationId del evento recibido se propaga a los eventos que BC2 emita como reaccion
        CorrelationContext.establecer(sobre.correlationId());
        try {
            boolean soportado = switch (sobre.eventName()) {
                case PACIENTE_REGISTRADO -> { alRegistrarPaciente(convertir(sobre, PacienteRegistrado.class)); yield true; }
                case ANTECEDENTE_REGISTRADO -> { alRegistrarAntecedente(convertir(sobre, AntecedenteClinicoRegistrado.class)); yield true; }
                case NUTRICIONISTA_REGISTRADO -> { alRegistrarNutricionista(convertir(sobre, NutricionistaRegistrado.class)); yield true; }
                case EVALUACION_REALIZADA -> { alRealizarEvaluacion(convertir(sobre, EvaluacionRealizada.class)); yield true; }
                case PROXIMA_EVALUACION -> { alProgramarProximaEvaluacion(convertir(sobre, ProximaEvaluacionProgramada.class)); yield true; }
                case SUSCRIPCION_CANCELADA -> { alCancelarSuscripcion(convertir(sobre, SuscripcionCancelada.class)); yield true; }
                default -> false;
            };
            if (!soportado) {
                log.debug("[inbox] evento {} no es de interes para BC2; se ignora", sobre.eventName());
                return Resultado.IGNORADO;
            }
            inbox.save(new InboxEventJpaEntity(sobre.eventId(), sobre.eventName(), reloj.instant()));
            return Resultado.PROCESADO;
        } finally {
            CorrelationContext.limpiar();
        }
    }

    // ------------------------------------------------------------------ manejadores

    private void alRegistrarPaciente(PacienteRegistrado e) {
        requerido(e.idPaciente(), "idPaciente");
        PacienteRefJpaEntity p = pacienteODefecto(e.idPaciente());
        p.setNombreCompleto(e.nombreCompleto());
        p.setFechaNacimiento(fecha(e.fechaNacimiento()));
        p.setSexo(e.sexo());
        p.setTelefono(e.telefono());
        p.setEmail(e.email());
        p.setIdNutricionista(e.idNutricionista());
        p.setFechaRegistro(fecha(e.fechaRegistro()));
        pacientes.save(p);
        log.info("[inbox] paciente {} registrado/actualizado en la copia local", e.idPaciente());
    }

    private void alRegistrarAntecedente(AntecedenteClinicoRegistrado e) {
        requerido(e.idAntecedente(), "idAntecedente");
        requerido(e.idPaciente(), "idPaciente");
        AntecedenteClinicoRefJpaEntity a = antecedentes.findById(e.idAntecedente())
                .orElseGet(AntecedenteClinicoRefJpaEntity::new);
        a.setIdAntecedente(e.idAntecedente());
        a.setIdPaciente(e.idPaciente());
        a.setTipo(e.tipo());
        a.setDescripcion(e.descripcion());
        a.setSeveridad(e.severidad());
        a.setIngredientesRestringidos(e.ingredientesRestringidos() == null ? null
                : jsonMapper.writeValueAsString(e.ingredientesRestringidos()));
        a.setIdNutricionista(e.idNutricionista());
        a.setFechaRegistro(fecha(e.fechaRegistro()));
        antecedentes.save(a);
        log.info("[inbox] antecedente {} del paciente {} guardado (severidad {}, restringidos {})",
                e.idAntecedente(), e.idPaciente(), e.severidad(), e.ingredientesRestringidos());
    }

    private void alRegistrarNutricionista(NutricionistaRegistrado e) {
        requerido(e.idNutricionista(), "idNutricionista");
        NutricionistaRefJpaEntity n = nutricionistas.findById(e.idNutricionista())
                .orElseGet(NutricionistaRefJpaEntity::new);
        n.setIdNutricionista(e.idNutricionista());
        n.setNombreCompleto(e.nombreCompleto());
        n.setMatriculaProfesional(e.matriculaProfesional());
        n.setEspecialidad(e.especialidad());
        n.setEmail(e.email());
        n.setActivo(e.activo() == null || e.activo());
        n.setFechaAlta(fecha(e.fechaAlta()));
        nutricionistas.save(n);
        log.info("[inbox] nutricionista {} registrado/actualizado en la copia local", e.idNutricionista());
    }

    private void alRealizarEvaluacion(EvaluacionRealizada e) {
        requerido(e.idEvaluacion(), "idEvaluacion");
        requerido(e.idPaciente(), "idPaciente");
        boolean ajuste = Boolean.TRUE.equals(e.requiereAjustePlan());

        EvaluacionRefJpaEntity ev = evaluaciones.findById(e.idEvaluacion()).orElseGet(EvaluacionRefJpaEntity::new);
        ev.setIdEvaluacion(e.idEvaluacion());
        ev.setIdPaciente(e.idPaciente());
        ev.setIdSuscripcion(e.idSuscripcion());
        ev.setIdNutricionista(e.idNutricionista());
        ev.setTipoEvaluacion(e.tipoEvaluacion());
        ev.setFechaEvaluacion(fecha(e.fechaEvaluacion()));
        ev.setPesoKg(e.pesoKg());
        ev.setImc(e.imc());
        ev.setObservaciones(e.observaciones());
        ev.setRequiereAjustePlan(ajuste);
        evaluaciones.save(ev);

        PacienteRefJpaEntity p = pacienteODefecto(e.idPaciente());
        p.setUltimaEvaluacionId(e.idEvaluacion());
        p.setAjustePlanPendiente(ajuste);
        pacientes.save(p);

        if (ajuste) {
            // Regla del contrato: "BC2 ajusta el plan si requiereAjustePlan es true".
            // El ajuste es una decision del nutricionista (crear un plan personalizado nuevo);
            // aqui queda marcado como pendiente para que no pase desapercibido.
            log.warn("[inbox] la evaluacion {} del paciente {} REQUIERE AJUSTAR EL PLAN. Observaciones: {}",
                    e.idEvaluacion(), e.idPaciente(), e.observaciones());
        }
    }

    private void alProgramarProximaEvaluacion(ProximaEvaluacionProgramada e) {
        requerido(e.idPaciente(), "idPaciente");
        PacienteRefJpaEntity p = pacienteODefecto(e.idPaciente());
        p.setProximaEvaluacionFecha(fecha(e.fechaProximaEvaluacion()));
        p.setProximaEvaluacionTipo(e.tipoEvaluacion());
        p.setRequiereAnalisisClinico(e.requiereAnalisisClinico());
        pacientes.save(p);
        log.info("[inbox] proxima evaluacion del paciente {}: {}", e.idPaciente(), e.fechaProximaEvaluacion());
    }

    private void alCancelarSuscripcion(SuscripcionCancelada e) {
        requerido(e.idPlan(), "idPlan");
        Integer idPlan = idNumerico(e.idPlan());
        boolean desactivado = planesPersonalizados.desactivarPorSuscripcionCancelada(idPlan, e.idPaciente());
        log.info("[inbox] suscripcion {} cancelada ({}): plan {} {}", e.idSuscripcion(), e.tipoBaja(),
                e.idPlan(), desactivado ? "desactivado" : "sin cambios");
    }

    // ------------------------------------------------------------------ utilidades

    private Sobre leerSobre(String json) {
        try {
            return jsonMapper.readValue(json, Sobre.class);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("JSON del evento mal formado: " + ex.getOriginalMessage(), ex);
        }
    }

    private <T> T convertir(Sobre sobre, Class<T> tipo) {
        try {
            return jsonMapper.treeToValue(sobre.data(), tipo);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("El 'data' de " + sobre.eventName() + " no coincide con el contrato: "
                    + ex.getOriginalMessage(), ex);
        }
    }

    /** Si el evento llega antes que bc1.PacienteRegistrado, se crea la fila solo con el id. */
    private PacienteRefJpaEntity pacienteODefecto(String idPaciente) {
        return pacientes.findById(idPaciente).orElseGet(() -> new PacienteRefJpaEntity(idPaciente));
    }

    private static void requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta el campo obligatorio '" + campo + "' en el evento");
        }
    }

    private static LocalDate fecha(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(iso);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Fecha invalida (se espera yyyy-MM-dd): " + iso, ex);
        }
    }

    /** "PLAN-205" -> 205 (los ids del contrato son strings con prefijo; internamente son enteros). */
    static Integer idNumerico(String idConPrefijo) {
        int guion = idConPrefijo.lastIndexOf('-');
        String numero = guion >= 0 ? idConPrefijo.substring(guion + 1) : idConPrefijo;
        try {
            return Integer.valueOf(numero);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identificador con formato invalido: " + idConPrefijo, ex);
        }
    }
}
