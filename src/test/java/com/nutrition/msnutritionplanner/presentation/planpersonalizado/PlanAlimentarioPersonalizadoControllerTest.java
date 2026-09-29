package com.nutrition.msnutritionplanner.presentation.planpersonalizado;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.msnutritionplanner.application.planbase.dto.TiempoDeComidaRequest;
import com.nutrition.msnutritionplanner.application.planpersonalizado.PlanAlimentarioPersonalizadoUseCases;
import com.nutrition.msnutritionplanner.application.planpersonalizado.dto.CrearPlanPersonalizadoRequest;
import com.nutrition.msnutritionplanner.application.planpersonalizado.dto.PlanPersonalizadoResponse;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
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
@DisplayName("PlanAlimentarioPersonalizadoController")
class PlanAlimentarioPersonalizadoControllerTest {

    @Mock
    private PlanAlimentarioPersonalizadoUseCases useCases;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        PlanAlimentarioPersonalizadoController controller = new PlanAlimentarioPersonalizadoController(useCases);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/planes-personalizados con datos validos responde 201")
    void crearPlanPersonalizado() throws Exception {
        CrearPlanPersonalizadoRequest request = new CrearPlanPersonalizadoRequest(1, 2001, "Plan modificado");
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(1, 1, 2001, null, "Plan modificado", "BORRADOR", true, List.of());
        when(useCases.crear(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/planes-personalizados")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.idPlanBase").value(1))
                .andExpect(jsonPath("$.idPaciente").value(2001));
    }

    @Test
    @DisplayName("POST /api/v1/planes-personalizados sin idPlanBase responde 400")
    void crearPlanPersonalizadoFallaValidacion() throws Exception {
        CrearPlanPersonalizadoRequest request = new CrearPlanPersonalizadoRequest(null, 2001, "Desc");

        mockMvc.perform(post("/api/v1/planes-personalizados")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/planes-personalizados/{id} existente responde 200")
    void obtenerPlanPersonalizado() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(1, 1, 2001, null, "Plan modificado", "BORRADOR", true, List.of());
        when(useCases.obtener(1)).thenReturn(response);

        mockMvc.perform(get("/api/v1/planes-personalizados/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/planes-personalizados/{id} inexistente responde 404")
    void obtenerPlanPersonalizadoInexistente() throws Exception {
        when(useCases.obtener(99)).thenThrow(new EntityNotFoundException("PlanAlimentarioPersonalizado", 99));

        mockMvc.perform(get("/api/v1/planes-personalizados/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/planes-personalizados responde 200")
    void listarPlanesPersonalizadosPorPlanBase() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(1, 1, 2001, null, "Plan modificado", "BORRADOR", true, List.of());
        when(useCases.listarPorPlanBase(1)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/planes-personalizados")
                        .param("idPlanBase", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/planes-personalizados/{id}/dias responde 200")
    void agregarDia() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(1, 1, 2001, null, "Plan modificado", "BORRADOR", true, List.of());
        when(useCases.agregarDia(1)).thenReturn(response);

        mockMvc.perform(post("/api/v1/planes-personalizados/1/dias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/planes-personalizados/{id}/dias/{idDia} responde 200")
    void quitarDia() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(1, 1, 2001, null, "Plan modificado", "BORRADOR", true, List.of());
        when(useCases.quitarDia(1, 10)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/planes-personalizados/1/dias/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/planes-personalizados/{id}/dias/{idDia}/tiempos-comida responde 200")
    void agregarTiempoDeComida() throws Exception {
        TiempoDeComidaRequest request = new TiempoDeComidaRequest("DESAYUNO", "8:00 AM", List.of());
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(1, 1, 2001, null, "Plan modificado", "BORRADOR", true, List.of());
        when(useCases.agregarTiempoDeComida(eq(1), eq(10), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/planes-personalizados/1/dias/10/tiempos-comida")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/planes-personalizados/{id}/dias/{idDia}/tiempos-comida/{idTiempo} responde 200")
    void quitarTiempoDeComida() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(1, 1, 2001, null, "Plan modificado", "BORRADOR", true, List.of());
        when(useCases.quitarTiempoDeComida(1, 10, 20)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/planes-personalizados/1/dias/10/tiempos-comida/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PATCH /api/v1/planes-personalizados/{id}/completar responde 200")
    void completarPlanPersonalizado() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(1, 1, 2001, null, "Plan modificado", "COMPLETO", true, List.of());
        when(useCases.completar(1)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/planes-personalizados/1/completar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETO"));
    }

    @Test
    @DisplayName("PATCH /api/v1/planes-personalizados/{id}/descartar responde 200")
    void descartarPlanPersonalizado() throws Exception {
        PlanPersonalizadoResponse response = new PlanPersonalizadoResponse(1, 1, 2001, null, "Plan modificado", "DESCARTADO", true, List.of());
        when(useCases.descartar(1)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/planes-personalizados/1/descartar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESCARTADO"));
    }

    @Test
    @DisplayName("DELETE /api/v1/planes-personalizados/{id} responde 204")
    void eliminarPlanPersonalizado() throws Exception {
        doNothing().when(useCases).eliminar(1);

        mockMvc.perform(delete("/api/v1/planes-personalizados/1"))
                .andExpect(status().isNoContent());
    }
}
