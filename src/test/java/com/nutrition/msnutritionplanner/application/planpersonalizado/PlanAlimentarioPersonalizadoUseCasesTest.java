package com.nutrition.msnutritionplanner.application.planpersonalizado;

import com.nutrition.msnutritionplanner.application.planbase.dto.TiempoDeComidaRequest;
import com.nutrition.msnutritionplanner.application.planpersonalizado.dto.CrearPlanPersonalizadoRequest;
import com.nutrition.msnutritionplanner.application.planpersonalizado.dto.PlanPersonalizadoResponse;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.planbase.Dia;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBaseRepository;
import com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizado;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizadoRepository;
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
 * Unit test del caso de uso PlanAlimentarioPersonalizado: se mockean
 * unicamente los puertos de repositorio (PlanAlimentarioPersonalizadoRepository,
 * PlanAlimentarioBaseRepository, RecetaRepository). Sigue el patron de
 * IngredienteUseCasesTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Casos de uso: PlanAlimentarioPersonalizado")
class PlanAlimentarioPersonalizadoUseCasesTest {

    @Mock
    private PlanAlimentarioPersonalizadoRepository planPersonalizadoRepository;

    @Mock
    private PlanAlimentarioBaseRepository planBaseRepository;

    @Mock
    private RecetaRepository recetaRepository;

    private PlanAlimentarioPersonalizadoUseCases useCases;

    @BeforeEach
    void setUp() {
        useCases = new PlanAlimentarioPersonalizadoUseCases(
                planPersonalizadoRepository, planBaseRepository, recetaRepository);
    }

    private Receta receta(int id) {
        return Receta.reconstruir(id, "Receta " + id, "Instruccion", true, List.of(1));
    }

    private PlanAlimentarioBase planBaseCompleto() {
        Dia dia = Dia.reconstruir(10, Orden.of(1),
                List.of(TiempoDeComida.reconstruir(100, Hora.of(8, 0), List.of(1))));
        return PlanAlimentarioBase.reconstruir(7, EstadoPlan.COMPLETO, LocalDate.of(2026, 1, 1), true, List.of(dia));
    }

    /** Plan personalizado INICIADO con dos dias, para poder ejercitar quitarDia(). */
    private PlanAlimentarioPersonalizado planPersonalizadoConDosDias() {
        Dia dia1 = Dia.reconstruir(20, Orden.of(1),
                List.of(TiempoDeComida.reconstruir(200, Hora.of(8, 0), List.of(1))));
        Dia dia2 = Dia.reconstruir(21, Orden.of(2),
                List.of(TiempoDeComida.reconstruir(201, Hora.of(13, 0), List.of(1))));
        return PlanAlimentarioPersonalizado.reconstruir(1, 7, "Cambio de horarios", EstadoPlan.INICIADO,
                LocalDate.of(2026, 1, 1), true, List.of(dia1, dia2));
    }

