package com.nutrition.msnutritionplanner.presentation.receta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.msnutritionplanner.application.receta.RecetaUseCases;
import com.nutrition.msnutritionplanner.application.receta.dto.IngredienteRecetaRequest;
import com.nutrition.msnutritionplanner.application.receta.dto.RecetaRequest;
import com.nutrition.msnutritionplanner.application.receta.dto.RecetaResponse;
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
@DisplayName("RecetaController")
class RecetaControllerTest {

    @Mock
    private RecetaUseCases recetaUseCases;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RecetaController controller = new RecetaController(recetaUseCases);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/recetas con datos validos responde 201")
    void crearReceta() throws Exception {
        RecetaRequest request = new RecetaRequest("Ensalada", "Ensalada verde", "Mezclar todo");
        RecetaResponse response = new RecetaResponse(1, "Ensalada", "Ensalada verde", "Mezclar todo", 0.0, 0.0, 0.0, 0.0, true, List.of());
        when(recetaUseCases.crear(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/recetas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ensalada"));
    }

    @Test
    @DisplayName("POST /api/v1/recetas sin nombre responde 400")
    void crearRecetaFallaValidacion() throws Exception {
        RecetaRequest request = new RecetaRequest("", "desc", "prep");

        mockMvc.perform(post("/api/v1/recetas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/recetas/{id} con datos validos responde 200")
    void actualizarReceta() throws Exception {
        RecetaRequest request = new RecetaRequest("Ensalada Editada", "Desc edit", "Prep edit");
        RecetaResponse response = new RecetaResponse(1, "Ensalada Editada", "Desc edit", "Prep edit", 0.0, 0.0, 0.0, 0.0, true, List.of());
        when(recetaUseCases.actualizar(eq(1), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/recetas/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ensalada Editada"));
    }

    @Test
    @DisplayName("POST /api/v1/recetas/{id}/ingredientes responde 200")
    void agregarIngrediente() throws Exception {
        IngredienteRecetaRequest request = new IngredienteRecetaRequest(5);
        RecetaResponse response = new RecetaResponse(1, "Ensalada", "Desc", "Prep", 0.0, 0.0, 0.0, 0.0, true, List.of());
        when(recetaUseCases.agregarIngrediente(1, 5)).thenReturn(response);

        mockMvc.perform(post("/api/v1/recetas/1/ingredientes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/recetas/{id}/ingredientes/{idIngrediente} responde 200")
    void quitarIngrediente() throws Exception {
        RecetaResponse response = new RecetaResponse(1, "Ensalada", "Desc", "Prep", 0.0, 0.0, 0.0, 0.0, true, List.of());
        when(recetaUseCases.quitarIngrediente(1, 5)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/recetas/1/ingredientes/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/recetas/{id} existente responde 200")
    void obtenerReceta() throws Exception {
        RecetaResponse response = new RecetaResponse(1, "Ensalada", "Desc", "Prep", 0.0, 0.0, 0.0, 0.0, true, List.of());
        when(recetaUseCases.obtener(1)).thenReturn(response);

        mockMvc.perform(get("/api/v1/recetas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/recetas/{id} inexistente responde 404")
    void obtenerRecetaInexistente() throws Exception {
        when(recetaUseCases.obtener(99)).thenThrow(new EntityNotFoundException("Receta", 99));

        mockMvc.perform(get("/api/v1/recetas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/recetas responde 200")
    void listarRecetas() throws Exception {
        RecetaResponse response = new RecetaResponse(1, "Ensalada", "Desc", "Prep", 0.0, 0.0, 0.0, 0.0, true, List.of());
        when(recetaUseCases.listar()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/recetas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/recetas/{id} responde 204")
    void eliminarReceta() throws Exception {
        doNothing().when(recetaUseCases).eliminar(1);

        mockMvc.perform(delete("/api/v1/recetas/1"))
                .andExpect(status().isNoContent());
    }
}
