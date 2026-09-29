package com.nutrition.msnutritionplanner.application.planbase;

import com.nutrition.msnutritionplanner.application.planbase.dto.CrearPlanBaseRequest;
import com.nutrition.msnutritionplanner.application.planbase.dto.PlanBaseResponse;
import com.nutrition.msnutritionplanner.application.planbase.dto.TiempoDeComidaRequest;
import com.nutrition.msnutritionplanner.domain.exceptions.BusinessRuleException;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.planbase.Dia;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBaseRepository;
import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.domain.receta.RecetaRepository;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;
import com.nutrition.msnutritionplanner.domain.shared.Hora;
import com.nutrition.msnutritionplanner.domain.shared.Orden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test del caso de uso PlanAlimentarioBase: se mockean unicamente los
 * puertos de repositorio (PlanAlimentarioBaseRepository, RecetaRepository).
 * Sigue el patron de IngredienteUseCasesTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Casos de uso: PlanAlimentarioBase")
class PlanAlimentarioBaseUseCasesTest {

    @Mock
    private PlanAlimentarioBaseRepository planRepository;

    @Mock
    private RecetaRepository recetaRepository;

    private PlanAlimentarioBaseUseCases useCases;

    @BeforeEach
    void setUp() {
        useCases = new PlanAlimentarioBaseUseCases(planRepository, recetaRepository);
    }

    private Receta receta(int id) {
        return Receta.reconstruir(id, "Receta " + id, "Instruccion", true, List.of(1));
    }

