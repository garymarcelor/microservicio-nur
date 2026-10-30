package com.nutrition.msnutritionplanner.application.planpersonalizado;

import com.nutrition.msnutritionplanner.application.planbase.dto.TiempoDeComidaRequest;
import com.nutrition.msnutritionplanner.application.planpersonalizado.dto.CrearPlanPersonalizadoRequest;
import com.nutrition.msnutritionplanner.application.planpersonalizado.dto.PlanPersonalizadoResponse;
import com.nutrition.msnutritionplanner.application.shared.event.IntegrationEventPublisher;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.planbase.Dia;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBaseRepository;
import com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizado;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizadoRepository;
import com.nutrition.msnutritionplanner.domain.receta.IngredienteCantidad;
import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.domain.receta.RecetaRepository;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;
import com.nutrition.msnutritionplanner.domain.shared.Hora;
import com.nutrition.msnutritionplanner.domain.shared.Orden;
import com.nutrition.msnutritionplanner.domain.shared.UnidadMedida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Casos de uso: PlanAlimentarioPersonalizado")
class PlanAlimentarioPersonalizadoUseCasesTest {

    @Mock
    private PlanAlimentarioPersonalizadoRepository planPersonalizadoRepository;

    @Mock
    private PlanAlimentarioBaseRepository planBaseRepository;

    @Mock
    private RecetaRepository recetaRepository;

    @Mock
    private IntegrationEventPublisher eventPublisher;

    private PlanAlimentarioPersonalizadoUseCases useCases;

    private static IngredienteCantidad ing(int id) {
        return new IngredienteCantidad(id, new BigDecimal("100"), UnidadMedida.GRAMOS);
    }

    @BeforeEach
    void setUp() {
        useCases = new PlanAlimentarioPersonalizadoUseCases(planPersonalizadoRepository, planBaseRepository, recetaRepository, eventPublisher);
    }

    @Test
    @DisplayName("crear() genera una version personalizada a partir de un plan base completo")
    void crearPlanPersonalizado() {
        Dia d1 = Dia.reconstruir(1, Orden.of(1), new ArrayList<>());
        PlanAlimentarioBase planBase = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>(List.of(d1)));
        planBase.completar();

        when(planBaseRepository.buscarPorId(1)).thenReturn(Optional.of(planBase));
        when(planPersonalizadoRepository.guardar(any(PlanAlimentarioPersonalizado.class)))
                .thenAnswer(inv -> {
                    PlanAlimentarioPersonalizado arg = inv.getArgument(0);
                    return PlanAlimentarioPersonalizado.reconstruir(10, 1, arg.getIdPaciente(), arg.getDescripcionCambios(), arg.getEstadoPlan(), arg.getFechaInicioVigencia(), true, Collections.emptyList());
                });

        CrearPlanPersonalizadoRequest request = new CrearPlanPersonalizadoRequest(1, "pac-1", "Cambio por alergia");
        PlanPersonalizadoResponse response = useCases.crear(request);

