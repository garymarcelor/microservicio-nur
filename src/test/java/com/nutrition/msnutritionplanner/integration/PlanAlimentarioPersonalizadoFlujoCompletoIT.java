package com.nutrition.msnutritionplanner.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * INTEGRATION TEST — Flujo 2: "Personalizar un plan alimentario".
 *
 * Corresponde al caso de estudio: dentro del servicio de Catering, el
 * paciente solicita un cambio sobre su plan ya asignado, lo que genera
 * una nueva version (PlanAlimentarioPersonalizado) derivada del plan
 * base original.
 *
 *   1) preparar un plan base COMPLETO (con un dia y un tiempo de comida)
 *   2) crear una version personalizada a partir de ese plan base
 *   3) agregarle un dia y un tiempo de comida propios
 *   4) completar la version personalizada
 *   5) verificar que quedo bien enlazada al plan base (idPlanBase) y que
 *      el plan base original NO fue modificado por los cambios del
 *      plan personalizado (independencia entre agregados)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@DisplayName("[IT] Flujo completo: Plan Alimentario Personalizado")
class PlanAlimentarioPersonalizadoFlujoCompletoIT {

    @Autowired
    private WebApplicationContext wac;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    @DisplayName("crea plan personalizado desde un plan base COMPLETO, lo edita y lo completa")
    void flujoCompletoPlanPersonalizado() throws Exception {

        // --- Preparacion: ingrediente + receta + plan base COMPLETO ---
        int idIngrediente = crearIngrediente("Quinua", "Quinua cocida", "CARBOHIDRATO");
        int idReceta = crearReceta("Quinua sola", "Cocinar la quinua en agua", idIngrediente);
        int idPlanBase = crearPlanBaseCompleto("2026-09-01", idReceta);

        // 1) Crear la version personalizada a partir del plan base
        String personalizadoJson = """
                {"idPlanBase":%d,"descripcionCambios":"El paciente pidio evitar lacteos"}
                """.formatted(idPlanBase);

        MvcResult personalizadoResult = mockMvc.perform(post("/api/v1/planes-personalizados")
                        .contentType("application/json").content(personalizadoJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPlanBase").value(idPlanBase))
                .andExpect(jsonPath("$.estadoPlan").value("INICIADO"))
                .andReturn();
        int idPersonalizado = jsonId(personalizadoResult);

        // 2) Agregarle su propio dia y tiempo de comida
        MvcResult diaResult = mockMvc.perform(post("/api/v1/planes-personalizados/{id}/dias", idPersonalizado))
                .andExpect(status().isOk())
                .andReturn();
        int idDia = objectMapper.readTree(diaResult.getResponse().getContentAsString())
                .at("/dias/0/id").asInt();

        String tiempoJson = """
                {"hora":"08:00:00","recetasIds":[%d]}
                """.formatted(idReceta);
        mockMvc.perform(post("/api/v1/planes-personalizados/{id}/dias/{idDia}/tiempos-comida", idPersonalizado, idDia)
                        .contentType("application/json").content(tiempoJson))
                .andExpect(status().isOk());

        // 3) Completar el plan personalizado
        mockMvc.perform(patch("/api/v1/planes-personalizados/{id}/completar", idPersonalizado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPlan").value("COMPLETO"));

        // 4) Verificar el enlace hacia el plan base y que aparezca en su listado
        mockMvc.perform(get("/api/v1/planes-personalizados").param("idPlanBase", String.valueOf(idPlanBase)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(idPersonalizado))
                .andExpect(jsonPath("$[0].idPlanBase").value(idPlanBase));

        // 5) Verificar que el plan BASE original sigue intacto (1 solo dia,
        //    el que se creo en la preparacion), es decir, ambos agregados
        //    son independientes entre si.
        MvcResult planBaseResult = mockMvc.perform(get("/api/v1/planes-base/{id}", idPlanBase))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode planBase = objectMapper.readTree(planBaseResult.getResponse().getContentAsString());
        assertThat(planBase.get("estadoPlan").asText()).isEqualTo("COMPLETO");
        assertThat(planBase.get("dias")).hasSize(1);
    }

    @Test
    @DisplayName("crear un plan personalizado desde un plan base que NO esta COMPLETO responde 400")
    void fallaSiElPlanBaseNoEstaCompleto() throws Exception {
        MvcResult planResult = mockMvc.perform(post("/api/v1/planes-base")
                        .contentType("application/json")
                        .content("{\"fechaInicioVigencia\":\"2026-09-01\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        int idPlanIniciado = jsonId(planResult); // se queda en INICIADO, nunca se completa

        String personalizadoJson = """
                {"idPlanBase":%d,"descripcionCambios":"cambio"}
                """.formatted(idPlanIniciado);

        mockMvc.perform(post("/api/v1/planes-personalizados")
                        .contentType("application/json").content(personalizadoJson))
                .andExpect(status().isBadRequest());
    }

    // ---------- Helpers de preparacion (reutilizables entre tests de este flujo) ----------

    private int crearPlanBaseCompleto(String fecha, int idReceta) throws Exception {
        MvcResult planResult = mockMvc.perform(post("/api/v1/planes-base")
                        .contentType("application/json")
                        .content("{\"fechaInicioVigencia\":\"%s\"}".formatted(fecha)))
                .andExpect(status().isCreated())
                .andReturn();
        int idPlan = jsonId(planResult);

        MvcResult diaResult = mockMvc.perform(post("/api/v1/planes-base/{id}/dias", idPlan))
                .andExpect(status().isOk())
                .andReturn();
        int idDia = objectMapper.readTree(diaResult.getResponse().getContentAsString())
                .at("/dias/0/id").asInt();

        String tiempoJson = """
                {"hora":"07:00:00","recetasIds":[%d]}
                """.formatted(idReceta);
        mockMvc.perform(post("/api/v1/planes-base/{id}/dias/{idDia}/tiempos-comida", idPlan, idDia)
                        .contentType("application/json").content(tiempoJson))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/planes-base/{id}/completar", idPlan))
                .andExpect(status().isOk());

        return idPlan;
    }

    private int crearReceta(String nombre, String instruccion, int idIngrediente) throws Exception {
        String json = """
                {"nombre":"%s","instruccion":"%s","ingredientesIds":[%d]}
                """.formatted(nombre, instruccion, idIngrediente);
        MvcResult result = mockMvc.perform(post("/api/v1/recetas")
                        .contentType("application/json").content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return jsonId(result);
    }

    private int crearIngrediente(String nombre, String descripcion, String tipo) throws Exception {
        String json = """
                {"nombre":"%s","descripcion":"%s","tipo":"%s"}
                """.formatted(nombre, descripcion, tipo);
        MvcResult result = mockMvc.perform(post("/api/v1/ingredientes")
                        .contentType("application/json").content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return jsonId(result);
    }

    private int jsonId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asInt();
    }
}
