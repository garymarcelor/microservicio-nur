package com.nutrition.msnutritionplanner.application.receta;

import com.nutrition.msnutritionplanner.application.receta.dto.RecetaRequest;
import com.nutrition.msnutritionplanner.application.receta.dto.RecetaResponse;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.ingrediente.Ingrediente;
import com.nutrition.msnutritionplanner.domain.ingrediente.IngredienteRepository;
import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.domain.receta.RecetaRepository;
import com.nutrition.msnutritionplanner.domain.shared.TipoIngrediente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test del caso de uso Receta: se mockean unicamente los puertos de
 * repositorio (RecetaRepository, IngredienteRepository), nunca una clase
 * concreta de infraestructura. Sigue el patron de IngredienteUseCasesTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Casos de uso: Receta")
class RecetaUseCasesTest {

    @Mock
    private RecetaRepository recetaRepository;

    @Mock
    private IngredienteRepository ingredienteRepository;

    private RecetaUseCases useCases;

    @BeforeEach
    void setUp() {
        useCases = new RecetaUseCases(recetaRepository, ingredienteRepository);
    }

    private Ingrediente ingrediente(int id) {
        return Ingrediente.reconstruir(id, "Ingrediente " + id, "desc", TipoIngrediente.OTRO, true);
    }

    @Test
    @DisplayName("crear() valida que los ingredientes existan, construye la receta y la persiste")
    void creaReceta() {
        RecetaRequest request = new RecetaRequest("Ensalada", "Mezclar todo", List.of(1, 2));
        when(ingredienteRepository.buscarPorIds(List.of(1, 2))).thenReturn(List.of(ingrediente(1), ingrediente(2)));
        when(recetaRepository.guardar(any(Receta.class)))
                .thenAnswer(invocation -> {
                    Receta arg = invocation.getArgument(0);
                    return Receta.reconstruir(1, arg.getNombre(), arg.getInstruccion(), true, arg.getIngredientesIds());
                });

        RecetaResponse response = useCases.crear(request);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.nombre()).isEqualTo("Ensalada");
        assertThat(response.ingredientesIds()).containsExactly(1, 2);
        verify(recetaRepository, times(1)).guardar(any(Receta.class));
    }

    @Test
    @DisplayName("crear() lanza DomainValidationException si algun ingrediente indicado no existe")
    void fallaAlCrearConIngredienteInexistente() {
        RecetaRequest request = new RecetaRequest("Ensalada", "Mezclar todo", List.of(1, 99));
        when(ingredienteRepository.buscarPorIds(List.of(1, 99))).thenReturn(List.of(ingrediente(1)));

        assertThatThrownBy(() -> useCases.crear(request))
                .isInstanceOf(DomainValidationException.class);
        verify(recetaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizar() sincroniza nombre, instruccion e ingredientes de una receta existente")
    void actualizaReceta() {
        Receta existente = Receta.reconstruir(5, "Vieja", "Instruccion vieja", true, List.of(1, 2));
        RecetaRequest request = new RecetaRequest("Nueva", "Instruccion nueva", List.of(2, 3));
        when(ingredienteRepository.buscarPorIds(List.of(2, 3))).thenReturn(List.of(ingrediente(2), ingrediente(3)));
        when(recetaRepository.buscarPorId(5)).thenReturn(Optional.of(existente));
        when(recetaRepository.guardar(any(Receta.class))).thenAnswer(inv -> inv.getArgument(0));

        RecetaResponse response = useCases.actualizar(5, request);

        assertThat(response.nombre()).isEqualTo("Nueva");
        assertThat(response.instruccion()).isEqualTo("Instruccion nueva");
        assertThat(response.ingredientesIds()).containsExactlyInAnyOrder(2, 3);
    }

    @Test
    @DisplayName("actualizar() con un id inexistente lanza EntityNotFoundException")
    void fallaAlActualizarInexistente() {
        RecetaRequest request = new RecetaRequest("Nueva", "Instruccion nueva", List.of(1));
        when(ingredienteRepository.buscarPorIds(List.of(1))).thenReturn(List.of(ingrediente(1)));
        when(recetaRepository.buscarPorId(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCases.actualizar(404, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("agregarIngrediente() agrega el ingrediente a una receta existente")
    void agregaIngrediente() {
        Receta existente = Receta.reconstruir(5, "Ensalada", "Mezclar", true, List.of(1));
        when(ingredienteRepository.existePorId(2)).thenReturn(true);
        when(recetaRepository.buscarPorId(5)).thenReturn(Optional.of(existente));
        when(recetaRepository.guardar(any(Receta.class))).thenAnswer(inv -> inv.getArgument(0));

        RecetaResponse response = useCases.agregarIngrediente(5, 2);

        assertThat(response.ingredientesIds()).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    @DisplayName("agregarIngrediente() con un ingrediente inexistente lanza EntityNotFoundException")
    void fallaAlAgregarIngredienteInexistente() {
        when(ingredienteRepository.existePorId(99)).thenReturn(false);

        assertThatThrownBy(() -> useCases.agregarIngrediente(5, 99))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
        verify(recetaRepository, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("quitarIngrediente() quita el ingrediente indicado de una receta existente")
    void quitaIngrediente() {
        Receta existente = Receta.reconstruir(5, "Ensalada", "Mezclar", true, List.of(1, 2));
        when(recetaRepository.buscarPorId(5)).thenReturn(Optional.of(existente));
        when(recetaRepository.guardar(any(Receta.class))).thenAnswer(inv -> inv.getArgument(0));

        RecetaResponse response = useCases.quitarIngrediente(5, 2);

        assertThat(response.ingredientesIds()).containsExactly(1);
    }

    @Test
    @DisplayName("eliminar() da de baja la receta y la vuelve a guardar")
    void eliminaReceta() {
        Receta existente = Receta.reconstruir(5, "Ensalada", "Mezclar", true, List.of(1));
        when(recetaRepository.buscarPorId(5)).thenReturn(Optional.of(existente));
        when(recetaRepository.guardar(any(Receta.class))).thenAnswer(inv -> inv.getArgument(0));

        useCases.eliminar(5);

        ArgumentCaptor<Receta> captor = ArgumentCaptor.forClass(Receta.class);
        verify(recetaRepository).guardar(captor.capture());
        assertThat(captor.getValue().isActivo()).isFalse();
    }

    @Test
    @DisplayName("obtener() con un id inexistente lanza EntityNotFoundException")
    void fallaAlObtenerInexistente() {
        when(recetaRepository.buscarPorId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCases.obtener(99))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("obtener() traduce la receta encontrada a RecetaResponse")
    void obtieneReceta() {
        Receta existente = Receta.reconstruir(5, "Ensalada", "Mezclar", true, List.of(1));
        when(recetaRepository.buscarPorId(5)).thenReturn(Optional.of(existente));

        RecetaResponse response = useCases.obtener(5);

        assertThat(response.id()).isEqualTo(5);
        assertThat(response.nombre()).isEqualTo("Ensalada");
    }

    @Test
    @DisplayName("listar() traduce cada Receta activa del repositorio a RecetaResponse")
    void listaRecetasActivas() {
        when(recetaRepository.listarActivas()).thenReturn(List.of(
                Receta.reconstruir(1, "Ensalada", "Mezclar", true, List.of(1)),
                Receta.reconstruir(2, "Sopa", "Hervir", true, List.of(2))));

        List<RecetaResponse> respuesta = useCases.listar();

        assertThat(respuesta).hasSize(2);
        assertThat(respuesta).extracting(RecetaResponse::nombre).containsExactly("Ensalada", "Sopa");
    }
}
