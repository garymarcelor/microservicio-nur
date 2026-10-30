package com.nutrition.msnutritionplanner.infrastructure.messaging.out;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cargas utiles ("data") de los eventos de integracion que PUBLICA BC2,
 * con los nombres de campo EXACTOS de TareaActividad2-tabla-corregida.md.
 * El orden de los componentes de cada record es el orden del JSON resultante.
 *
 * Son clases de infraestructura: el dominio no conoce el formato del
 * contrato; si el contrato cambia, solo cambia esta capa.
 */
public final class EventosSalida {

    private EventosSalida() {
    }

    /** Sobre comun a todos los eventos (envelope del contrato). */
    public record Sobre<T>(
            String eventId,
            String eventName,
            String eventVersion,
            String occurredOn,
            String source,
            String correlationId,
            T data) {
    }

    // ------------------------------------------------------------------
    // 7 · bc2.PlanBaseCreado  y  8 · bc2.PlanPersonalizadoCreado
    // ------------------------------------------------------------------

    /** Receta dentro de un tiempo de comida (con nombre y porcion, para que Catering no consulte a BC2). */
    public record RecetaEnTiempo(String idReceta, String nombre, Integer porcionGramos) {
    }

    public record TiempoComidaData(String id, String hora, List<RecetaEnTiempo> recetas) {
    }

    public record DiaData(String id, Integer numOrden, Integer nroTiemposComida, List<TiempoComidaData> tiemposDeComida) {
    }

    public record PlanBaseCreadoData(
            String id,
            String estadoPlan,
            String fechaInicioVigencia,
            Integer duracionDias,
            boolean activo,
            List<DiaData> dias) {
    }

    public record PlanPersonalizadoCreadoData(
            String id,
            String idPlanBase,
            String idPaciente,
            String descripcionCambios,
            String estadoPlan,
            String fechaInicioVigencia,
            Integer duracionDias,
            boolean activo,
            List<DiaData> dias) {
    }

    // ------------------------------------------------------------------
    // 9 · bc2.RecetaCreada
    // ------------------------------------------------------------------

    public record IngredienteData(String idIngrediente, String nombre, BigDecimal cantidad, String unidad) {
    }

    public record RecetaCreadaData(
            String id,
            String nombre,
            String instruccion,
            Integer porcionGramos,
            boolean activo,
            List<IngredienteData> ingredientes) {
    }
}