    @Test
    @DisplayName("crear() genera una version personalizada a partir de un plan base COMPLETO existente")
    void creaPlanPersonalizado() {
        CrearPlanPersonalizadoRequest request = new CrearPlanPersonalizadoRequest(7, "Sin lactosa");
        when(planBaseRepository.buscarPorId(7)).thenReturn(Optional.of(planBaseCompleto()));
        when(planPersonalizadoRepository.guardar(any(PlanAlimentarioPersonalizado.class)))
                .thenAnswer(inv -> {
                    PlanAlimentarioPersonalizado arg = inv.getArgument(0);
                    return PlanAlimentarioPersonalizado.reconstruir(1, arg.getIdPlanBase(), arg.getDescripcionCambios(),
                            arg.getEstadoPlan(), arg.getFechaInicioVigencia(), arg.isActivo(), arg.getDias());
                });

        PlanPersonalizadoResponse response = useCases.crear(request);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.idPlanBase()).isEqualTo(7);
        assertThat(response.descripcionCambios()).isEqualTo("Sin lactosa");
        assertThat(response.estadoPlan()).isEqualTo(EstadoPlan.INICIADO);
    }

    @Test
    @DisplayName("crear() con un plan base inexistente lanza EntityNotFoundException")
    void fallaAlCrearConPlanBaseInexistente() {
        CrearPlanPersonalizadoRequest request = new CrearPlanPersonalizadoRequest(99, "Sin lactosa");
        when(planBaseRepository.buscarPorId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCases.crear(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
        verify(planPersonalizadoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("agregarDia() agrega un dia mas al plan personalizado existente")
    void agregaDia() {
        PlanAlimentarioPersonalizado plan = planPersonalizadoConDosDias();
        when(planPersonalizadoRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any(PlanAlimentarioPersonalizado.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanPersonalizadoResponse response = useCases.agregarDia(1);

        assertThat(response.dias()).hasSize(3);
    }

    @Test
    @DisplayName("agregarDia() con un plan personalizado inexistente lanza EntityNotFoundException")
    void fallaAlAgregarDiaPlanInexistente() {
        when(planPersonalizadoRepository.buscarPorId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCases.agregarDia(99))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("quitarDia() quita el dia indicado y reordena los restantes")
    void quitaDia() {
        PlanAlimentarioPersonalizado plan = planPersonalizadoConDosDias();
        when(planPersonalizadoRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any(PlanAlimentarioPersonalizado.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanPersonalizadoResponse response = useCases.quitarDia(1, 20);

        assertThat(response.dias()).hasSize(1);
        assertThat(response.dias().get(0).id()).isEqualTo(21);
        assertThat(response.dias().get(0).numOrden()).isEqualTo(1);
    }

    @Test
    @DisplayName("agregarTiempoDeComida() valida que las recetas existan y las asigna al dia")
    void agregaTiempoDeComida() {
        PlanAlimentarioPersonalizado plan = planPersonalizadoConDosDias();
        TiempoDeComidaRequest request = new TiempoDeComidaRequest(LocalTime.of(20, 0), List.of(1));
        when(recetaRepository.buscarPorIds(List.of(1))).thenReturn(List.of(receta(1)));
        when(planPersonalizadoRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any(PlanAlimentarioPersonalizado.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanPersonalizadoResponse response = useCases.agregarTiempoDeComida(1, 20, request);

        assertThat(response.dias().get(0).nroTiemposComida()).isEqualTo(2);
    }

    @Test
    @DisplayName("agregarTiempoDeComida() lanza DomainValidationException si alguna receta no existe")
    void fallaAlAgregarTiempoDeComidaConRecetaInexistente() {
        TiempoDeComidaRequest request = new TiempoDeComidaRequest(LocalTime.of(20, 0), List.of(99));
        when(recetaRepository.buscarPorIds(List.of(99))).thenReturn(List.of());

        assertThatThrownBy(() -> useCases.agregarTiempoDeComida(1, 20, request))
                .isInstanceOf(DomainValidationException.class);
        verify(planPersonalizadoRepository, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("quitarTiempoDeComida() quita el tiempo de comida indicado del dia")
    void quitaTiempoDeComida() {
        Dia dia = Dia.reconstruir(20, Orden.of(1), List.of(
                TiempoDeComida.reconstruir(200, Hora.of(8, 0), List.of(1)),
                TiempoDeComida.reconstruir(201, Hora.of(13, 0), List.of(1))));
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(1, 7, "Cambios",
                EstadoPlan.INICIADO, LocalDate.of(2026, 1, 1), true, List.of(dia));
        when(planPersonalizadoRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any(PlanAlimentarioPersonalizado.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanPersonalizadoResponse response = useCases.quitarTiempoDeComida(1, 20, 200);

        assertThat(response.dias().get(0).nroTiemposComida()).isEqualTo(1);
    }

    @Test
    @DisplayName("completar() marca el plan personalizado como COMPLETO")
    void completaPlan() {
        PlanAlimentarioPersonalizado plan = planPersonalizadoConDosDias();
        when(planPersonalizadoRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any(PlanAlimentarioPersonalizado.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanPersonalizadoResponse response = useCases.completar(1);

        assertThat(response.estadoPlan()).isEqualTo(EstadoPlan.COMPLETO);
    }

    @Test
    @DisplayName("descartar() marca el plan personalizado como DESCARTADO")
    void descartaPlan() {
        PlanAlimentarioPersonalizado plan = planPersonalizadoConDosDias();
        when(planPersonalizadoRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any(PlanAlimentarioPersonalizado.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanPersonalizadoResponse response = useCases.descartar(1);

        assertThat(response.estadoPlan()).isEqualTo(EstadoPlan.DESCARTADO);
    }

    @Test
    @DisplayName("eliminar() da de baja el plan personalizado y lo vuelve a guardar")
    void eliminaPlan() {
        PlanAlimentarioPersonalizado plan = planPersonalizadoConDosDias();
        when(planPersonalizadoRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any(PlanAlimentarioPersonalizado.class))).thenAnswer(inv -> inv.getArgument(0));

        useCases.eliminar(1);

        ArgumentCaptor<PlanAlimentarioPersonalizado> captor = ArgumentCaptor.forClass(PlanAlimentarioPersonalizado.class);
        verify(planPersonalizadoRepository).guardar(captor.capture());
        assertThat(captor.getValue().isActivo()).isFalse();
    }

    @Test
    @DisplayName("obtener() con un id inexistente lanza EntityNotFoundException")
    void fallaAlObtenerInexistente() {
        when(planPersonalizadoRepository.buscarPorId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCases.obtener(99))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("obtener() traduce el plan encontrado a PlanPersonalizadoResponse")
    void obtienePlan() {
        PlanAlimentarioPersonalizado plan = planPersonalizadoConDosDias();
        when(planPersonalizadoRepository.buscarPorId(1)).thenReturn(Optional.of(plan));

        PlanPersonalizadoResponse response = useCases.obtener(1);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.idPlanBase()).isEqualTo(7);
    }

    @Test
    @DisplayName("listarPorPlanBase() traduce cada version personalizada activa a PlanPersonalizadoResponse")
    void listaPlanesPersonalizadosDeUnPlanBase() {
        when(planPersonalizadoRepository.listarActivosPorPlanBase(7)).thenReturn(List.of(planPersonalizadoConDosDias()));

        List<PlanPersonalizadoResponse> respuesta = useCases.listarPorPlanBase(7);

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.get(0).idPlanBase()).isEqualTo(7);
    }
}
