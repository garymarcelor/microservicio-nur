package com.nutrition.msnutritionplanner.domain.ingrediente;

import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.shared.TipoIngrediente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agregado Ingrediente")
class IngredienteTest {

    @Test
    @DisplayName("crear() construye un ingrediente activo con los datos indicados")
    void creaIngredienteValido() {
        Ingrediente ingrediente = Ingrediente.crear("Palta", "Palta fresca", TipoIngrediente.GRASA);

        assertThat(ingrediente.getNombre()).isEqualTo("Palta");
        assertThat(ingrediente.getDescripcion()).isEqualTo("Palta fresca");
        assertThat(ingrediente.getTipo()).isEqualTo(TipoIngrediente.GRASA);
        assertThat(ingrediente.isActivo()).isTrue();
        assertThat(ingrediente.getId()).isNull();
    }

    @Test
    @DisplayName("crear() sin nombre lanza DomainValidationException")
    void fallaSinNombre() {
        assertThatThrownBy(() -> Ingrediente.crear("  ", "desc", TipoIngrediente.FRUTA))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    @DisplayName("crear() sin tipo lanza DomainValidationException")
    void fallaSinTipo() {
        assertThatThrownBy(() -> Ingrediente.crear("Manzana", "desc", null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("tipo");
    }

    @Test
    @DisplayName("actualizarDatos() modifica nombre, descripcion y tipo")
    void actualizaDatos() {
        Ingrediente ingrediente = Ingrediente.crear("Manzana", "Manzana roja", TipoIngrediente.FRUTA);

        ingrediente.actualizarDatos("Manzana verde", "Manzana verde acida", TipoIngrediente.FRUTA);

        assertThat(ingrediente.getNombre()).isEqualTo("Manzana verde");
        assertThat(ingrediente.getDescripcion()).isEqualTo("Manzana verde acida");
    }

    @Test
    @DisplayName("darDeBaja() marca el ingrediente como inactivo")
    void daDeBaja() {
        Ingrediente ingrediente = Ingrediente.crear("Sal", "Sal de mesa", TipoIngrediente.CONDIMENTO);

        ingrediente.darDeBaja();

        assertThat(ingrediente.isActivo()).isFalse();
    }

    @Test
    @DisplayName("actualizarDatos() sobre un ingrediente dado de baja lanza BusinessRuleException")
    void fallaAlActualizarInactivo() {
        Ingrediente ingrediente = Ingrediente.crear("Sal", "Sal de mesa", TipoIngrediente.CONDIMENTO);
        ingrediente.darDeBaja();

        assertThatThrownBy(() -> ingrediente.actualizarDatos("Sal marina", "desc", TipoIngrediente.CONDIMENTO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("reactivar() vuelve a marcar el ingrediente como activo")
    void reactivaIngrediente() {
        Ingrediente ingrediente = Ingrediente.crear("Sal", "Sal de mesa", TipoIngrediente.CONDIMENTO);
        ingrediente.darDeBaja();

        ingrediente.reactivar();

        assertThat(ingrediente.isActivo()).isTrue();
    }

    @Test
    @DisplayName("reconstruir() respeta el id y el estado activo recibidos (uso de infraestructura)")
    void reconstruyeDesdePersistencia() {
        Ingrediente ingrediente = Ingrediente.reconstruir(42, "Quinua", "Quinua cocida", TipoIngrediente.CARBOHIDRATO, false);

        assertThat(ingrediente.getId()).isEqualTo(42);
        assertThat(ingrediente.isActivo()).isFalse();
    }
}
