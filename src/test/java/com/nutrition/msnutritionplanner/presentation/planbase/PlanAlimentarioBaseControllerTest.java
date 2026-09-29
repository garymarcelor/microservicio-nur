package com.nutrition.msnutritionplanner.presentation.planbase;

import com.nutrition.msnutritionplanner.application.planbase.PlanAlimentarioBaseUseCases;
import com.nutrition.msnutritionplanner.application.planbase.dto.PlanBaseResponse;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;
import com.nutrition.msnutritionplanner.presentation.shared.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanAlimentarioBaseController")
class PlanAlimentarioBaseControllerTest {

    @Mock
    private PlanAlimentarioBaseUseCases useCases;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PlanAlimentarioBaseController controller = new PlanAlimentarioBaseController(useCases);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/planes-base con datos validos responde 201")
    void crearPlanBase() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, null, true, List.of());
        when(useCases.crear(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/planes-base")
                        .contentType("application/json")
                        .content("{\"fechaInicioVigencia\":\"2026-03-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estadoPlan").value("INICIADO"));
    }

    @Test
    @DisplayName("POST /api/v1/planes-base sin fechaInicioVigencia responde 400")
    void crearPlanBaseFallaValidacion() throws Exception {
        mockMvc.perform(post("/api/v1/planes-base")
                        .contentType("application/json")
                        .content("{\"fechaInicioVigencia\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/planes-base/{id} existente responde 200")
    void obtenerPlanBase() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, null, true, List.of());
        when(useCases.obtener(1)).thenReturn(response);

        mockMvc.perform(get("/api/v1/planes-base/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/planes-base/{id} inexistente responde 404")
    void obtenerPlanBaseInexistente() throws Exception {
        when(useCases.obtener(99)).thenThrow(new EntityNotFoundException("PlanAlimentarioBase", 99));

        mockMvc.perform(get("/api/v1/planes-base/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/planes-base responde 200")
    void listarPlanesBase() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, null, true, List.of());
        when(useCases.listar()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/planes-base"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/planes-base/{id}/dias responde 200")
    void agregarDia() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, null, true, List.of());
        when(useCases.agregarDia(1)).thenReturn(response);

        mockMvc.perform(post("/api/v1/planes-base/1/dias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/planes-base/{id}/dias/{idDia} responde 200")
    void quitarDia() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, null, true, List.of());
        when(useCases.quitarDia(1, 10)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/planes-base/1/dias/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/planes-base/{id}/dias/{idDia}/tiempos-comida responde 200")
    void agregarTiempoDeComida() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, null, true, List.of());
        when(useCases.agregarTiempoDeComida(eq(1), eq(10), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/planes-base/1/dias/10/tiempos-comida")
                        .contentType("application/json")
                        .content("{\"hora\":\"08:00\",\"recetasIds\":[1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/planes-base/{id}/dias/{idDia}/tiempos-comida/{idTiempo} responde 200")
    void quitarTiempoDeComida() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, null, true, List.of());
        when(useCases.quitarTiempoDeComida(1, 10, 20)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/planes-base/1/dias/10/tiempos-comida/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PATCH /api/v1/planes-base/{id}/completar responde 200")
    void completarPlanBase() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.COMPLETO, null, true, List.of());
        when(useCases.completar(1)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/planes-base/1/completar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPlan").value("COMPLETO"));
    }

    @Test
    @DisplayName("PATCH /api/v1/planes-base/{id}/descartar responde 200")
    void descartarPlanBase() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.DESCARTADO, null, true, List.of());
        when(useCases.descartar(1)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/planes-base/1/descartar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPlan").value("DESCARTADO"));
    }

    @Test
    @DisplayName("DELETE /api/v1/planes-base/{id} responde 204")
    void eliminarPlanBase() throws Exception {
        doNothing().when(useCases).eliminar(1);

        mockMvc.perform(delete("/api/v1/planes-base/1"))
                .andExpect(status().isNoContent());
    }
}
