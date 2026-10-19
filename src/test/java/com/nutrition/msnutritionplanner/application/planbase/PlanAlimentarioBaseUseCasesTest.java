package com.nutrition.msnutritionplanner.application.planbase;

import com.nutrition.msnutritionplanner.application.planbase.dto.CrearPlanBaseRequest;
import com.nutrition.msnutritionplanner.application.planbase.dto.PlanBaseResponse;
import com.nutrition.msnutritionplanner.application.planbase.dto.TiempoDeComidaRequest;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.planbase.Dia;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBaseRepository;
import com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Test
    @DisplayName("crear() construye y guarda un nuevo PlanAlimentarioBase")
    void crearPlan() {
        LocalDate inicio = LocalDate.now().plusDays(1);
        CrearPlanBaseRequest request = new CrearPlanBaseRequest(inicio);
        when(planRepository.guardar(any(PlanAlimentarioBase.class)))
                .thenAnswer(inv -> {
                    PlanAlimentarioBase arg = inv.getArgument(0);
                    return PlanAlimentarioBase.reconstruir(1, arg.getEstadoPlan(), arg.getFechaInicioVigencia(), true, Collections.emptyList());
                });

        PlanBaseResponse response = useCases.crear(request);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.fechaInicioVigencia()).isEqualTo(inicio);
        verify(planRepository).guardar(any(PlanAlimentarioBase.class));
    }

    @Test
    @DisplayName("agregarDia() agrega un dia al plan y lo guarda")
    void agregarDia() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanBaseResponse response = useCases.agregarDia(1);

        assertThat(response.dias()).hasSize(1);
        verify(planRepository).guardar(plan);
    }

    @Test
    @DisplayName("quitarDia() remueve el dia especificado")
    void quitarDia() {
        Dia d1 = Dia.reconstruir(1, Orden.of(1), new ArrayList<>());
        Dia d2 = Dia.reconstruir(2, Orden.of(2), new ArrayList<>());
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>(List.of(d1, d2)));

        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanBaseResponse response = useCases.quitarDia(1, 2);

        assertThat(response.dias()).hasSize(1);
        verify(planRepository).guardar(plan);
    }

    @Test
    @DisplayName("agregarTiempoDeComida() valida recetas y agrega el tiempo de comida")
    void agregarTiempoDeComida() {
        Dia d1 = Dia.reconstruir(1, Orden.of(1), new ArrayList<>());
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>(List.of(d1)));

        Receta recetaMock = Receta.reconstruir(10, "Ensalada", "Mezclar", true, List.of(1));
        when(recetaRepository.buscarPorIds(List.of(10))).thenReturn(List.of(recetaMock));
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        TiempoDeComidaRequest request = new TiempoDeComidaRequest(LocalTime.of(8, 0), List.of(10));
        PlanBaseResponse response = useCases.agregarTiempoDeComida(1, 1, request);

        assertThat(response.dias().get(0).tiemposDeComida()).hasSize(1);
        verify(planRepository).guardar(plan);
    }

    @Test
    @DisplayName("agregarTiempoDeComida() lanza DomainValidationException si alguna receta no existe")
    void agregarTiempoDeComidaRecetaInexistente() {
        when(recetaRepository.buscarPorIds(List.of(99))).thenReturn(Collections.emptyList());

        TiempoDeComidaRequest request = new TiempoDeComidaRequest(LocalTime.of(8, 0), List.of(99));

        assertThatThrownBy(() -> useCases.agregarTiempoDeComida(1, 1, request))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("no existen");
    }

    @Test
    @DisplayName("quitarTiempoDeComida() remueve el tiempo de comida del dia")
    void quitarTiempoDeComida() {
        TiempoDeComida t1 = TiempoDeComida.reconstruir(10, Hora.of(LocalTime.of(8, 0)), List.of(1));
        TiempoDeComida t2 = TiempoDeComida.reconstruir(20, Hora.of(LocalTime.of(12, 0)), List.of(2));
        Dia d1 = Dia.reconstruir(1, Orden.of(1), new ArrayList<>(List.of(t1, t2)));
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>(List.of(d1)));

        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanBaseResponse response = useCases.quitarTiempoDeComida(1, 1, 20);

        assertThat(response.dias().get(0).tiemposDeComida()).hasSize(1);
    }

    @Test
    @DisplayName("completar() cambia estado a COMPLETO")
    void completarPlan() {
        Dia d1 = Dia.reconstruir(1, Orden.of(1), new ArrayList<>());
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, new ArrayList<>(List.of(d1)));

        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanBaseResponse response = useCases.completar(1);

        assertThat(response.estadoPlan()).isEqualTo(EstadoPlan.COMPLETO);
    }

    @Test
    @DisplayName("descartar() cambia estado a DESCARTADO")
    void descartarPlan() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));
        when(planRepository.guardar(any(PlanAlimentarioBase.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanBaseResponse response = useCases.descartar(1);

        assertThat(response.estadoPlan()).isEqualTo(EstadoPlan.DESCARTADO);
    }

    @Test
    @DisplayName("eliminar() marca el plan como inactivo")
    void eliminarPlan() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));

        useCases.eliminar(1);

        ArgumentCaptor<PlanAlimentarioBase> captor = ArgumentCaptor.forClass(PlanAlimentarioBase.class);
        verify(planRepository).guardar(captor.capture());
        assertThat(captor.getValue().isActivo()).isFalse();
    }

    @Test
    @DisplayName("obtener() retorna el plan si existe")
    void obtenerPlan() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(planRepository.buscarPorId(1)).thenReturn(Optional.of(plan));

        PlanBaseResponse response = useCases.obtener(1);

        assertThat(response.id()).isEqualTo(1);
    }

    @Test
    @DisplayName("obtener() lanza EntityNotFoundException si no existe")
    void obtenerPlanInexistente() {
        when(planRepository.buscarPorId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCases.obtener(99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("listar() retorna todos los planes activos")
    void listarPlanes() {
        PlanAlimentarioBase plan = PlanAlimentarioBase.reconstruir(1, EstadoPlan.INICIADO, LocalDate.now(), true, Collections.emptyList());
        when(planRepository.listarActivos()).thenReturn(List.of(plan));

        List<PlanBaseResponse> result = useCases.listar();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1);
    }
}
