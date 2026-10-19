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

    @Test
    @DisplayName("crear() valida los ingredientes y guarda la receta")
    void crearReceta() {
        Ingrediente ing1 = Ingrediente.reconstruir(1, "Ing 1", "Desc", TipoIngrediente.VEGETAL, true);
        when(ingredienteRepository.buscarPorIds(List.of(1))).thenReturn(List.of(ing1));

        when(recetaRepository.guardar(any(Receta.class)))
                .thenAnswer(inv -> {
                    Receta arg = inv.getArgument(0);
                    return Receta.reconstruir(10, arg.getNombre(), arg.getInstruccion(), true, arg.getIngredientesIds());
                });

        RecetaRequest request = new RecetaRequest("Ensalada", "Picar y mezclar", List.of(1));
        RecetaResponse response = useCases.crear(request);

        assertThat(response.id()).isEqualTo(10);
        assertThat(response.nombre()).isEqualTo("Ensalada");
        assertThat(response.ingredientesIds()).containsExactly(1);
    }

    @Test
    @DisplayName("crear() lanza DomainValidationException cuando faltan ingredientes")
    void crearRecetaIngredientesInvalidos() {
        when(ingredienteRepository.buscarPorIds(List.of(99))).thenReturn(List.of());

        RecetaRequest request = new RecetaRequest("Ensalada", "Picar y mezclar", List.of(99));

        assertThatThrownBy(() -> useCases.crear(request))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("actualizar() modifica nombre, instrucciones e ingredientes de la receta")
    void actualizarReceta() {
        Ingrediente ing1 = Ingrediente.reconstruir(1, "Ing 1", "Desc", TipoIngrediente.VEGETAL, true);
        Ingrediente ing2 = Ingrediente.reconstruir(2, "Ing 2", "Desc", TipoIngrediente.FRUTA, true);
        when(ingredienteRepository.buscarPorIds(List.of(1, 2))).thenReturn(List.of(ing1, ing2));

        Receta existente = Receta.reconstruir(10, "Viejo Nombre", "Vieja Instruccion", true, List.of(1));
        when(recetaRepository.buscarPorId(10)).thenReturn(Optional.of(existente));
        when(recetaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        RecetaRequest request = new RecetaRequest("Nuevo Nombre", "Nueva Instruccion", List.of(1, 2));
        RecetaResponse response = useCases.actualizar(10, request);

        assertThat(response.nombre()).isEqualTo("Nuevo Nombre");
        assertThat(response.ingredientesIds()).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    @DisplayName("agregarIngrediente() agrega un ingrediente si existe")
    void agregarIngrediente() {
        when(ingredienteRepository.existePorId(2)).thenReturn(true);
        Receta existente = Receta.reconstruir(10, "Ensalada", "Instrucciones", true, List.of(1));
        when(recetaRepository.buscarPorId(10)).thenReturn(Optional.of(existente));
        when(recetaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        RecetaResponse response = useCases.agregarIngrediente(10, 2);

        assertThat(response.ingredientesIds()).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    @DisplayName("agregarIngrediente() lanza EntityNotFoundException si el ingrediente no existe")
    void agregarIngredienteInexistente() {
        when(ingredienteRepository.existePorId(99)).thenReturn(false);

        assertThatThrownBy(() -> useCases.agregarIngrediente(10, 99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("quitarIngrediente() remueve un ingrediente de la receta")
    void quitarIngrediente() {
        Receta existente = Receta.reconstruir(10, "Ensalada", "Instrucciones", true, List.of(1, 2));
        when(recetaRepository.buscarPorId(10)).thenReturn(Optional.of(existente));
        when(recetaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        RecetaResponse response = useCases.quitarIngrediente(10, 2);

        assertThat(response.ingredientesIds()).containsExactly(1);
    }

    @Test
    @DisplayName("eliminar() da de baja la receta")
    void eliminarReceta() {
        Receta existente = Receta.reconstruir(10, "Ensalada", "Instrucciones", true, List.of(1));
        when(recetaRepository.buscarPorId(10)).thenReturn(Optional.of(existente));

        useCases.eliminar(10);

        ArgumentCaptor<Receta> captor = ArgumentCaptor.forClass(Receta.class);
        verify(recetaRepository).guardar(captor.capture());
        assertThat(captor.getValue().isActivo()).isFalse();
    }

    @Test
    @DisplayName("obtener() retorna la receta si existe")
    void obtenerReceta() {
        Receta existente = Receta.reconstruir(10, "Ensalada", "Instrucciones", true, List.of(1));
        when(recetaRepository.buscarPorId(10)).thenReturn(Optional.of(existente));

        RecetaResponse response = useCases.obtener(10);

        assertThat(response.id()).isEqualTo(10);
    }

    @Test
    @DisplayName("listar() retorna todas las recetas activas")
    void listarRecetas() {
        Receta existente = Receta.reconstruir(10, "Ensalada", "Instrucciones", true, List.of(1));
        when(recetaRepository.listarActivas()).thenReturn(List.of(existente));

        List<RecetaResponse> list = useCases.listar();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(10);
    }
}
