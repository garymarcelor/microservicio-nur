package com.nutrition.msnutritionplanner.infrastructure.messaging.out;

import com.nutrition.msnutritionplanner.application.shared.event.CorrelationContext;
import com.nutrition.msnutritionplanner.domain.ingrediente.Ingrediente;
import com.nutrition.msnutritionplanner.domain.ingrediente.IngredienteRepository;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.receta.IngredienteCantidad;
import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.domain.receta.RecetaRepository;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;
import com.nutrition.msnutritionplanner.domain.shared.Hora;
import com.nutrition.msnutritionplanner.domain.shared.Orden;
import com.nutrition.msnutritionplanner.domain.shared.TipoIngrediente;
import com.nutrition.msnutritionplanner.domain.shared.UnidadMedida;
import com.nutrition.msnutritionplanner.domain.planbase.Dia;
import com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizado;
import com.nutrition.msnutritionplanner.infrastructure.outbox.OutboxJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.outbox.SpringDataOutboxRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica que el JSON que se guarda en el outbox coincide con los contratos
 * de TareaActividad2-tabla-corregida.md (eventos 7, 8 y 9).
 * Es un test unitario puro: el repositorio del outbox y los de dominio estan simulados.
 */
@ExtendWith(MockitoExtension.class)
class OutboxIntegrationEventPublisherTest {

