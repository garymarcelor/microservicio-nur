package com.nutrition.msnutritionplanner.presentation.ingrediente;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.msnutritionplanner.application.ingrediente.IngredienteUseCases;
import com.nutrition.msnutritionplanner.application.ingrediente.dto.IngredienteRequest;
import com.nutrition.msnutritionplanner.application.ingrediente.dto.IngredienteResponse;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.shared.TipoIngrediente;
import com.nutrition.msnutritionplanner.presentation.shared.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit test de la capa presentation: se levanta SOLO el controller (modo
 * standalone, sin contexto de Spring completo) y se mockea el caso de uso.
 * Es rapido (no requiere base de datos) y sirve para verificar el mapeo
 * HTTP <-> DTO y el manejo de errores (via GlobalExceptionHandler).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IngredienteController")
class IngredienteControllerTest {

    @Mock
    private IngredienteUseCases ingredienteUseCases;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        IngredienteController controller = new IngredienteController(ingredienteUseCases);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/ingredientes con datos validos responde 201 con el ingrediente creado")
    void creaIngrediente() throws Exception {
        IngredienteRequest request = new IngredienteRequest("Palta", "Palta fresca", TipoIngrediente.GRASA);
        IngredienteResponse response = new IngredienteResponse(1, "Palta", "Palta fresca", TipoIngrediente.GRASA, true);
        when(ingredienteUseCases.crear(request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/ingredientes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Palta"));
    }

    @Test
    @DisplayName("POST /api/v1/ingredientes sin nombre responde 400 (Bean Validation)")
    void fallaValidacionSinNombre() throws Exception {
        IngredienteRequest request = new IngredienteRequest("", "desc", TipoIngrediente.GRASA);

        mockMvc.perform(post("/api/v1/ingredientes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/ingredientes/{id} inexistente responde 404")
    void obtenerInexistenteResponde404() throws Exception {
        when(ingredienteUseCases.obtener(99)).thenThrow(new EntityNotFoundException("Ingrediente", 99));

        mockMvc.perform(get("/api/v1/ingredientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/v1/ingredientes/{id} responde 204")
    void eliminaIngrediente() throws Exception {
        mockMvc.perform(delete("/api/v1/ingredientes/1"))
                .andExpect(status().isNoContent());
    }
}
