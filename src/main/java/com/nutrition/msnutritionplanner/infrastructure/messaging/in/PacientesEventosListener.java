package com.nutrition.msnutritionplanner.infrastructure.messaging.in;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Consumidor Kafka de los eventos de BC1 (MsPatientsConsulCore).
 *
 * Topic: {@code app.kafka.consumo.pacientes-topic} (por defecto bc1.pacientes.eventos).
 * Procesa PacienteRegistrado, AntecedenteClinicoRegistrado,
 * NutricionistaRegistrado, EvaluacionRealizada y ProximaEvaluacionProgramada.
 * Cualquier otro evento del topic (p. ej. DireccionClienteRegistrada, que es
 * solo para Catering) se ignora filtrando por la cabecera eventName.
 *
 * Manejo de errores:
 *  - mensaje mal formado o que viola una regla de negocio -> se registra y se
 *    DESCARTA (reintentarlo daria el mismo resultado y bloquearia la particion);
 *  - error tecnico (p. ej. MySQL caido) -> la excepcion se propaga y Kafka
 *    reintenta el mensaje.
 *
 * Solo existe con {@code app.kafka.enabled=true}.
 */
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class PacientesEventosListener {

    private static final Logger log = LoggerFactory.getLogger(PacientesEventosListener.class);
    static final String CABECERA_EVENTO = "eventName";

    private static final Set<String> DE_INTERES = Set.of(
            ProcesadorEventosEntrada.PACIENTE_REGISTRADO,
            ProcesadorEventosEntrada.ANTECEDENTE_REGISTRADO,
            ProcesadorEventosEntrada.NUTRICIONISTA_REGISTRADO,
            ProcesadorEventosEntrada.EVALUACION_REALIZADA,
            ProcesadorEventosEntrada.PROXIMA_EVALUACION);

    private final ProcesadorEventosEntrada procesador;

    public PacientesEventosListener(ProcesadorEventosEntrada procesador) {
        this.procesador = procesador;
    }

    @KafkaListener(topics = "${app.kafka.consumo.pacientes-topic}", groupId = "${spring.application.name}")
    public void recibir(ConsumerRecord<String, String> registro) {
        String evento = nombreDe(registro, procesador);
        if (!DE_INTERES.contains(evento)) {
            log.debug("Ignorado {} (key={})", evento, registro.key());
            return;
        }
        try {
            var resultado = procesador.procesar(registro.value());
            log.info("Evento {} key={} -> {}", evento, registro.key(), resultado);
        } catch (DomainException | IllegalArgumentException | IllegalStateException e) {
            log.warn("Evento {} rechazado y descartado (partition={}, offset={}): {}",
                    evento, registro.partition(), registro.offset(), e.getMessage());
        }
    }

    /** La cabecera eventName; si el productor no la envio, el campo del sobre. */
    static String nombreDe(ConsumerRecord<String, String> registro, ProcesadorEventosEntrada procesador) {
        Header cabecera = registro.headers().lastHeader(CABECERA_EVENTO);
        if (cabecera != null) {
            return new String(cabecera.value(), StandardCharsets.UTF_8);
        }
        try {
            return procesador.nombreDe(registro.value());
        } catch (IllegalArgumentException e) {
            return null; // mensaje ilegible: no es de interes
        }
    }
}
