package com.nutrition.msnutritionplanner.presentation.planbase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nutrition.msnutritionplanner.application.planbase.PlanAlimentarioBaseUseCases;
import com.nutrition.msnutritionplanner.application.planbase.dto.CrearPlanBaseRequest;
import com.nutrition.msnutritionplanner.application.planbase.dto.PlanBaseResponse;
import com.nutrition.msnutritionplanner.application.planbase.dto.TiempoDeComidaRequest;
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
@DisplayName("PlanAlimentarioBaseController")
class PlanAlimentarioBaseControllerTest {

    @Mock
    private PlanAlimentarioBaseUseCases useCases;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        PlanAlimentarioBaseController controller = new PlanAlimentarioBaseController(useCases);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/planes-base con datos validos responde 201")
    void crear() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(1);
        CrearPlanBaseRequest request = new CrearPlanBaseRequest(fecha);
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, fecha, true, Collections.emptyList());

        when(useCases.crear(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/planes-base")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/planes-base/{id} responde 200 con el plan")
    void obtener() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.obtener(1)).thenReturn(response);

        mockMvc.perform(get("/api/v1/planes-base/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/planes-base/{id} inexistente responde 404")
    void obtenerInexistente() throws Exception {
        when(useCases.obtener(99)).thenThrow(new EntityNotFoundException("PlanAlimentarioBase", 99));

        mockMvc.perform(get("/api/v1/planes-base/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/planes-base responde 200 con la lista")
    void listar() throws Exception {
        when(useCases.listar()).thenReturn(List.of(
                new PlanBaseResponse(1, EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList())
        ));

        mockMvc.perform(get("/api/v1/planes-base"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/planes-base/{id}/dias agrega un dia y responde 200")
    void agregarDia() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.agregarDia(1)).thenReturn(response);

        mockMvc.perform(post("/api/v1/planes-base/1/dias"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/planes-base/{id}/dias/{idDia} quita un dia y responde 200")
    void quitarDia() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.quitarDia(1, 2)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/planes-base/1/dias/2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/planes-base/{id}/dias/{idDia}/tiempos-comida agrega tiempo y responde 200")
    void agregarTiempoDeComida() throws Exception {
        TiempoDeComidaRequest request = new TiempoDeComidaRequest(LocalTime.of(8, 0), List.of(1));
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.agregarTiempoDeComida(eq(1), eq(2), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/planes-base/1/dias/2/tiempos-comida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/planes-base/{id}/dias/{idDia}/tiempos-comida/{idTiempo} quita tiempo y responde 200")
    void quitarTiempoDeComida() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.quitarTiempoDeComida(1, 2, 3)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/planes-base/1/dias/2/tiempos-comida/3"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/v1/planes-base/{id}/completar responde 200")
    void completar() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.COMPLETO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.completar(1)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/planes-base/1/completar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPlan").value("COMPLETO"));
    }

    @Test
    @DisplayName("PATCH /api/v1/planes-base/{id}/descartar responde 200")
    void descartar() throws Exception {
        PlanBaseResponse response = new PlanBaseResponse(1, EstadoPlan.DESCARTADO, LocalDate.now(), true, Collections.emptyList());
        when(useCases.descartar(1)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/planes-base/1/descartar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPlan").value("DESCARTADO"));
    }

    @Test
    @DisplayName("DELETE /api/v1/planes-base/{id} responde 204")
    void eliminar() throws Exception {
        doNothing().when(useCases).eliminar(1);

        mockMvc.perform(delete("/api/v1/planes-base/1"))
                .andExpect(status().isNoContent());
    }
}
