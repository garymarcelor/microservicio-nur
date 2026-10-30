package com.nutrition.msnutritionplanner.domain.receta;

import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.shared.UnidadMedida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agregado Receta")
class RecetaTest {

    private static IngredienteCantidad ing(int id) {
        return new IngredienteCantidad(id, new BigDecimal("100"), UnidadMedida.GRAMOS);
    }

    @Test
    @DisplayName("crear() construye una receta activa con sus ingredientes (por id)")
    void creaRecetaValida() {
        Receta receta = Receta.crear("Omelette", "Batir y cocinar", 200, List.of(ing(1), ing(2), ing(3)));

        assertThat(receta.getNombre()).isEqualTo("Omelette");
        assertThat(receta.isActivo()).isTrue();
        assertThat(receta.getIngredientesIds()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("crear() sin ingredientes lanza DomainValidationException")
    void fallaSinIngredientes() {
        assertThatThrownBy(() -> Receta.crear("Omelette", "instruccion", 200, List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("ingrediente");
    }

    @Test
    @DisplayName("crear() sin nombre lanza DomainValidationException")
    void fallaSinNombre() {
        assertThatThrownBy(() -> Receta.crear(" ", "instruccion", 200, List.of(ing(1))))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("agregarIngrediente() agrega un nuevo id sin duplicar")
    void agregaIngrediente() {
        Receta receta = Receta.crear("Omelette", "instruccion", 200, List.of(ing(1)));

        receta.agregarIngrediente(ing(2));
        receta.agregarIngrediente(ing(1)); // duplicado, el Set no lo repite

        assertThat(receta.getIngredientesIds()).containsExactly(1, 2);
    }

    @Test
    @DisplayName("quitarIngrediente() elimina el id indicado si quedan mas de uno")
    void quitaIngrediente() {
        Receta receta = Receta.crear("Omelette", "instruccion", 200, List.of(ing(1), ing(2)));

        receta.quitarIngrediente(1);

        assertThat(receta.getIngredientesIds()).containsExactly(2);
    }

    @Test
    @DisplayName("quitarIngrediente() no permite dejar la receta sin ingredientes")
    void noPermiteQuitarElUltimoIngrediente() {
        Receta receta = Receta.crear("Omelette", "instruccion", 200, List.of(ing(1)));

        assertThatThrownBy(() -> receta.quitarIngrediente(1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("al menos un ingrediente");
    }

    @Test
    @DisplayName("darDeBaja() impide seguir modificando la receta")
    void noPermiteModificarRecetaDadaDeBaja() {
        Receta receta = Receta.crear("Omelette", "instruccion", 200, List.of(ing(1)));
        receta.darDeBaja();

        assertThat(receta.isActivo()).isFalse();
        assertThatThrownBy(() -> receta.agregarIngrediente(ing(2)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> receta.actualizarDatos("Otro nombre", "otra instruccion", 300))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("actualizarDatos() modifica nombre e instruccion de una receta activa")
    void actualizaDatos() {
        Receta receta = Receta.crear("Omelette", "instruccion original", 200, List.of(ing(1)));

        receta.actualizarDatos("Omelette de espinaca", "instruccion nueva", 300);

        assertThat(receta.getNombre()).isEqualTo("Omelette de espinaca");
        assertThat(receta.getInstruccion()).isEqualTo("instruccion nueva", 300);
    }
}