        assertThat(response.id()).isEqualTo(10);
        assertThat(response.idPlanBase()).isEqualTo(1);
        assertThat(response.descripcionCambios()).isEqualTo("Cambio por alergia");
        assertThat(response.idPaciente()).isEqualTo("pac-1");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("crear() lanza EntityNotFoundException si el plan base no existe")
    void crearPlanPersonalizadoPlanBaseInexistente() {
        when(planBaseRepository.buscarPorId(99)).thenReturn(Optional.empty());

        CrearPlanPersonalizadoRequest request = new CrearPlanPersonalizadoRequest(99, "pac-1", "Cambio");

        assertThatThrownBy(() -> useCases.crear(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("agregarDia() agrega un dia y lo guarda")
    void agregarDia() {
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "pac-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(planPersonalizadoRepository.buscarPorId(10)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        PlanPersonalizadoResponse response = useCases.agregarDia(10);

        assertThat(response.dias()).hasSize(1);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("quitarDia() remueve un dia y guarda el plan")
    void quitarDia() {
        Dia d1 = Dia.reconstruir(1, Orden.of(1), new ArrayList<>());
        Dia d2 = Dia.reconstruir(2, Orden.of(2), new ArrayList<>());
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "pac-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>(List.of(d1, d2)));

        when(planPersonalizadoRepository.buscarPorId(10)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        PlanPersonalizadoResponse response = useCases.quitarDia(10, 2);

        assertThat(response.dias()).hasSize(1);
    }

    @Test
    @DisplayName("agregarTiempoDeComida() valida recetas y agrega el tiempo")
    void agregarTiempoDeComida() {
        Dia d1 = Dia.reconstruir(1, Orden.of(1), new ArrayList<>());
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "pac-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>(List.of(d1)));

        Receta receta = Receta.reconstruir(5, "Receta 5", "Instrucciones", 200, true, List.of(ing(1)));
        when(recetaRepository.buscarPorIds(List.of(5))).thenReturn(List.of(receta));
        when(planPersonalizadoRepository.buscarPorId(10)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        TiempoDeComidaRequest request = new TiempoDeComidaRequest(LocalTime.of(12, 0), List.of(5));
        PlanPersonalizadoResponse response = useCases.agregarTiempoDeComida(10, 1, request);

        assertThat(response.dias().get(0).tiemposDeComida()).hasSize(1);
    }

    @Test
    @DisplayName("quitarTiempoDeComida() remueve un tiempo de comida")
    void quitarTiempoDeComida() {
        TiempoDeComida t1 = TiempoDeComida.reconstruir(10, Hora.of(LocalTime.of(8, 0)), List.of(1));
        TiempoDeComida t2 = TiempoDeComida.reconstruir(20, Hora.of(LocalTime.of(12, 0)), List.of(2));
        Dia d1 = Dia.reconstruir(1, Orden.of(1), new ArrayList<>(List.of(t1, t2)));
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "pac-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>(List.of(d1)));

        when(planPersonalizadoRepository.buscarPorId(10)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        PlanPersonalizadoResponse response = useCases.quitarTiempoDeComida(10, 1, 20);

        assertThat(response.dias().get(0).tiemposDeComida()).hasSize(1);
    }

    @Test
    @DisplayName("completar() cambia el estado a COMPLETO")
    void completar() {
        Dia d1 = Dia.reconstruir(1, Orden.of(1), new ArrayList<>());
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "pac-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>(List.of(d1)));

        when(planPersonalizadoRepository.buscarPorId(10)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        PlanPersonalizadoResponse response = useCases.completar(10);

        assertThat(response.estadoPlan()).isEqualTo(EstadoPlan.COMPLETO);
        verify(eventPublisher).planPersonalizadoCreado(any(PlanAlimentarioPersonalizado.class));
    }

    @Test
    @DisplayName("descartar() cambia el estado a DESCARTADO")
    void descartar() {
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "pac-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(planPersonalizadoRepository.buscarPorId(10)).thenReturn(Optional.of(plan));
        when(planPersonalizadoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        PlanPersonalizadoResponse response = useCases.descartar(10);

        assertThat(response.estadoPlan()).isEqualTo(EstadoPlan.DESCARTADO);
    }

    @Test
    @DisplayName("eliminar() marca el plan personalizado como inactivo")
    void eliminar() {
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "pac-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(planPersonalizadoRepository.buscarPorId(10)).thenReturn(Optional.of(plan));

        useCases.eliminar(10);

        ArgumentCaptor<PlanAlimentarioPersonalizado> captor = ArgumentCaptor.forClass(PlanAlimentarioPersonalizado.class);
        verify(planPersonalizadoRepository).guardar(captor.capture());
        assertThat(captor.getValue().isActivo()).isFalse();
    }

    @Test
    @DisplayName("obtener() retorna el plan si existe")
    void obtener() {
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "pac-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(planPersonalizadoRepository.buscarPorId(10)).thenReturn(Optional.of(plan));

        PlanPersonalizadoResponse response = useCases.obtener(10);

        assertThat(response.id()).isEqualTo(10);
    }

    @Test
    @DisplayName("listarPorPlanBase() retorna lista de respuestas para ese plan base")
    void listarPorPlanBase() {
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "pac-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(planPersonalizadoRepository.listarActivosPorPlanBase(1)).thenReturn(List.of(plan));

        List<PlanPersonalizadoResponse> list = useCases.listarPorPlanBase(1);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).idPlanBase()).isEqualTo(1);
    }

    @Test
    @DisplayName("desactivarPorSuscripcionCancelada() retorna false si el plan no existe")
    void desactivarPlanInexistente() {
        when(planPersonalizadoRepository.buscarPorId(99)).thenReturn(Optional.empty());

        boolean resultado = useCases.desactivarPorSuscripcionCancelada(99, "pac-1");

        assertThat(resultado).isFalse();
        verify(planPersonalizadoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("desactivarPorSuscripcionCancelada() retorna false si el plan ya esta inactivo")
    void desactivarPlanYaInactivo() {
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "pac-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), false, Collections.emptyList());
        when(planPersonalizadoRepository.buscarPorId(10)).thenReturn(Optional.of(plan));

        boolean resultado = useCases.desactivarPorSuscripcionCancelada(10, "pac-1");

        assertThat(resultado).isFalse();
        verify(planPersonalizadoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("desactivarPorSuscripcionCancelada() da de baja y guarda el plan activo")
    void desactivarPlanActivo() {
        PlanAlimentarioPersonalizado plan = PlanAlimentarioPersonalizado.reconstruir(10, 1, "pac-1", "Cambio", EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(planPersonalizadoRepository.buscarPorId(10)).thenReturn(Optional.of(plan));

        boolean resultado = useCases.desactivarPorSuscripcionCancelada(10, "pac-1");

        assertThat(resultado).isTrue();
        ArgumentCaptor<PlanAlimentarioPersonalizado> captor = ArgumentCaptor.forClass(PlanAlimentarioPersonalizado.class);
        verify(planPersonalizadoRepository).guardar(captor.capture());
        assertThat(captor.getValue().isActivo()).isFalse();
    }
}
