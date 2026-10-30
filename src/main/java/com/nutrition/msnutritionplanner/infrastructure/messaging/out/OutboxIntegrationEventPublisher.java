package com.nutrition.msnutritionplanner.infrastructure.messaging.out;

import com.nutrition.msnutritionplanner.application.shared.event.CorrelationContext;
import com.nutrition.msnutritionplanner.application.shared.event.IntegrationEventPublisher;
import com.nutrition.msnutritionplanner.domain.ingrediente.Ingrediente;
import com.nutrition.msnutritionplanner.domain.ingrediente.IngredienteRepository;
import com.nutrition.msnutritionplanner.domain.planbase.Dia;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizado;
import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.domain.receta.RecetaRepository;
import com.nutrition.msnutritionplanner.infrastructure.messaging.out.EventosSalida.DiaData;
import com.nutrition.msnutritionplanner.infrastructure.messaging.out.EventosSalida.IngredienteData;
import com.nutrition.msnutritionplanner.infrastructure.messaging.out.EventosSalida.PlanBaseCreadoData;
import com.nutrition.msnutritionplanner.infrastructure.messaging.out.EventosSalida.PlanPersonalizadoCreadoData;
import com.nutrition.msnutritionplanner.infrastructure.messaging.out.EventosSalida.RecetaCreadaData;
import com.nutrition.msnutritionplanner.infrastructure.messaging.out.EventosSalida.RecetaEnTiempo;
import com.nutrition.msnutritionplanner.infrastructure.messaging.out.EventosSalida.Sobre;
import com.nutrition.msnutritionplanner.infrastructure.messaging.out.EventosSalida.TiempoComidaData;
import com.nutrition.msnutritionplanner.infrastructure.outbox.OutboxJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.outbox.SpringDataOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementacion del puerto {@link IntegrationEventPublisher} con el patron
 * TRANSACTIONAL OUTBOX.
 *
 * NO publica en Kafka: arma el sobre JSON del contrato y lo INSERTA en la
 * tabla {@code outbox_event} usando la transaccion del caso de uso
 * ({@code Propagation.MANDATORY}: si alguien lo llama sin transaccion falla,
 * porque entonces se perderia la garantia de atomicidad). El envio real a
 * Kafka lo hace despues {@code OutboxRelay}.
 *
 * Los identificadores viajan como string con prefijo, segun el contrato:
 * PLAN-, DIA-, TC-, REC-, ING-.
 */
@Component
public class OutboxIntegrationEventPublisher implements IntegrationEventPublisher {

    static final String VERSION = "1.0";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final SpringDataOutboxRepository outbox;
    private final RecetaRepository recetaRepository;
    private final IngredienteRepository ingredienteRepository;
    private final JsonMapper jsonMapper;
    private final Clock reloj;
    private final String topic;
    private final String source;

    public OutboxIntegrationEventPublisher(SpringDataOutboxRepository outbox,
                                           RecetaRepository recetaRepository,
                                           IngredienteRepository ingredienteRepository,
                                           JsonMapper jsonMapper,
                                           Clock reloj,
                                           @Value("${app.kafka.topic}") String topic,
                                           @Value("${app.events.source:MSNutritionPlanner}") String source) {
        this.outbox = outbox;
        this.recetaRepository = recetaRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.jsonMapper = jsonMapper;
        this.reloj = reloj;
        this.topic = topic;
        this.source = source;
    }

    // ---------------------------------------------------------------- 7
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void planBaseCreado(PlanAlimentarioBase plan) {
        String idPlan = "PLAN-" + plan.getId();
        PlanBaseCreadoData data = new PlanBaseCreadoData(
                idPlan, plan.getEstadoPlan().name(), plan.getFechaInicioVigencia().toString(),
                plan.getDias().size(), plan.isActivo(), mapearDias(plan));
        registrar("bc2.PlanBaseCreado", idPlan, "corr-catalogo-" + idPlan, data);
    }

    // ---------------------------------------------------------------- 8
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void planPersonalizadoCreado(PlanAlimentarioPersonalizado plan) {
        String idPlan = "PLAN-" + plan.getId();
        PlanPersonalizadoCreadoData data = new PlanPersonalizadoCreadoData(
                idPlan, "PLAN-" + plan.getIdPlanBase(), plan.getIdPaciente(), plan.getDescripcionCambios(),
                plan.getEstadoPlan().name(), plan.getFechaInicioVigencia().toString(),
                plan.getDias().size(), plan.isActivo(), mapearDias(plan));
        registrar("bc2.PlanPersonalizadoCreado", idPlan, "corr-alta-" + plan.getIdPaciente(), data);
    }