    /** Plan INICIADO con dos dias, cada uno con un tiempo de comida ya cargado. */
    private PlanAlimentarioBase planConDosDias() {
        Dia dia1 = Dia.reconstruir(10, Orden.of(1),
                List.of(com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida.reconstruir(
                        100, Hora.of(8, 0), List.of(1))));
        Dia dia2 = Dia.reconstruir(11, Orden.of(2),
                List.of(com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida.reconstruir(
                        101, Hora.of(13, 0), List.of(1))));
        return PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.of(2026, 1, 1), true,
                List.of(dia1, dia2));
    }

    @Test
    @DisplayName("crear() construye el plan de dominio y lo persiste")
    void creaPlan() {
        CrearPlanBaseRequest request = new CrearPlanBaseRequest(LocalDate.of(2026, 3, 1));
        when(planRepository.guardar(any(PlanAlimentarioBase.class)))
                .thenAnswer(inv -> {
                    PlanAlimentarioBase arg = inv.getArgument(0);
                    return PlanAlimentarioBase.reconstruir(1, arg.getEstadoPlan(), arg.getFechaInicioVigencia(),
                            arg.isActivo(), arg.getDias());
                });

        PlanBaseResponse response = useCases.crear(request);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.estadoPlan()).isEqualTo(EstadoPlan.INICIADO);
        verify(planRepository, times(1)).guardar(any(PlanAlimentarioBase.class));
    }

    @Test
    @DisplayName("agregarDia() agrega un dia mas al plan existente")
    void agregaDia() {
        PlanAlimentarioBase plan = planConDosDias();
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanBaseResponse response = useCases.agregarDia(1);

        assertThat(response.dias()).hasSize(3);
    }

    @Test
    @DisplayName("agregarDia() con un plan inexistente lanza EntityNotFoundException")
    void fallaAlAgregarDiaPlanInexistente() {
        when(planRepository.buscarPorId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCases.agregarDia(99))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("quitarDia() quita el dia indicado y reordena los restantes")
    void quitaDia() {
        PlanAlimentarioBase plan = planConDosDias();
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanBaseResponse response = useCases.quitarDia(1, 10);

        assertThat(response.dias()).hasSize(1);
        assertThat(response.dias().get(0).id()).isEqualTo(11);
        assertThat(response.dias().get(0).numOrden()).isEqualTo(1);
    }

    @Test
    @DisplayName("quitarDia() propaga BusinessRuleException si el plan solo tiene un dia")
    void fallaAlQuitarUnicoDia() {
        Dia unico = Dia.reconstruir(10, Orden.of(1), List.of());
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO,
                LocalDate.of(2026, 1, 1), true, List.of(unico));
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> useCases.quitarDia(1, 10))
                .isInstanceOf(BusinessRuleException.class);
        verify(planRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("agregarTiempoDeComida() valida que las recetas existan y las asigna al dia")
    void agregaTiempoDeComida() {
        PlanAlimentarioBase plan = planConDosDias();
        TiempoDeComidaRequest request = new TiempoDeComidaRequest(LocalTime.of(20, 0), List.of(1));
        when(recetaRepository.buscarPorIds(List.of(1))).thenReturn(List.of(receta(1)));
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanBaseResponse response = useCases.agregarTiempoDeComida(1, 10, request);

        assertThat(response.dias().get(0).nroTiemposComida()).isEqualTo(2);
    }

    @Test
    @DisplayName("agregarTiempoDeComida() lanza DomainValidationException si alguna receta no existe")
    void fallaAlAgregarTiempoDeComidaConRecetaInexistente() {
        TiempoDeComidaRequest request = new TiempoDeComidaRequest(LocalTime.of(20, 0), List.of(99));
        when(recetaRepository.buscarPorIds(List.of(99))).thenReturn(List.of());

        assertThatThrownBy(() -> useCases.agregarTiempoDeComida(1, 10, request))
                .isInstanceOf(DomainValidationException.class);
        verify(planRepository, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("quitarTiempoDeComida() quita el tiempo de comida indicado del dia")
    void quitaTiempoDeComida() {
        Dia dia1 = Dia.reconstruir(10, Orden.of(1), List.of(
                com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida.reconstruir(100, Hora.of(8, 0), List.of(1)),
                com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida.reconstruir(101, Hora.of(13, 0), List.of(1))));
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO,
                LocalDate.of(2026, 1, 1), true, List.of(dia1));
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanBaseResponse response = useCases.quitarTiempoDeComida(1, 10, 100);

        assertThat(response.dias().get(0).nroTiemposComida()).isEqualTo(1);
    }

    @Test
    @DisplayName("completar() marca el plan como COMPLETO cuando esta INICIADO y tiene dias")
    void completaPlan() {
        PlanAlimentarioBase plan = planConDosDias();
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanBaseResponse response = useCases.completar(1);

        assertThat(response.estadoPlan()).isEqualTo(EstadoPlan.COMPLETO);
    }

    @Test
    @DisplayName("completar() propaga BusinessRuleException si el plan no esta INICIADO")
    void fallaAlCompletarPlanNoIniciado() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.DESCARTADO,
                LocalDate.of(2026, 1, 1), true, List.of(Dia.reconstruir(10, Orden.of(1), List.of())));
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> useCases.completar(1)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("descartar() marca el plan como DESCARTADO")
    void descartaPlan() {
        PlanAlimentarioBase plan = planConDosDias();
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanBaseResponse response = useCases.descartar(1);

        assertThat(response.estadoPlan()).isEqualTo(EstadoPlan.DESCARTADO);
    }

    @Test
    @DisplayName("eliminar() da de baja el plan y lo vuelve a guardar")
    void eliminaPlan() {
        PlanAlimentarioBase plan = planConDosDias();
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        useCases.eliminar(1);

        ArgumentCaptor<PlanAlimentarioBase> captor = ArgumentCaptor.forClass(PlanAlimentarioBase.class);
        verify(planRepository).guardar(captor.capture());
        assertThat(captor.getValue().isActivo()).isFalse();
    }

    @Test
    @DisplayName("obtener() con un id inexistente lanza EntityNotFoundException")
    void fallaAlObtenerInexistente() {
        when(planRepository.buscarPorId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCases.obtener(99))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("obtener() traduce el plan encontrado a PlanBaseResponse")
    void obtienePlan() {
        PlanAlimentarioBase plan = planConDosDias();
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));

        PlanBaseResponse response = useCases.obtener(1);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.dias()).hasSize(2);
    }

    @Test
    @DisplayName("listar() traduce cada PlanAlimentarioBase activo del repositorio a PlanBaseResponse")
    void listaPlanesActivos() {
        when(planRepository.listarActivos()).thenReturn(List.of(planConDosDias()));

        List<PlanBaseResponse> respuesta = useCases.listar();

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.get(0).id()).isEqualTo(1);
    }
}
