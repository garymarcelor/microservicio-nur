package com.nutrition.msnutritionplanner.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Configuracion de la mensajeria (Kafka + Transactional Outbox).
 *
 *  - {@code @EnableScheduling} activa el {@code @Scheduled} del relay.
 *  - {@link Clock} se inyecta (en vez de usar Instant.now() directo) para
 *    poder fijar la hora en los tests.
 *  - Los topics se crean al arrancar si no existen (solo con Kafka activo).
 *    Los topics de entrada tambien se declaran para que el consumidor no falle
 *    si BC1/BC3 aun no publicaron nada.
 */
@Configuration
@EnableScheduling
public class KafkaConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
    public NewTopic topicEventosNutricion(@Value("${app.kafka.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
    public NewTopic topicEventosPacientes(@Value("${app.kafka.consumo.pacientes-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
    public NewTopic topicEventosCatering(@Value("${app.kafka.consumo.catering-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
