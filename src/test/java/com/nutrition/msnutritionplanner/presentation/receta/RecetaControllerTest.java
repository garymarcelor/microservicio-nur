package com.nutrition.msnutritionplanner.presentation.receta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.msnutritionplanner.application.receta.RecetaUseCases;
import com.nutrition.msnutritionplanner.application.receta.dto.IngredienteRecetaRequest;
import com.nutrition.msnutritionplanner.application.receta.dto.RecetaRequest;
import com.nutrition.msnutritionplanner.application.receta.dto.RecetaResponse;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.shared.UnidadMedida;
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

import java.math.BigDecimal;
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
    @DisplayName("POST /api/v1/recetas sin porcionGramos responde 400")
    void crearSinPorcionGramos() throws Exception {
        String json = "{\"nombre\":\"Ensalada\",\"instruccion\":\"Picar todo\","
                + "\"ingredientes\":[{\"idIngrediente\":1,\"cantidad\":50,\"unidad\":\"GRAMOS\"}]}";

        mockMvc.perform(post("/api/v1/recetas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    /** Construye ingredientes de receta de prueba (50 g cada uno). */
    private static List<IngredienteRecetaRequest> ingredientes(Integer... ids) {
        return java.util.Arrays.stream(ids)
                .map(id -> new IngredienteRecetaRequest(id, new BigDecimal("50"), UnidadMedida.GRAMOS))
                .toList();
    }

    /** Construye una respuesta de receta de prueba con porcion de 250 g. */
    private static RecetaResponse respuesta(Integer id, String nombre, String instruccion, List<Integer> ids) {
        List<RecetaResponse.IngredienteDetalle> detalle = ids.stream()
                .map(i -> new RecetaResponse.IngredienteDetalle(i, new BigDecimal("50"), UnidadMedida.GRAMOS))
                .toList();
        return new RecetaResponse(id, nombre, instruccion, 250, true, ids, detalle);
    }

    @Test
    @DisplayName("POST /api/v1/recetas crea una receta y responde 201")
    void crear() throws Exception {
        RecetaRequest request = new RecetaRequest("Ensalada", "Picar todo", 250, ingredientes(1));
        RecetaResponse response = respuesta(10, "Ensalada", "Picar todo", List.of(1));

        when(recetaUseCases.crear(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/recetas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nombre").value("Ensalada"));
    }

    @Test
    @DisplayName("PUT /api/v1/recetas/{id} actualiza la receta y responde 200")
    void actualizar() throws Exception {
        RecetaRequest request = new RecetaRequest("Ensalada Caesar", "Picar y aderezar", 300, ingredientes(1, 2));
        RecetaResponse response = respuesta(10, "Ensalada Caesar", "Picar y aderezar", List.of(1, 2));

        when(recetaUseCases.actualizar(eq(10), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/recetas/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ensalada Caesar"));
    }

    @Test
    @DisplayName("POST /api/v1/recetas/{id}/ingredientes agrega un ingrediente y responde 200")
    void agregarIngrediente() throws Exception {
        IngredienteRecetaRequest request = new IngredienteRecetaRequest(2, new BigDecimal("50"), UnidadMedida.GRAMOS);
        RecetaResponse response = respuesta(10, "Ensalada", "Picar todo", List.of(1, 2));

        when(recetaUseCases.agregarIngrediente(eq(10), any(IngredienteRecetaRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/recetas/10/ingredientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredientesIds").value(org.hamcrest.Matchers.hasItems(1, 2)));
    }

    @Test
    @DisplayName("DELETE /api/v1/recetas/{id}/ingredientes/{idIngrediente} quita un ingrediente y responde 200")
    void quitarIngrediente() throws Exception {
        RecetaResponse response = respuesta(10, "Ensalada", "Picar todo", List.of(1));

        when(recetaUseCases.quitarIngrediente(10, 2)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/recetas/10/ingredientes/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredientesIds").value(org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/recetas/{id} obtiene la receta")
    void obtener() throws Exception {
        RecetaResponse response = respuesta(10, "Ensalada", "Picar todo", List.of(1));
        when(recetaUseCases.obtener(10)).thenReturn(response);

        mockMvc.perform(get("/api/v1/recetas/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/recetas/{id} inexistente responde 404")
    void obtenerInexistente() throws Exception {
        when(recetaUseCases.obtener(99)).thenThrow(new EntityNotFoundException("Receta", 99));

        mockMvc.perform(get("/api/v1/recetas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/recetas lista todas las recetas activas")
    void listar() throws Exception {
        RecetaResponse response = respuesta(10, "Ensalada", "Picar todo", List.of(1));
        when(recetaUseCases.listar()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/recetas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("DELETE /api/v1/recetas/{id} elimina la receta y responde 204")
    void eliminar() throws Exception {
        doNothing().when(recetaUseCases).eliminar(10);

        mockMvc.perform(delete("/api/v1/recetas/10"))
                .andExpect(status().isNoContent());
    }
}
