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
 * INTEGRATION TEST — Flujo 1: "Elaborar un plan alimentario base".
 *
 * Levanta el contexto completo de Spring (controller -> caso de uso ->
 * repositorio JPA -> base de datos H2 en memoria) y recorre el flujo real
 * de negocio de punta a punta:
 *
 *   1) crear ingredientes
 *   2) crear una receta con esos ingredientes
 *   3) crear un plan alimentario base
 *   4) agregarle un dia
 *   5) agregarle un tiempo de comida (con la receta creada)
 *   6) completar el plan
 *   7) leerlo de vuelta y verificar que todo haya quedado persistido
 *
 * Se ejecuta con "mvn verify" (maven-failsafe-plugin), separado de los
 * unit tests que corren con "mvn test".
 *
 * @Transactional hace que todos los cambios de este test se reviertan al
 * finalizar, para no dejar datos "sucios" para otros tests de integracion.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@DisplayName("[IT] Flujo completo: Plan Alimentario Base")
class PlanAlimentarioBaseFlujoCompletoIT {

    @Autowired
    private WebApplicationContext wac;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    @DisplayName("crea ingredientes -> receta -> plan base -> dia -> tiempo de comida -> completar -> verificar")
    void flujoCompletoPlanBase() throws Exception {

        // 1) Crear dos ingredientes
        int idAvena = crearIngrediente("Avena", "Hojuelas de avena", "CARBOHIDRATO");
        int idPlatano = crearIngrediente("Platano", "Platano fresco", "FRUTA");

        // 2) Crear una receta con esos ingredientes
        String recetaJson = """
                {"nombre":"Avena con platano","instruccion":"Cocinar la avena y agregar el platano",
                 "ingredientesIds":[%d,%d]}
                """.formatted(idAvena, idPlatano);

        MvcResult recetaResult = mockMvc.perform(post("/api/v1/recetas")
                        .contentType("application/json").content(recetaJson))
                .andExpect(status().isCreated())
                .andReturn();
        int idReceta = jsonId(recetaResult);

        // 3) Crear el plan alimentario base
        MvcResult planResult = mockMvc.perform(post("/api/v1/planes-base")
                        .contentType("application/json")
                        .content("{\"fechaInicioVigencia\":\"2026-09-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPlan").value("INICIADO"))
                .andReturn();
        int idPlan = jsonId(planResult);

        // 4) Agregar un dia al plan
        MvcResult diaResult = mockMvc.perform(post("/api/v1/planes-base/{id}/dias", idPlan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias.length()").value(1))
                .andReturn();
        JsonNode planConDia = objectMapper.readTree(diaResult.getResponse().getContentAsString());
        int idDia = planConDia.at("/dias/0/id").asInt();

        // 5) Agregar un tiempo de comida a ese dia, usando la receta creada
        String tiempoJson = """
                {"hora":"07:30:00","recetasIds":[%d]}
                """.formatted(idReceta);

        mockMvc.perform(post("/api/v1/planes-base/{id}/dias/{idDia}/tiempos-comida", idPlan, idDia)
                        .contentType("application/json").content(tiempoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias[0].tiemposDeComida.length()").value(1))
                .andExpect(jsonPath("$.dias[0].tiemposDeComida[0].recetasIds[0]").value(idReceta));

        // 6) Completar el plan
        mockMvc.perform(patch("/api/v1/planes-base/{id}/completar", idPlan))
                .andDo(result -> {
                    if (result.getResponse().getStatus() != 200) {
                        java.nio.file.Files.writeString(java.nio.file.Paths.get("/tmp/error.txt"), 
                                "STATUS: " + result.getResponse().getStatus() + "\n" +
                                "BODY: " + result.getResponse().getContentAsString() + "\n" +
                                "EX: " + result.getResolvedException());
                    }
                })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPlan").value("COMPLETO"));

        // 7) Volver a leerlo y verificar que todo haya quedado persistido
        MvcResult finalResult = mockMvc.perform(get("/api/v1/planes-base/{id}", idPlan))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode planFinal = objectMapper.readTree(finalResult.getResponse().getContentAsString());
        assertThat(planFinal.get("estadoPlan").asText()).isEqualTo("COMPLETO");
        assertThat(planFinal.get("dias")).hasSize(1);
        assertThat(planFinal.at("/dias/0/tiemposDeComida/0/hora").asText()).isEqualTo("07:30:00");
        assertThat(planFinal.at("/dias/0/tiemposDeComida/0/recetasIds/0").asInt()).isEqualTo(idReceta);
    }

    @Test
    @DisplayName("completar() sin dias responde 400 (regla de negocio expuesta via API)")
    void completarSinDiasResponde400() throws Exception {
        MvcResult planResult = mockMvc.perform(post("/api/v1/planes-base")
                        .contentType("application/json")
                        .content("{\"fechaInicioVigencia\":\"2026-09-01\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        int idPlan = jsonId(planResult);

        mockMvc.perform(patch("/api/v1/planes-base/{id}/completar", idPlan))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
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
