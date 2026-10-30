package com.nutrition.msnutritionplanner.infrastructure.messaging.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

/**
 * Formato JSON de los eventos que BC2 CONSUME, tal como los publican
 * MsPatientsConsulCore (BC1) y MsCateringSubscription (BC3) segun
 * TareaActividad2-tabla-corregida.md.
 *
 * Todos llevan {@code @JsonIgnoreProperties(ignoreUnknown = true)}: si el
 * productor agrega campos nuevos en una version futura del contrato, BC2 no
 * se rompe (principio de tolerancia: "se estricto al enviar, tolerante al recibir").
 */
public final class EventosEntrada {

    private EventosEntrada() {
    }

    /**
     * Sobre comun. {@code data} se deja como arbol JSON y se convierte al
     * record concreto segun {@code eventName}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sobre(String eventId, String eventName, String eventVersion, String occurredOn,
                        String source, String correlationId, JsonNode data) {
    }

    /** 1 · bc1.PacienteRegistrado */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PacienteRegistrado(String idPaciente, String nombreCompleto, String fechaNacimiento,
                                     String sexo, String telefono, String email,
                                     String idNutricionista, String fechaRegistro) {
    }

    /** 2 · bc1.AntecedenteClinicoRegistrado */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AntecedenteClinicoRegistrado(String idPaciente, String idAntecedente, String tipo,
                                               String descripcion, String severidad,
                                               List<String> ingredientesRestringidos,
                                               String idNutricionista, String fechaRegistro) {
    }

    /** 3 · bc1.NutricionistaRegistrado */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NutricionistaRegistrado(String idNutricionista, String nombreCompleto,
                                          String matriculaProfesional, String especialidad,
                                          String email, Boolean activo, String fechaAlta) {
    }

    /** 5 · bc1.EvaluacionRealizada */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvaluacionRealizada(String idEvaluacion, String idPaciente, String idSuscripcion,
                                      String idNutricionista, String tipoEvaluacion,
                                      String fechaEvaluacion, BigDecimal pesoKg, BigDecimal imc,
                                      String observaciones, Boolean requiereAjustePlan) {
    }

    /** 6 · bc1.ProximaEvaluacionProgramada */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProximaEvaluacionProgramada(String idPaciente, String idEvaluacionAnterior,
                                              String idNutricionista, String fechaProximaEvaluacion,
                                              String tipoEvaluacion, Boolean requiereAnalisisClinico) {
    }

    /** 13 · bc3.SuscripcionCancelada */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SuscripcionCancelada(String idSuscripcion, String idPaciente, String idPlan,
                                       String tipoBaja, String motivo, String fechaEfectiva,
                                       Integer entregasPendientesAnuladas) {
    }
}
