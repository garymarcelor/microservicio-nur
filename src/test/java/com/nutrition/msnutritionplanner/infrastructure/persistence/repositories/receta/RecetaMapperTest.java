package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.receta;

import com.nutrition.msnutritionplanner.domain.receta.IngredienteCantidad;
import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.domain.shared.UnidadMedida;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaIngredienteJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecetaMapper")
class RecetaMapperTest {

    private RecetaMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RecetaMapper();
    }

    private static IngredienteJpaEntity ingredienteJpa(Integer id) {
        IngredienteJpaEntity ing = new IngredienteJpaEntity();
        ing.setId(id);
        return ing;
    }

    private static RecetaIngredienteJpaEntity fila(RecetaJpaEntity receta, Integer idIngrediente,
                                                   String cantidad, UnidadMedida unidad) {
        RecetaIngredienteJpaEntity fila = new RecetaIngredienteJpaEntity();
        fila.setReceta(receta);
        fila.setIngrediente(ingredienteJpa(idIngrediente));
        fila.setCantidad(new BigDecimal(cantidad));
        fila.setUnidad(unidad);
        return fila;
    }

    @Test
    @DisplayName("toDomain() convierte una RecetaJpaEntity con sus ingredientes a dominio")
    void toDomain() {
        RecetaJpaEntity entity = new RecetaJpaEntity();
        entity.setId(1);
        entity.setNombre("Ensalada");
        entity.setInstruccion("Mezclar");
        entity.setPorcionGramos(250);
        entity.setActivo(true);
        entity.getIngredientes().add(fila(entity, 100, "180", UnidadMedida.GRAMOS));
        entity.getIngredientes().add(fila(entity, 101, "10", UnidadMedida.MILILITROS));

        Receta receta = mapper.toDomain(entity);

        assertThat(receta.getId()).isEqualTo(1);
        assertThat(receta.getNombre()).isEqualTo("Ensalada");
        assertThat(receta.getInstruccion()).isEqualTo("Mezclar");
        assertThat(receta.getPorcionGramos()).isEqualTo(250);
        assertThat(receta.isActivo()).isTrue();
        assertThat(receta.getIngredientesIds()).containsExactly(100, 101);
        assertThat(receta.getIngredientes()).containsExactly(
                new IngredienteCantidad(100, new BigDecimal("180"), UnidadMedida.GRAMOS),
                new IngredienteCantidad(101, new BigDecimal("10"), UnidadMedida.MILILITROS));
    }

    @Test
    @DisplayName("copyScalarFields() copia los campos escalares de Receta a la entidad JPA")
    void copyScalarFields() {
        Receta receta = Receta.reconstruir(1, "Ensalada Nueva", "Instruccion nueva", 300, true,
                List.of(new IngredienteCantidad(100, new BigDecimal("50"), UnidadMedida.GRAMOS)));
        RecetaJpaEntity destino = new RecetaJpaEntity();

        mapper.copyScalarFields(receta, destino);

        assertThat(destino.getNombre()).isEqualTo("Ensalada Nueva");
        assertThat(destino.getInstruccion()).isEqualTo("Instruccion nueva");
        assertThat(destino.getPorcionGramos()).isEqualTo(300);
        assertThat(destino.isActivo()).isTrue();
    }

    @Test
    @DisplayName("mergeIngredientes() actualiza cantidad/unidad de un ingrediente existente")
    void mergeActualizaExistente() {
        RecetaJpaEntity destino = new RecetaJpaEntity();
        RecetaIngredienteJpaEntity existente = fila(destino, 100, "50", UnidadMedida.GRAMOS);
        destino.getIngredientes().add(existente);
        Map<Integer, IngredienteJpaEntity> porId = new HashMap<>();
        porId.put(100, existente.getIngrediente());

        mapper.mergeIngredientes(destino,
                List.of(new IngredienteCantidad(100, new BigDecimal("75"), UnidadMedida.MILILITROS)), porId);

        assertThat(destino.getIngredientes()).hasSize(1);
        assertThat(destino.getIngredientes().get(0)).isSameAs(existente);
        assertThat(existente.getCantidad()).isEqualByComparingTo("75");
        assertThat(existente.getUnidad()).isEqualTo(UnidadMedida.MILILITROS);
    }

    @Test
    @DisplayName("mergeIngredientes() agrega un ingrediente nuevo")
    void mergeAgregaNuevo() {
        RecetaJpaEntity destino = new RecetaJpaEntity();
        RecetaIngredienteJpaEntity existente = fila(destino, 100, "50", UnidadMedida.GRAMOS);
        destino.getIngredientes().add(existente);
        IngredienteJpaEntity nuevoIng = ingredienteJpa(101);
        Map<Integer, IngredienteJpaEntity> porId = new HashMap<>();
        porId.put(100, existente.getIngrediente());
        porId.put(101, nuevoIng);

        mapper.mergeIngredientes(destino, List.of(
                new IngredienteCantidad(100, new BigDecimal("50"), UnidadMedida.GRAMOS),
                new IngredienteCantidad(101, new BigDecimal("2"), UnidadMedida.UNIDADES)), porId);

        assertThat(destino.getIngredientes()).hasSize(2);
        RecetaIngredienteJpaEntity nueva = destino.getIngredientes().get(1);
        assertThat(nueva.getReceta()).isSameAs(destino);
        assertThat(nueva.getIngrediente()).isSameAs(nuevoIng);
        assertThat(nueva.getCantidad()).isEqualByComparingTo("2");
        assertThat(nueva.getUnidad()).isEqualTo(UnidadMedida.UNIDADES);
    }

    @Test
    @DisplayName("mergeIngredientes() elimina el ingrediente que ya no esta en el dominio")
    void mergeEliminaAusente() {
        RecetaJpaEntity destino = new RecetaJpaEntity();
        RecetaIngredienteJpaEntity f1 = fila(destino, 100, "50", UnidadMedida.GRAMOS);
        RecetaIngredienteJpaEntity f2 = fila(destino, 101, "20", UnidadMedida.GRAMOS);
        destino.getIngredientes().add(f1);
        destino.getIngredientes().add(f2);
        Map<Integer, IngredienteJpaEntity> porId = new HashMap<>();
        porId.put(100, f1.getIngrediente());
        porId.put(101, f2.getIngrediente());

        mapper.mergeIngredientes(destino,
                List.of(new IngredienteCantidad(100, new BigDecimal("50"), UnidadMedida.GRAMOS)), porId);

        assertThat(destino.getIngredientes()).containsExactly(f1);
    }
}
