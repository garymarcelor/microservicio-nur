package com.nutrition.msnutritionplanner.presentation.planpersonalizado;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nutrition.msnutritionplanner.application.planbase.dto.TiempoDeComidaRequest;
import com.nutrition.msnutritionplanner.application.planpersonalizado.PlanAlimentarioPersonalizadoUseCases;
import com.nutrition.msnutritionplanner.application.planpersonalizado.dto.CrearPlanPersonalizadoRequest;
import com.nutrition.msnutritionplanner.application.planpersonalizado.dto.PlanPersonalizadoResponse;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;
import com.nutrition.msnutritionplanner.presentation.shared.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanAlimentarioPersonalizadoController")
class PlanAlimentarioPersonalizadoControllerTest {

    @Mock
    private PlanAlimentarioPersonalizadoUseCases useCases;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        PlanAlimentarioPersonalizadoController controller = new PlanAlimentarioPersonalizadoController(useCases);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/planes-personalizados responde 201")
    void crear() throws Exception {
        CrearPlanPersonalizadoRequest request = new CrearPlanPersonalizadoRequest(1, "PAC-1", "Motivo cambio");
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(10, 1, "PAC-1", "Motivo cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());

        when(useCases.crear(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/planes-personalizados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.idPlanBase").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/planes-personalizados/{id} responde 200")
    void obtener() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(10, 1, "PAC-1", "Motivo cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.obtener(10)).thenReturn(response);

        mockMvc.perform(get("/api/v1/planes-personalizados/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/planes-personalizados/{id} inexistente responde 404")
    void obtenerInexistente() throws Exception {
        when(useCases.obtener(99)).thenThrow(new EntityNotFoundException("PlanAlimentarioPersonalizado", 99));

        mockMvc.perform(get("/api/v1/planes-personalizados/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/planes-personalizados?idPlanBase=1 responde 200")
    void listarPorPlanBase() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(10, 1, "PAC-1", "Motivo cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.listarPorPlanBase(1)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/planes-personalizados?idPlanBase=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("POST /api/v1/planes-personalizados/{id}/dias agrega un dia y responde 200")
    void agregarDia() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(10, 1, "PAC-1", "Motivo cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.agregarDia(10)).thenReturn(response);

        mockMvc.perform(post("/api/v1/planes-personalizados/10/dias"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/planes-personalizados/{id}/dias/{idDia} quita un dia y responde 200")
    void quitarDia() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(10, 1, "PAC-1", "Motivo cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.quitarDia(10, 2)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/planes-personalizados/10/dias/2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/planes-personalizados/{id}/dias/{idDia}/tiempos-comida agrega tiempo y responde 200")
    void agregarTiempoDeComida() throws Exception {
        TiempoDeComidaRequest request = new TiempoDeComidaRequest(LocalTime.of(8, 0), List.of(1));
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(10, 1, "PAC-1", "Motivo cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.agregarTiempoDeComida(eq(10), eq(2), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/planes-personalizados/10/dias/2/tiempos-comida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/planes-personalizados/{id}/dias/{idDia}/tiempos-comida/{idTiempo} quita tiempo y responde 200")
    void quitarTiempoDeComida() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(10, 1, "PAC-1", "Motivo cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.quitarTiempoDeComida(10, 2, 3)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/planes-personalizados/10/dias/2/tiempos-comida/3"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/v1/planes-personalizados/{id}/completar responde 200")
    void completar() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(10, 1, "PAC-1", "Motivo cambio", EstadoPlan.COMPLETO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.completar(10)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/planes-personalizados/10/completar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPlan").value("COMPLETO"));
    }

    @Test
    @DisplayName("PATCH /api/v1/planes-personalizados/{id}/descartar responde 200")
    void descartar() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(10, 1, "PAC-1", "Motivo cambio", EstadoPlan.DESCARTADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.descartar(10)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/planes-personalizados/10/descartar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPlan").value("DESCARTADO"));
    }

    @Test
    @DisplayName("DELETE /api/v1/planes-personalizados/{id} responde 204")
    void eliminar() throws Exception {
        doNothing().when(useCases).eliminar(10);

        mockMvc.perform(delete("/api/v1/planes-personalizados/10"))
                .andExpect(status().isNoContent());
    }
}