    // ---------------------------------------------------------------- 9
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recetaCreada(Receta receta) {
        String idReceta = "REC-" + receta.getId();
        // Los nombres de ingrediente no estan en la receta (solo su id): se consultan
        Map<Integer, Ingrediente> ingredientes = ingredienteRepository.buscarPorIds(receta.getIngredientesIds()).stream()
                .collect(Collectors.toMap(Ingrediente::getId, Function.identity()));

        List<IngredienteData> detalle = receta.getIngredientes().stream()
                .map(i -> new IngredienteData(
                        "ING-" + i.idIngrediente(), nombreDe(ingredientes.get(i.idIngrediente())),
                        normalizar(i.cantidad()), i.unidad().name()))
                .toList();

        RecetaCreadaData data = new RecetaCreadaData(
                idReceta, receta.getNombre(), receta.getInstruccion(), receta.getPorcionGramos(),
                receta.isActivo(), detalle);
        registrar("bc2.RecetaCreada", idReceta, "corr-catalogo-" + idReceta, data);
    }

    // ------------------------------------------------------------ privados

    /** Convierte los dias/tiempos del plan al formato del contrato, resolviendo nombre y porcion de cada receta. */
    private List<DiaData> mapearDias(PlanAlimentarioBase plan) {
        List<Integer> idsRecetas = plan.getDias().stream()
                .flatMap(d -> d.getTiemposDeComida().stream())
                .flatMap(t -> t.getRecetasIds().stream())
                .distinct()
                .toList();
        Map<Integer, Receta> recetas = idsRecetas.isEmpty() ? Map.of()
                : recetaRepository.buscarPorIds(idsRecetas).stream()
                .collect(Collectors.toMap(Receta::getId, Function.identity()));

        return plan.getDias().stream().map(dia -> mapearDia(dia, recetas)).toList();
    }

    private DiaData mapearDia(Dia dia, Map<Integer, Receta> recetas) {
        List<TiempoComidaData> tiempos = dia.getTiemposDeComida().stream()
                .map(t -> mapearTiempo(t, recetas))
                .toList();
        return new DiaData("DIA-" + dia.getId(), dia.getNumOrden().valor(), dia.getNroTiemposComida(), tiempos);
    }

    private TiempoComidaData mapearTiempo(TiempoDeComida tiempo, Map<Integer, Receta> recetas) {
        List<RecetaEnTiempo> items = tiempo.getRecetasIds().stream()
                .map(idReceta -> {
                    Receta r = recetas.get(idReceta);
                    return new RecetaEnTiempo("REC-" + idReceta,
                            r != null ? r.getNombre() : null, r != null ? r.getPorcionGramos() : null);
                })
                .toList();
        return new TiempoComidaData("TC-" + tiempo.getId(), tiempo.getHora().valor().format(HORA), items);
    }

    private String nombreDe(Ingrediente ingrediente) {
        return ingrediente != null ? ingrediente.getNombre() : null;
    }

    /** 180.00 -> 180 (sin notacion cientifica) para que el JSON coincida con el contrato. */
    static BigDecimal normalizar(BigDecimal valor) {
        BigDecimal limpio = valor.stripTrailingZeros();
        return limpio.scale() < 0 ? limpio.setScale(0) : limpio;
    }

    /**
     * Arma el sobre del contrato y lo guarda en outbox_event.
     * El correlationId del flujo en curso (cabecera HTTP o evento consumido)
     * tiene prioridad; si no hay, se usa el valor por defecto indicado.
     */
    private <T> void registrar(String eventName, String messageKey, String correlacionPorDefecto, T data) {
        String eventId = UUID.randomUUID().toString();
        String correlationId = CorrelationContext.actual().orElse(correlacionPorDefecto);
        Sobre<T> sobre = new Sobre<>(eventId, eventName, VERSION, reloj.instant().toString(),
                source, correlationId, data);
        String json = jsonMapper.writeValueAsString(sobre);
        outbox.save(new OutboxJpaEntity(eventId, eventName, topic, messageKey, json, reloj.instant()));
    }
}
