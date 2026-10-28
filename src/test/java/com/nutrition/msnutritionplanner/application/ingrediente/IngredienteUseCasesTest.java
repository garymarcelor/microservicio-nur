package com.nutrition.msnutritionplanner.application.ingrediente;

import com.nutrition.msnutritionplanner.application.ingrediente.dto.IngredienteRequest;
import com.nutrition.msnutritionplanner.application.ingrediente.dto.IngredienteResponse;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.ingrediente.Ingrediente;
import com.nutrition.msnutritionplanner.domain.ingrediente.IngredienteRepository;
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
@DisplayName("Casos de uso: Ingrediente")
class IngredienteUseCasesTest {

    @Mock
    private IngredienteRepository ingredienteRepository;

    private IngredienteUseCases useCases;

    @BeforeEach
    void setUp() {
        useCases = new IngredienteUseCases(ingredienteRepository);
    }

    @Test
    @DisplayName("crear() construye el ingrediente de dominio y lo persiste")
    void creaIngrediente() {
        IngredienteRequest request = new IngredienteRequest("Palta", "Palta fresca", TipoIngrediente.GRASA);
        when(ingredienteRepository.guardar(any(Ingrediente.class)))
                .thenAnswer(invocation -> {
                    Ingrediente arg = invocation.getArgument(0);
                    return Ingrediente.reconstruir(1, arg.getNombre(), arg.getDescripcion(), arg.getTipo(), true);
                });

        IngredienteResponse response = useCases.crear(request);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.nombre()).isEqualTo("Palta");
        verify(ingredienteRepository, times(1)).guardar(any(Ingrediente.class));
    }

    @Test
    @DisplayName("actualizar() modifica el ingrediente existente y lo persiste")
    void actualizaIngrediente() {
        Ingrediente existente = Ingrediente.reconstruir(1, "Palta", "Palta fresca", TipoIngrediente.GRASA, true);
        when(ingredienteRepository.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(ingredienteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        IngredienteRequest request = new IngredienteRequest("Palta Hass", "Palta madura", TipoIngrediente.GRASA);
        IngredienteResponse response = useCases.actualizar(1, request);

        assertThat(response.nombre()).isEqualTo("Palta Hass");
        assertThat(response.descripcion()).isEqualTo("Palta madura");
    }

    @Test
    @DisplayName("obtener() retorna el ingrediente existente")
    void obtieneIngrediente() {
        Ingrediente existente = Ingrediente.reconstruir(1, "Palta", "Palta fresca", TipoIngrediente.GRASA, true);
        when(ingredienteRepository.buscarPorId(1)).thenReturn(Optional.of(existente));

        IngredienteResponse response = useCases.obtener(1);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.nombre()).isEqualTo("Palta");
    }

    @Test
    @DisplayName("obtener() con un id inexistente lanza EntityNotFoundException")
    void fallaAlObtenerInexistente() {
        when(ingredienteRepository.buscarPorId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCases.obtener(99))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("eliminar() da de baja el ingrediente y lo vuelve a guardar")
    void eliminaIngrediente() {
        Ingrediente existente = Ingrediente.reconstruir(5, "Sal", "Sal de mesa", TipoIngrediente.CONDIMENTO, true);
        when(ingredienteRepository.buscarPorId(5)).thenReturn(Optional.of(existente));
        when(ingredienteRepository.guardar(any(Ingrediente.class))).thenAnswer(inv -> inv.getArgument(0));

        useCases.eliminar(5);

        ArgumentCaptor<Ingrediente> captor = ArgumentCaptor.forClass(Ingrediente.class);
        verify(ingredienteRepository).guardar(captor.capture());
        assertThat(captor.getValue().isActivo()).isFalse();
    }

    @Test
    @DisplayName("listar() traduce cada Ingrediente activo del repositorio a IngredienteResponse")
    void listaIngredientesActivos() {
        when(ingredienteRepository.listarActivos()).thenReturn(List.of(
                Ingrediente.reconstruir(1, "Palta", "desc", TipoIngrediente.GRASA, true),
                Ingrediente.reconstruir(2, "Manzana", "desc", TipoIngrediente.FRUTA, true)));

        List<IngredienteResponse> respuesta = useCases.listar();

        assertThat(respuesta).hasSize(2);
        assertThat(respuesta).extracting(IngredienteResponse::nombre).containsExactly("Palta", "Manzana");
    }
}