    @Mock
    private SpringDataOutboxRepository outbox;
    @Mock
    private RecetaRepository recetaRepository;
    @Mock
    private IngredienteRepository ingredienteRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private OutboxIntegrationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-10T16:00:00Z"), ZoneOffset.UTC);
        publisher = new OutboxIntegrationEventPublisher(outbox, recetaRepository, ingredienteRepository,
                jsonMapper, reloj, "bc2.nutricion.eventos", "MSNutritionPlanner");
    }

    @AfterEach
    void limpiar() {
        CorrelationContext.limpiar();
    }

    private JsonNode guardado(String esperadoEvento, String esperadaKey) {
        ArgumentCaptor<OutboxJpaEntity> captor = ArgumentCaptor.forClass(OutboxJpaEntity.class);
        verify(outbox).save(captor.capture());
        OutboxJpaEntity fila = captor.getValue();
        assertThat(fila.getEventName()).isEqualTo(esperadoEvento);
        assertThat(fila.getMessageKey()).isEqualTo(esperadaKey);
        assertThat(fila.getTopic()).isEqualTo("bc2.nutricion.eventos");
        assertThat(fila.getPublishedAt()).as("debe quedar PENDIENTE").isNull();
        return jsonMapper.readTree(fila.getPayload());
    }

    private Receta receta(int id, String nombre, int porcion) {
        return Receta.reconstruir(id, nombre, "Cocinar", porcion, true,
                List.of(new IngredienteCantidad(12, new BigDecimal("180.00"), UnidadMedida.GRAMOS),
                        new IngredienteCantidad(7, new BigDecimal("10"), UnidadMedida.MILILITROS)));
    }

    @Test
    @DisplayName("recetaCreada() guarda el sobre y el data de bc2.RecetaCreada segun el contrato")
    void recetaCreada() {
        when(ingredienteRepository.buscarPorIds(anyList())).thenReturn(List.of(
                Ingrediente.reconstruir(12, "Pechuga de pollo", "x", TipoIngrediente.PROTEINA, true),
                Ingrediente.reconstruir(7, "Aceite de oliva", "x", TipoIngrediente.GRASA, true)));

        publisher.recetaCreada(receta(45, "Pechuga con quinua", 350));

        JsonNode json = guardado("bc2.RecetaCreada", "REC-45");
        assertThat(json.get("eventName").asString()).isEqualTo("bc2.RecetaCreada");
        assertThat(json.get("eventVersion").asString()).isEqualTo("1.0");
        assertThat(json.get("source").asString()).isEqualTo("MSNutritionPlanner");
        // 16:00 UTC = 12:00 en Bolivia: el contrato exige el desfase -04:00
        assertThat(json.get("occurredOn").asString()).isEqualTo("2026-10-10T12:00:00-04:00");
        assertThat(json.get("eventId").asString()).hasSize(36);
        assertThat(json.get("correlationId").asString()).isEqualTo("corr-catalogo-REC-45");

        JsonNode data = json.get("data");
        assertThat(data.get("id").asString()).isEqualTo("REC-45");
        assertThat(data.get("nombre").asString()).isEqualTo("Pechuga con quinua");
        assertThat(data.get("porcionGramos").asInt()).isEqualTo(350);
        assertThat(data.get("activo").asBoolean()).isTrue();
        JsonNode ing = data.get("ingredientes").get(0);
        assertThat(ing.get("idIngrediente").asString()).isEqualTo("ING-12");
        assertThat(ing.get("nombre").asString()).isEqualTo("Pechuga de pollo");
        // 180.00 debe viajar como 180 (sin ceros ni notacion cientifica)
        assertThat(ing.get("cantidad").toString()).isEqualTo("180");
        assertThat(ing.get("unidad").asString()).isEqualTo("GRAMOS");
        assertThat(data.get("ingredientes").get(1).get("unidad").asString()).isEqualTo("MILILITROS");
    }

    @Test
    @DisplayName("planBaseCreado() incluye dias, tiempos con hora HH:mm y recetas con nombre y porcion")
    void planBaseCreado() {
        Receta r14 = receta(14, "Avena", 250);
        when(recetaRepository.buscarPorIds(anyList())).thenReturn(List.of(r14));
        Hora ocho = Hora.of(8, 0);
        TiempoDeComida tc = TiempoDeComida.reconstruir(8001, ocho, List.of(14));
        Dia dia = Dia.reconstruir(2001, Orden.of(1), List.of(tc));
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(
                41, EstadoPlan.COMPLETO, LocalDate.of(2026, 10, 22), true, List.of(dia));

        publisher.planBaseCreado(plan);

        JsonNode json = guardado("bc2.PlanBaseCreado", "PLAN-41");
        assertThat(json.get("correlationId").asString()).isEqualTo("corr-catalogo-PLAN-41");
        JsonNode data = json.get("data");
        assertThat(data.get("id").asString()).isEqualTo("PLAN-41");
        assertThat(data.get("estadoPlan").asString()).isEqualTo("COMPLETO");
        assertThat(data.get("fechaInicioVigencia").asString()).isEqualTo("2026-10-22");
        assertThat(data.get("duracionDias").asInt()).isEqualTo(1);
        JsonNode d = data.get("dias").get(0);
        assertThat(d.get("id").asString()).isEqualTo("DIA-2001");
        assertThat(d.get("numOrden").asInt()).isEqualTo(1);
        assertThat(d.get("nroTiemposComida").asInt()).isEqualTo(1);
        JsonNode t = d.get("tiemposDeComida").get(0);
        assertThat(t.get("id").asString()).isEqualTo("TC-8001");
        assertThat(t.get("hora").asString()).isEqualTo("08:00");
        JsonNode rec = t.get("recetas").get(0);
        assertThat(rec.get("idReceta").asString()).isEqualTo("REC-14");
        assertThat(rec.get("nombre").asString()).isEqualTo("Avena");
        assertThat(rec.get("porcionGramos").asInt()).isEqualTo(250);
    }

    @Test
    @DisplayName("planPersonalizadoCreado() incluye idPlanBase, idPaciente y descripcionCambios")
    void planPersonalizadoCreado() {
        when(recetaRepository.buscarPorIds(anyList())).thenReturn(List.of(receta(14, "Avena", 250)));
        Dia dia = Dia.reconstruir(3001, Orden.of(1),
                List.of(TiempoDeComida.reconstruir(9001, Hora.of(8, 0), List.of(14))));
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(
                205, 41, "PAC-1024", "Se reemplaza mani por girasol", EstadoPlan.COMPLETO,
                LocalDate.of(2026, 10, 22), true, List.of(dia));

        publisher.planPersonalizadoCreado(plan);

        JsonNode json = guardado("bc2.PlanPersonalizadoCreado", "PLAN-205");
        assertThat(json.get("correlationId").asString()).isEqualTo("corr-alta-PAC-1024");
        JsonNode data = json.get("data");
        assertThat(data.get("id").asString()).isEqualTo("PLAN-205");
        assertThat(data.get("idPlanBase").asString()).isEqualTo("PLAN-41");
        assertThat(data.get("idPaciente").asString()).isEqualTo("PAC-1024");
        assertThat(data.get("descripcionCambios").asString()).isEqualTo("Se reemplaza mani por girasol");
    }

    @Test
    @DisplayName("El correlationId del flujo en curso tiene prioridad sobre el valor por defecto")
    void correlationIdPropagado() {
        when(ingredienteRepository.buscarPorIds(anyList())).thenReturn(List.of());
        CorrelationContext.establecer("corr-flujo-externo");

        publisher.recetaCreada(receta(1, "Ensalada", 200));

        assertThat(guardado("bc2.RecetaCreada", "REC-1").get("correlationId").asString())
                .isEqualTo("corr-flujo-externo");
    }

    @Test
    @DisplayName("normalizar() quita ceros finales sin usar notacion cientifica")
    void normalizar() {
        assertThat(OutboxIntegrationEventPublisher.normalizar(new BigDecimal("180.00")).toPlainString()).isEqualTo("180");
        assertThat(OutboxIntegrationEventPublisher.normalizar(new BigDecimal("1200")).toPlainString()).isEqualTo("1200");
        assertThat(OutboxIntegrationEventPublisher.normalizar(new BigDecimal("2.50")).toPlainString()).isEqualTo("2.5");
    }

    @Test
    @DisplayName("enHoraDeBolivia() da el desfase -04:00 sin fracciones de segundo, aunque cambie el dia")
    void enHoraDeBolivia() {
        assertThat(OutboxIntegrationEventPublisher.enHoraDeBolivia(Instant.parse("2026-10-23T02:15:30.987Z")))
                .isEqualTo("2026-10-22T22:15:30-04:00");
    }
}
