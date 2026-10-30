package com.nutrition.msnutritionplanner.infrastructure.messaging.in;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka de los eventos de BC3 (MsCateringSubscription).
 *
 * Topic: {@code app.kafka.consumo.catering-topic} (por defecto bc3.catering.eventos).
 * De todo lo que publica BC3, a BC2 solo le interesa bc3.SuscripcionCancelada
 * (desactiva el plan personalizado); el resto se ignora por la cabecera eventName.
 *
 * Solo existe con {@code app.kafka.enabled=true}. El manejo de errores es el
 * mismo que en {@link PacientesEventosListener}.
 */
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class CateringEventosListener {

    private static final Logger log = LoggerFactory.getLogger(CateringEventosListener.class);

    private final ProcesadorEventosEntrada procesador;

    public CateringEventosListener(ProcesadorEventosEntrada procesador) {
        this.procesador = procesador;
    }

    @KafkaListener(topics = "${app.kafka.consumo.catering-topic}", groupId = "${spring.application.name}")
    public void recibir(ConsumerRecord<String, String> registro) {
        String evento = PacientesEventosListener.nombreDe(registro, procesador);
        if (!ProcesadorEventosEntrada.SUSCRIPCION_CANCELADA.equals(evento)) {
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
}
