package com.nutrition.msnutritionplanner.infrastructure.messaging.in;

import com.nutrition.msnutritionplanner.application.planpersonalizado.PlanAlimentarioPersonalizadoUseCases;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.ProcesadorEventosEntrada.Resultado;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.AntecedenteClinicoRefRepository;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.EvaluacionRefRepository;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.InboxEventRepository;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.NutricionistaRefRepository;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.PacienteRefJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel.PacienteRefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del consumo de eventos con los JSON EXACTOS del documento
 * TareaActividad2-tabla-corregida.md: idempotencia (inbox), filtrado,
 * efectos en los modelos de lectura y desactivacion del plan al cancelarse la suscripcion.
 */
@ExtendWith(MockitoExtension.class)
class ProcesadorEventosEntradaTest {

    @Mock
    private InboxEventRepository inbox;
    @Mock
    private PacienteRefRepository pacientes;
    @Mock
    private NutricionistaRefRepository nutricionistas;
    @Mock
    private AntecedenteClinicoRefRepository antecedentes;
    @Mock
    private EvaluacionRefRepository evaluaciones;
    @Mock
    private PlanAlimentarioPersonalizadoUseCases planesPersonalizados;

    private ProcesadorEventosEntrada procesador;

    private static final String PACIENTE_REGISTRADO = """
            {"eventId":"3f2a0001-9c1e-4b7a-a1d2-7e5f0c8b1001","eventName":"bc1.PacienteRegistrado",
             "eventVersion":"1.0","occurredOn":"2026-10-15T13:00:00Z","source":"MsPatientsConsulCore",
             "correlationId":"corr-alta-PAC-1024",
             "data":{"idPaciente":"PAC-1024","nombreCompleto":"Juan Pérez Rojas","fechaNacimiento":"1985-04-12",
                     "sexo":"M","telefono":"+591 70012345","email":"jperez@correo.bo",
                     "idNutricionista":"NUT-12","fechaRegistro":"2026-10-15","campoNuevoDesconocido":"x"}}
            """;

    private static final String SUSCRIPCION_CANCELADA = """
            {"eventId":"3f2a0013-9c1e-4b7a-a1d2-7e5f0c8b1013","eventName":"bc3.SuscripcionCancelada",
             "eventVersion":"1.0","occurredOn":"2026-11-23T03:59:59Z","source":"MsCateringSubscription",
             "correlationId":"corr-baja-SUS-501",
             "data":{"idSuscripcion":"SUS-501","idPaciente":"PAC-1024","idPlan":"PLAN-205",
                     "tipoBaja":"VENCIMIENTO","motivo":"Finalización","fechaEfectiva":"2026-11-22",
                     "entregasPendientesAnuladas":0}}
            """;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"), ZoneOffset.UTC);
        procesador = new ProcesadorEventosEntrada(JsonMapper.builder().build(), reloj, inbox, pacientes,
                nutricionistas, antecedentes, evaluaciones, planesPersonalizados);
    }

    @Test
    @DisplayName("PacienteRegistrado: guarda la copia local, registra el inbox e ignora campos desconocidos")
    void pacienteRegistrado() {
        when(inbox.existsById("3f2a0001-9c1e-4b7a-a1d2-7e5f0c8b1001")).thenReturn(false);
        when(pacientes.findById("PAC-1024")).thenReturn(Optional.empty());

        Resultado resultado = procesador.procesar(PACIENTE_REGISTRADO);

        assertThat(resultado).isEqualTo(Resultado.PROCESADO);
        ArgumentCaptor<PacienteRefJpaEntity> captor = ArgumentCaptor.forClass(PacienteRefJpaEntity.class);
        verify(pacientes).save(captor.capture());
        PacienteRefJpaEntity p = captor.getValue();
        assertThat(p.getIdPaciente()).isEqualTo("PAC-1024");
        assertThat(p.getNombreCompleto()).isEqualTo("Juan Pérez Rojas");
        assertThat(p.getFechaNacimiento()).hasToString("1985-04-12");
        assertThat(p.getIdNutricionista()).isEqualTo("NUT-12");
        verify(inbox).save(any());
    }

    @Test
    @DisplayName("Un eventId ya procesado se descarta sin ejecutar ningun efecto (idempotencia)")
    void duplicado() {
        when(inbox.existsById("3f2a0001-9c1e-4b7a-a1d2-7e5f0c8b1001")).thenReturn(true);

        assertThat(procesador.procesar(PACIENTE_REGISTRADO)).isEqualTo(Resultado.DUPLICADO);

        verify(pacientes, never()).save(any());
        verify(inbox, never()).save(any());
    }

    @Test
    @DisplayName("SuscripcionCancelada: desactiva el plan 205 (PLAN-205) del paciente")
    void suscripcionCancelada() {
        when(inbox.existsById(anyString())).thenReturn(false);
        when(planesPersonalizados.desactivarPorSuscripcionCancelada(205, "PAC-1024")).thenReturn(true);

        assertThat(procesador.procesar(SUSCRIPCION_CANCELADA)).isEqualTo(Resultado.PROCESADO);

        verify(planesPersonalizados).desactivarPorSuscripcionCancelada(205, "PAC-1024");
        verify(inbox).save(any());
    }

    @Test
    @DisplayName("Un evento de otro tipo se ignora y NO se registra en el inbox")
    void eventoNoSoportado() {
        when(inbox.existsById(anyString())).thenReturn(false);
        String json = """
                {"eventId":"e-1","eventName":"bc3.SuscripcionPausada","eventVersion":"1.0",
                 "occurredOn":"2026-10-25T14:00:00Z","source":"MsCateringSubscription",
                 "correlationId":"c","data":{"idSuscripcion":"SUS-501"}}
                """;

        assertThat(procesador.procesar(json)).isEqualTo(Resultado.IGNORADO);

        verify(inbox, never()).save(any());
    }

    @Test
    @DisplayName("JSON mal formado o sin campos obligatorios lanza IllegalArgumentException")
    void invalidos() {
        assertThatThrownBy(() -> procesador.procesar("esto no es json"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> procesador.procesar("{\"eventName\":\"bc1.PacienteRegistrado\"}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("idNumerico() convierte ids con prefijo del contrato a enteros")
    void idNumerico() {
        assertThat(ProcesadorEventosEntrada.idNumerico("PLAN-205")).isEqualTo(205);
        assertThatThrownBy(() -> ProcesadorEventosEntrada.idNumerico("PLAN-abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
