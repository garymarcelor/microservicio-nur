# Módulo 5 – Actividad 2 · Tabla de eventos corregida (propuesta)

## 1. Introducción (texto corregido)

El presente documento identifica los eventos y mensajes de integración del sistema de Nur-tricenter,
compuesto por cinco microservicios de negocio: MsPatientsConsulCore (pacientes, consultas y
evaluaciones), MSNutritionPlanner (planes nutricionales, recetas e ingredientes),
MsCateringSubscription (suscripciones y calendario de entregas), MsProductionPackaging (órdenes de
producción y empaquetado) y MsLogisticDelivery (rutas, geolocalización y seguimiento de entregas).
Se suma un servicio de soporte, **MsNotificaciones**, responsable de los avisos al paciente; así
ningún microservicio de negocio asume la comunicación con el paciente.

Los eventos de integración representan hechos ya ocurridos en un microservicio que son de interés
para otro(s) microservicio(s). Cada contrato utiliza un sobre (envelope) común con los campos
`eventId`, `eventName`, `eventVersion`, `occurredOn` (ISO-8601, UTC), `source` y `correlationId`, y
un objeto `data` con la carga útil propia del evento.

**Convenciones:**

- **Nombre:** `bcN.SustantivoEnParticipio` (`bc3.SuscripcionCreada`, `bc5.EntregaConfirmada`).
- **Identificadores:** siempre *string* con prefijo (`PAC-1024`, `SUS-501`). Cada servicio usa su
  propio tipo interno (UUID, entero), pero en el contrato viaja como texto.
- **correlationId:** identifica un flujo de negocio y **se propaga** sin cambios por todos los
  eventos del flujo. Por ejemplo, el reparto del 23/10 usa `corr-entrega-2026-10-23` desde el
  cierre del calendario hasta la confirmación de la entrega.
- **Regla de cierre del calendario:** producción (BC4) y logística (BC5) trabajan solo con lo que
  trae `bc3.CalendarioConsolidadoCerrado`. Las pausas, cambios de dirección y cancelaciones se
  solicitan antes del cierre y se reflejan en el siguiente consolidado; un calendario ya cerrado no
  se modifica. Por eso BC4 y BC5 no consumen los eventos del ciclo de vida de la suscripción.

**Flujo principal:**

```
BC1 paciente/dirección/antecedentes ──► BC2 plan ──► BC3 suscripción + calendario
                                                          │ CalendarioConsolidadoCerrado
                                                          ▼
              BC3 ◄── EntregaConfirmada / NoConcretada ── BC5 ◄── PaqueteListo / OrdenCompletada ── BC4
                                                          │
                                                          └──► MsNotificaciones (avisos al paciente)
```

## 2. Resumen de la tabla

| N° | Evento | Productor | Consumidores | Cambio |
| :-: | :-- | :-- | :-- | :-- |
| 1 | `bc1.PacienteRegistrado` | MsPatientsConsulCore | MSNutritionPlanner, MsCateringSubscription | **Nuevo** |
| 2 | `bc1.AntecedenteClinicoRegistrado` | MsPatientsConsulCore | MSNutritionPlanner | Renombrado (antes `nuevoAntecedenteClinico`) |
| 3 | `bc1.NutricionistaRegistrado` | MsPatientsConsulCore | MSNutritionPlanner | Renombrado (antes `nuevoNutricionista`) |
| 4 | `bc1.DireccionClienteRegistrada` | MsPatientsConsulCore | MsCateringSubscription | Renombrado; consumidor pasa de Logística a Catering |
| 5 | `bc1.EvaluacionRealizada` | MsPatientsConsulCore | MSNutritionPlanner, MsCateringSubscription | **Nuevo** |
| 6 | `bc1.ProximaEvaluacionProgramada` | MsPatientsConsulCore | MSNutritionPlanner, MsCateringSubscription | Renombrado (antes `proximaEvaluacionPeriodica`) |
| 7 | `bc2.PlanBaseCreado` | MSNutritionPlanner | MsCateringSubscription | Género corregido; recetas con nombre y porción |
| 8 | `bc2.PlanPersonalizadoCreado` | MSNutritionPlanner | MsCateringSubscription | Género corregido; recetas con nombre y porción |
| 9 | `bc2.RecetaCreada` | MSNutritionPlanner | MsProductionPackaging | Renombrado; ingredientes con cantidad |
| 10 | `bc3.SuscripcionCreada` | MsCateringSubscription | MsPatientsConsulCore | Consumidores corregidos (sin "Auditoría" ni BC4) |
| 11 | `bc3.SuscripcionPausada` | MsCateringSubscription | MsPatientsConsulCore, MsNotificaciones | Consumidores corregidos |
| 12 | `bc3.SuscripcionReactivada` | MsCateringSubscription | MsPatientsConsulCore, MsNotificaciones | **Nuevo** |
| 13 | `bc3.SuscripcionCancelada` | MsCateringSubscription | MsPatientsConsulCore, MSNutritionPlanner, MsNotificaciones | Consumidores corregidos |
| 14 | `bc3.CalendarioConsolidadoCerrado` | MsCateringSubscription | MsProductionPackaging | Renombrado; trae paciente, dirección y recetas |
| 15 | `bc4.PaqueteListo` | MsProductionPackaging | MsLogisticDelivery | Agrega `idCalendarioDia` y la dirección completa |
| 16 | `bc4.OrdenCompletada` | MsProductionPackaging | MsLogisticDelivery | Totales coherentes con la lista |
| 17 | `bc5.EntregaProgramada` | MsLogisticDelivery | MsNotificaciones | Consumidor corregido |
| 18 | `bc5.RutaIniciada` | MsLogisticDelivery | MsNotificaciones | Consumidor corregido |
| 19 | `bc5.RepartidorLlegoAParada` | MsLogisticDelivery | MsNotificaciones | Consumidor corregido |
| 20 | `bc5.EntregaConfirmada` | MsLogisticDelivery | MsCateringSubscription, MsNotificaciones | **Nuevo** |
| 21 | `bc5.EntregaFallida` | MsLogisticDelivery | MsNotificaciones | Consumidor corregido |
| 22 | `bc5.EntregaReprogramada` | MsLogisticDelivery | MsNotificaciones | Consumidor corregido |
| 23 | `bc5.EntregaNoConcretada` | MsLogisticDelivery | MsCateringSubscription, MsNotificaciones | Se mantiene; agrega `idCalendarioDia` |

**Eliminados de la versión anterior:**

- `bc2.NuevoIngredienteCreada`. Ningún microservicio gestiona compras ni insumos, y BC4 recibe los
  ingredientes con su cantidad dentro de `bc2.RecetaCreada`. Queda como evento interno de BC2.
- `bc3.PreferenciasEntregaActualizadas`. Por la regla de cierre, la dirección de cada fecha ya
  viaja resuelta en `bc3.CalendarioConsolidadoCerrado`. Queda como evento interno de BC3.

## 3. Listado de eventos de integración

### 1 · bc1.PacienteRegistrado

- **Descripción:** alta de un nuevo paciente. BC2 lo necesita para crearle planes y Catering para
  registrar suscripciones a su nombre.
- **Productor:** MsPatientsConsulCore
- **Consumidores:** MSNutritionPlanner, MsCateringSubscription

```json
{
  "eventId": "3f2a0001-9c1e-4b7a-a1d2-7e5f0c8b1001",
  "eventName": "bc1.PacienteRegistrado",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-15T13:00:00Z",
  "source": "MsPatientsConsulCore",
  "correlationId": "corr-alta-PAC-1024",
  "data": {
    "idPaciente": "PAC-1024",
    "nombreCompleto": "Juan Pérez Rojas",
    "fechaNacimiento": "1985-04-12",
    "sexo": "M",
    "telefono": "+591 70012345",
    "email": "jperez@correo.bo",
    "idNutricionista": "NUT-12",
    "fechaRegistro": "2026-10-15"
  }
}
```

### 2 · bc1.AntecedenteClinicoRegistrado

- **Descripción:** se registró un nuevo antecedente alimenticio del paciente, que debe tomarse en
  cuenta al crear o ajustar sus planes.
- **Productor:** MsPatientsConsulCore
- **Consumidores:** MSNutritionPlanner

```json
{
  "eventId": "3f2a0002-9c1e-4b7a-a1d2-7e5f0c8b1002",
  "eventName": "bc1.AntecedenteClinicoRegistrado",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-16T14:30:00Z",
  "source": "MsPatientsConsulCore",
  "correlationId": "corr-alta-PAC-1024",
  "data": {
    "idPaciente": "PAC-1024",
    "idAntecedente": "ANT-315",
    "tipo": "ALERGIA_ALIMENTARIA",
    "descripcion": "Alergia al maní y frutos secos",
    "severidad": "ALTA",
    "ingredientesRestringidos": ["Maní", "Nuez"],
    "idNutricionista": "NUT-12",
    "fechaRegistro": "2026-10-16"
  }
}
```

### 3 · bc1.NutricionistaRegistrado

- **Descripción:** nuevo especialista en nutrición habilitado para crear planes.
- **Productor:** MsPatientsConsulCore
- **Consumidores:** MSNutritionPlanner

```json
{
  "eventId": "3f2a0003-9c1e-4b7a-a1d2-7e5f0c8b1003",
  "eventName": "bc1.NutricionistaRegistrado",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-01T12:00:00Z",
  "source": "MsPatientsConsulCore",
  "correlationId": "corr-alta-NUT-27",
  "data": {
    "idNutricionista": "NUT-27",
    "nombreCompleto": "Lic. María Fernanda Suárez",
    "matriculaProfesional": "NUT-SCZ-0457",
    "especialidad": "Nutrición clínica",
    "email": "msuarez@nurtricenter.bo",
    "activo": true,
    "fechaAlta": "2026-10-01"
  }
}
```

### 4 · bc1.DireccionClienteRegistrada

- **Descripción:** el paciente registró una dirección de entrega. Catering la asocia a los días del
  calendario y la envía resuelta en el consolidado, así producción y logística no dependen de BC1.
- **Productor:** MsPatientsConsulCore
- **Consumidores:** MsCateringSubscription

```json
{
  "eventId": "3f2a0004-9c1e-4b7a-a1d2-7e5f0c8b1004",
  "eventName": "bc1.DireccionClienteRegistrada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-15T13:05:00Z",
  "source": "MsPatientsConsulCore",
  "correlationId": "corr-alta-PAC-1024",
  "data": {
    "idPaciente": "PAC-1024",
    "idDireccion": "DIR-58",
    "alias": "Trabajo",
    "direccion": "Av. Cristo Redentor #1450, Edif. Torre Norte, piso 3",
    "referencia": "Frente al Banco Unión",
    "ciudad": "Santa Cruz de la Sierra",
    "geolocalizacion": { "latitud": -17.7612, "longitud": -63.1825 },
    "esPrincipal": true
  }
}
```

### 5 · bc1.EvaluacionRealizada

- **Descripción:** se completó una evaluación del paciente.
  - BC2 ajusta el plan si `requiereAjustePlan` es `true`.
  - Catering descuenta una de las evaluaciones incluidas en la suscripción.
- **Productor:** MsPatientsConsulCore
- **Consumidores:** MSNutritionPlanner, MsCateringSubscription

```json
{
  "eventId": "3f2a0005-9c1e-4b7a-a1d2-7e5f0c8b1005",
  "eventName": "bc1.EvaluacionRealizada",
  "eventVersion": "1.0",
  "occurredOn": "2026-11-04T14:00:00Z",
  "source": "MsPatientsConsulCore",
  "correlationId": "corr-eval-EVA-912",
  "data": {
    "idEvaluacion": "EVA-912",
    "idPaciente": "PAC-1024",
    "idSuscripcion": "SUS-501",
    "idNutricionista": "NUT-12",
    "tipoEvaluacion": "CONTROL_QUINCENAL",
    "fechaEvaluacion": "2026-11-04",
    "pesoKg": 82.4,
    "imc": 27.1,
    "observaciones": "Buena adherencia; reducir carbohidratos en la cena",
    "requiereAjustePlan": true
  }
}
```

### 6 · bc1.ProximaEvaluacionProgramada

- **Descripción:** fecha de la próxima evaluación periódica del paciente. BC2 la usa para preparar
  propuestas y Catering para avisar o reprogramar.
- **Productor:** MsPatientsConsulCore
- **Consumidores:** MSNutritionPlanner, MsCateringSubscription

```json
{
  "eventId": "3f2a0006-9c1e-4b7a-a1d2-7e5f0c8b1006",
  "eventName": "bc1.ProximaEvaluacionProgramada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-20T14:30:00Z",
  "source": "MsPatientsConsulCore",
  "correlationId": "corr-eval-EVA-877",
  "data": {
    "idPaciente": "PAC-1024",
    "idEvaluacionAnterior": "EVA-877",
    "idNutricionista": "NUT-12",
    "fechaProximaEvaluacion": "2026-11-04",
    "tipoEvaluacion": "CONTROL_QUINCENAL",
    "requiereAnalisisClinico": false
  }
}
```

### 7 · bc2.PlanBaseCreado

- **Descripción:** nuevo plan base disponible. Catering lo usa para ofrecerlo en las suscripciones y
  para armar el calendario.
- **Productor:** MSNutritionPlanner
- **Consumidores:** MsCateringSubscription

Respecto a `PlanBaseResponse` del OpenAPI, cada tiempo de comida lleva `recetas` (id, nombre y
porción) en lugar de solo `recetasIds`, para que Catering arme el calendario sin consultar a BC2.

```json
{
  "eventId": "3f2a0007-9c1e-4b7a-a1d2-7e5f0c8b1007",
  "eventName": "bc2.PlanBaseCreado",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-10T16:00:00Z",
  "source": "MSNutritionPlanner",
  "correlationId": "corr-catalogo-PLAN-41",
  "data": {
    "id": "PLAN-41",
    "estadoPlan": "COMPLETO",
    "fechaInicioVigencia": "2026-10-22",
    "duracionDias": 15,
    "activo": true,
    "dias": [
      {
        "id": "DIA-2001",
        "numOrden": 1,
        "nroTiemposComida": 3,
        "tiemposDeComida": [
          {
            "id": "TC-8001",
            "hora": "08:00",
            "recetas": [
              { "idReceta": "REC-14", "nombre": "Avena cocida con frutos rojos", "porcionGramos": 250 }
            ]
          },
          {
            "id": "TC-8002",
            "hora": "13:00",
            "recetas": [
              { "idReceta": "REC-31", "nombre": "Sopa de verduras", "porcionGramos": 300 },
              { "idReceta": "REC-33", "nombre": "Ensalada fresca", "porcionGramos": 200 }
            ]
          },
          {
            "id": "TC-8003",
            "hora": "19:30",
            "recetas": [
              { "idReceta": "REC-45", "nombre": "Pechuga a la plancha con quinua y vegetales", "porcionGramos": 350 }
            ]
          }
        ]
      }
    ]
  }
}
```

### 8 · bc2.PlanPersonalizadoCreado

- **Descripción:** se creó el plan personalizado de un paciente a partir de un plan base. Catering lo
  asigna a la suscripción y arma el calendario de entregas.
- **Productor:** MSNutritionPlanner
- **Consumidores:** MsCateringSubscription

```json
{
  "eventId": "3f2a0008-9c1e-4b7a-a1d2-7e5f0c8b1008",
  "eventName": "bc2.PlanPersonalizadoCreado",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-19T15:00:00Z",
  "source": "MSNutritionPlanner",
  "correlationId": "corr-alta-PAC-1024",
  "data": {
    "id": "PLAN-205",
    "idPlanBase": "PLAN-41",
    "idPaciente": "PAC-1024",
    "descripcionCambios": "Se reemplaza maní por semillas de girasol por alergia registrada",
    "estadoPlan": "COMPLETO",
    "fechaInicioVigencia": "2026-10-22",
    "duracionDias": 15,
    "activo": true,
    "dias": [
      {
        "id": "DIA-3001",
        "numOrden": 1,
        "nroTiemposComida": 3,
        "tiemposDeComida": [
          {
            "id": "TC-9001",
            "hora": "08:00",
            "recetas": [
              { "idReceta": "REC-14", "nombre": "Avena cocida con frutos rojos", "porcionGramos": 250 },
              { "idReceta": "REC-22", "nombre": "Yogur natural con semillas de girasol", "porcionGramos": 150 }
            ]
          },
          {
            "id": "TC-9002",
            "hora": "13:00",
            "recetas": [
              { "idReceta": "REC-31", "nombre": "Sopa de verduras", "porcionGramos": 300 }
            ]
          },
          {
            "id": "TC-9003",
            "hora": "19:30",
            "recetas": [
              { "idReceta": "REC-45", "nombre": "Pechuga a la plancha con quinua y vegetales", "porcionGramos": 350 }
            ]
          }
        ]
      }
    ]
  }
}
```

### 9 · bc2.RecetaCreada

- **Descripción:** nueva receta en el catálogo. Cocina (BC4) la usa para la preparación: las
  instrucciones y la cantidad de cada ingrediente por porción.
- **Productor:** MSNutritionPlanner
- **Consumidores:** MsProductionPackaging

```json
{
  "eventId": "3f2a0009-9c1e-4b7a-a1d2-7e5f0c8b1009",
  "eventName": "bc2.RecetaCreada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-05T14:00:00Z",
  "source": "MSNutritionPlanner",
  "correlationId": "corr-catalogo-REC-45",
  "data": {
    "id": "REC-45",
    "nombre": "Pechuga a la plancha con quinua y vegetales",
    "instruccion": "Sellar la pechuga 6 min por lado; cocer la quinua 15 min; saltear vegetales.",
    "porcionGramos": 350,
    "activo": true,
    "ingredientes": [
      { "idIngrediente": "ING-12", "nombre": "Pechuga de pollo", "cantidad": 180, "unidad": "GRAMOS" },
      { "idIngrediente": "ING-88", "nombre": "Quinua real", "cantidad": 80, "unidad": "GRAMOS" },
      { "idIngrediente": "ING-103", "nombre": "Brócoli", "cantidad": 80, "unidad": "GRAMOS" },
      { "idIngrediente": "ING-7", "nombre": "Aceite de oliva", "cantidad": 10, "unidad": "MILILITROS" }
    ]
  }
}
```

### 10 · bc3.SuscripcionCreada

- **Descripción:** alta de un paciente en un plan. BC1 agenda las evaluaciones incluidas en la
  suscripción.
- **Productor:** MsCateringSubscription
- **Consumidores:** MsPatientsConsulCore

```json
{
  "eventId": "3f2a0010-9c1e-4b7a-a1d2-7e5f0c8b1010",
  "eventName": "bc3.SuscripcionCreada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-20T16:00:00Z",
  "source": "MsCateringSubscription",
  "correlationId": "corr-alta-PAC-1024",
  "data": {
    "idSuscripcion": "SUS-501",
    "idPaciente": "PAC-1024",
    "idPlan": "PLAN-205",
    "tipoPlan": "PERSONALIZADO",
    "duracionDias": 30,
    "fechaInicio": "2026-10-22",
    "fechaFin": "2026-11-20",
    "evaluacionesIncluidas": 2,
    "montoFacturado": 2850.00,
    "moneda": "BOB",
    "nroFactura": "F-2026-004512"
  }
}
```

### 11 · bc3.SuscripcionPausada

- **Descripción:** el paciente suspende el servicio por un rango de fechas y la suscripción se
  extiende en la misma cantidad de días.
  - Las fechas pausadas ya no entran al consolidado, así que BC4 y BC5 no necesitan este evento.
  - BC1 mueve las evaluaciones que caigan dentro de la pausa.
- **Productor:** MsCateringSubscription
- **Consumidores:** MsPatientsConsulCore, MsNotificaciones

```json
{
  "eventId": "3f2a0011-9c1e-4b7a-a1d2-7e5f0c8b1011",
  "eventName": "bc3.SuscripcionPausada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-25T14:00:00Z",
  "source": "MsCateringSubscription",
  "correlationId": "corr-pausa-SUS-501-01",
  "data": {
    "idSuscripcion": "SUS-501",
    "idPaciente": "PAC-1024",
    "fechaDesde": "2026-10-28",
    "fechaHasta": "2026-10-29",
    "motivo": "VIAJE",
    "nuevaFechaFin": "2026-11-22",
    "fechaSolicitud": "2026-10-25"
  }
}
```

### 12 · bc3.SuscripcionReactivada

- **Descripción:** termina la pausa y se reanudan las entregas.
- **Productor:** MsCateringSubscription
- **Consumidores:** MsPatientsConsulCore, MsNotificaciones

```json
{
  "eventId": "3f2a0012-9c1e-4b7a-a1d2-7e5f0c8b1012",
  "eventName": "bc3.SuscripcionReactivada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-30T04:00:00Z",
  "source": "MsCateringSubscription",
  "correlationId": "corr-pausa-SUS-501-01",
  "data": {
    "idSuscripcion": "SUS-501",
    "idPaciente": "PAC-1024",
    "fechaReactivacion": "2026-10-30",
    "fechaFin": "2026-11-22"
  }
}
```

### 13 · bc3.SuscripcionCancelada

- **Descripción:** baja o vencimiento de la suscripción.
  - BC1 anula las evaluaciones pendientes.
  - BC2 desactiva el plan personalizado.
- **Productor:** MsCateringSubscription
- **Consumidores:** MsPatientsConsulCore, MSNutritionPlanner, MsNotificaciones

```json
{
  "eventId": "3f2a0013-9c1e-4b7a-a1d2-7e5f0c8b1013",
  "eventName": "bc3.SuscripcionCancelada",
  "eventVersion": "1.0",
  "occurredOn": "2026-11-23T03:59:59Z",
  "source": "MsCateringSubscription",
  "correlationId": "corr-baja-SUS-501",
  "data": {
    "idSuscripcion": "SUS-501",
    "idPaciente": "PAC-1024",
    "idPlan": "PLAN-205",
    "tipoBaja": "VENCIMIENTO",
    "motivo": "Finalización del periodo contratado",
    "fechaEfectiva": "2026-11-22",
    "entregasPendientesAnuladas": 0
  }
}
```

### 14 · bc3.CalendarioConsolidadoCerrado

- **Descripción:** cierre del calendario de un día de entrega. Es el disparador de la producción.
  Cada entrega trae todo lo que BC4 necesita para crear la orden y etiquetar los paquetes: paciente,
  dirección resuelta y recetas con sus porciones.
- **Productor:** MsCateringSubscription
- **Consumidores:** MsProductionPackaging

```json
{
  "eventId": "3f2a0014-9c1e-4b7a-a1d2-7e5f0c8b1014",
  "eventName": "bc3.CalendarioConsolidadoCerrado",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-22T03:59:59Z",
  "source": "MsCateringSubscription",
  "correlationId": "corr-entrega-2026-10-23",
  "data": {
    "idCalendario": "CAL-20261023",
    "fechaEntrega": "2026-10-23",
    "fechaCierre": "2026-10-21T23:59:59-04:00",
    "totalEntregas": 2,
    "entregas": [
      {
        "idCalendarioDia": "CD-70011",
        "idSuscripcion": "SUS-501",
        "idPaciente": "PAC-1024",
        "nombrePaciente": "Juan Pérez Rojas",
        "idPlan": "PLAN-205",
        "numDiaPlan": 2,
        "direccion": {
          "idDireccion": "DIR-58",
          "alias": "Trabajo",
          "direccion": "Av. Cristo Redentor #1450, Edif. Torre Norte, piso 3",
          "referencia": "Frente al Banco Unión",
          "ciudad": "Santa Cruz de la Sierra",
          "latitud": -17.7612,
          "longitud": -63.1825
        },
        "recetas": [
          { "idReceta": "REC-14", "nombre": "Avena cocida con frutos rojos", "hora": "08:00", "porcionGramos": 250 },
          { "idReceta": "REC-22", "nombre": "Yogur natural con semillas de girasol", "hora": "08:00", "porcionGramos": 150 },
          { "idReceta": "REC-31", "nombre": "Sopa de verduras", "hora": "13:00", "porcionGramos": 300 },
          { "idReceta": "REC-45", "nombre": "Pechuga a la plancha con quinua y vegetales", "hora": "19:30", "porcionGramos": 350 }
        ]
      },
      {
        "idCalendarioDia": "CD-70012",
        "idSuscripcion": "SUS-502",
        "idPaciente": "PAC-1031",
        "nombrePaciente": "María López Vaca",
        "idPlan": "PLAN-41",
        "numDiaPlan": 2,
        "direccion": {
          "idDireccion": "DIR-61",
          "alias": "Domicilio",
          "direccion": "Calle Junín #245, Barrio Centro",
          "referencia": "Portón verde",
          "ciudad": "Santa Cruz de la Sierra",
          "latitud": -17.7840,
          "longitud": -63.1810
        },
        "recetas": [
          { "idReceta": "REC-14", "nombre": "Avena cocida con frutos rojos", "hora": "08:00", "porcionGramos": 250 },
          { "idReceta": "REC-31", "nombre": "Sopa de verduras", "hora": "13:00", "porcionGramos": 300 },
          { "idReceta": "REC-33", "nombre": "Ensalada fresca", "hora": "13:00", "porcionGramos": 200 },
          { "idReceta": "REC-45", "nombre": "Pechuga a la plancha con quinua y vegetales", "hora": "19:30", "porcionGramos": 350 }
        ]
      }
    ]
  }
}
```

### 15 · bc4.PaqueteListo

- **Descripción:** un paquete quedó envasado, etiquetado y listo. Logística lo pre-registra antes de
  planificar la ruta. `idCalendarioDia` permite que los eventos de BC5 lleguen a Catering
  correlacionados con su día de calendario.
- **Productor:** MsProductionPackaging
- **Consumidores:** MsLogisticDelivery

```json
{
  "eventId": "3f2a0015-9c1e-4b7a-a1d2-7e5f0c8b1015",
  "eventName": "bc4.PaqueteListo",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-23T01:15:00Z",
  "source": "MsProductionPackaging",
  "correlationId": "corr-entrega-2026-10-23",
  "data": {
    "idPaquete": "PKG-20261023-00087",
    "etiqueta": "NUR-0087",
    "idOrdenProduccion": "ORD-3120",
    "idCalendarioDia": "CD-70011",
    "idSuscripcion": "SUS-501",
    "idPaciente": "PAC-1024",
    "nombrePaciente": "Juan Pérez Rojas",
    "fechaEntrega": "2026-10-23",
    "direccionEntrega": {
      "idDireccion": "DIR-58",
      "direccion": "Av. Cristo Redentor #1450, Edif. Torre Norte, piso 3",
      "referencia": "Frente al Banco Unión",
      "ciudad": "Santa Cruz de la Sierra",
      "latitud": -17.7612,
      "longitud": -63.1825
    },
    "nroPorciones": 4
  }
}
```

### 16 · bc4.OrdenCompletada

- **Descripción:** se cerró el lote del día; todos los paquetes están listos. Logística puede
  planificar las rutas.
- **Productor:** MsProductionPackaging
- **Consumidores:** MsLogisticDelivery

```json
{
  "eventId": "3f2a0016-9c1e-4b7a-a1d2-7e5f0c8b1016",
  "eventName": "bc4.OrdenCompletada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-23T01:40:00Z",
  "source": "MsProductionPackaging",
  "correlationId": "corr-entrega-2026-10-23",
  "data": {
    "idOrdenProduccion": "ORD-3120",
    "idCalendario": "CAL-20261023",
    "fechaProduccion": "2026-10-22",
    "fechaEntrega": "2026-10-23",
    "totalRecetasPreparadas": 8,
    "totalPaquetes": 2,
    "paquetesIds": ["PKG-20261023-00087", "PKG-20261023-00088"],
    "fechaCierre": "2026-10-22T21:40:00-04:00"
  }
}
```

### 17 · bc5.EntregaProgramada

- **Descripción:** la entrega quedó asignada a una ruta y una ventana horaria. Se avisa al paciente.
- **Productor:** MsLogisticDelivery
- **Consumidores:** MsNotificaciones

```json
{
  "eventId": "3f2a0017-9c1e-4b7a-a1d2-7e5f0c8b1017",
  "eventName": "bc5.EntregaProgramada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-23T02:30:00Z",
  "source": "MsLogisticDelivery",
  "correlationId": "corr-entrega-2026-10-23",
  "data": {
    "idEntrega": "ENT-9901",
    "idPaquete": "PKG-20261023-00087",
    "idCalendarioDia": "CD-70011",
    "idPaciente": "PAC-1024",
    "idRuta": "RUT-415",
    "idRepartidor": "REP-9",
    "fechaEntrega": "2026-10-23",
    "ventanaHoraria": { "desde": "07:00", "hasta": "09:00" },
    "ordenParada": 1
  }
}
```

### 18 · bc5.RutaIniciada

- **Descripción:** el repartidor salió a ruta. Se avisa a cada paciente que su paquete está en
  camino, con la hora estimada.
- **Productor:** MsLogisticDelivery
- **Consumidores:** MsNotificaciones

```json
{
  "eventId": "3f2a0018-9c1e-4b7a-a1d2-7e5f0c8b1018",
  "eventName": "bc5.RutaIniciada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-23T10:45:00Z",
  "source": "MsLogisticDelivery",
  "correlationId": "corr-entrega-2026-10-23",
  "data": {
    "idRuta": "RUT-415",
    "idRepartidor": "REP-9",
    "nombreRepartidor": "Carlos Méndez",
    "fechaHoraInicio": "2026-10-23T06:45:00-04:00",
    "totalParadas": 2,
    "entregas": [
      { "idEntrega": "ENT-9901", "idPaciente": "PAC-1024", "ordenParada": 1, "etaEstimada": "07:35" },
      { "idEntrega": "ENT-9902", "idPaciente": "PAC-1031", "ordenParada": 2, "etaEstimada": "07:50" }
    ]
  }
}
```

### 19 · bc5.RepartidorLlegoAParada

- **Descripción:** el repartidor llegó a la dirección del paciente. Se le avisa que su pedido llegó.
- **Productor:** MsLogisticDelivery
- **Consumidores:** MsNotificaciones

```json
{
  "eventId": "3f2a0019-9c1e-4b7a-a1d2-7e5f0c8b1019",
  "eventName": "bc5.RepartidorLlegoAParada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-23T11:32:10Z",
  "source": "MsLogisticDelivery",
  "correlationId": "corr-entrega-2026-10-23",
  "data": {
    "idEntrega": "ENT-9901",
    "idRuta": "RUT-415",
    "idPaciente": "PAC-1024",
    "idPaquete": "PKG-20261023-00087",
    "ordenParada": 1,
    "fechaHoraLlegada": "2026-10-23T07:32:10-04:00",
    "geolocalizacion": { "latitud": -17.76118, "longitud": -63.18247 }
  }
}
```

### 20 · bc5.EntregaConfirmada

- **Descripción:** el paciente recibió el paquete.
  - Catering marca el día del calendario como entregado, lo que cierra el ciclo de la suscripción.
  - Se envía la confirmación al paciente.
- **Productor:** MsLogisticDelivery
- **Consumidores:** MsCateringSubscription, MsNotificaciones

```json
{
  "eventId": "3f2a0020-9c1e-4b7a-a1d2-7e5f0c8b1020",
  "eventName": "bc5.EntregaConfirmada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-23T11:35:00Z",
  "source": "MsLogisticDelivery",
  "correlationId": "corr-entrega-2026-10-23",
  "data": {
    "idEntrega": "ENT-9901",
    "idPaquete": "PKG-20261023-00087",
    "idCalendarioDia": "CD-70011",
    "idSuscripcion": "SUS-501",
    "idPaciente": "PAC-1024",
    "fechaHoraEntrega": "2026-10-23T07:35:00-04:00",
    "recibidoPor": "Juan Pérez Rojas",
    "evidenciaFotoUrl": "https://cdn.nurtricenter.bo/evidencias/ENT-9901.jpg",
    "geolocalizacion": { "latitud": -17.76118, "longitud": -63.18247 }
  }
}
```

### 21 · bc5.EntregaFallida

- **Descripción:** un intento de entrega no se concretó. Se avisa al paciente. Todavía no es el
  resultado final: puede haber un reintento.
- **Productor:** MsLogisticDelivery
- **Consumidores:** MsNotificaciones

```json
{
  "eventId": "3f2a0021-9c1e-4b7a-a1d2-7e5f0c8b1021",
  "eventName": "bc5.EntregaFallida",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-23T11:50:00Z",
  "source": "MsLogisticDelivery",
  "correlationId": "corr-entrega-2026-10-23",
  "data": {
    "idEntrega": "ENT-9902",
    "idPaquete": "PKG-20261023-00088",
    "idCalendarioDia": "CD-70012",
    "idPaciente": "PAC-1031",
    "intento": 1,
    "motivo": "DESTINATARIO_AUSENTE",
    "observacion": "Nadie atendió tras 10 minutos de espera",
    "fechaHoraIntento": "2026-10-23T07:50:00-04:00",
    "evidenciaFotoUrl": "https://cdn.nurtricenter.bo/evidencias/ENT-9902-1.jpg",
    "geolocalizacion": { "latitud": -17.7840, "longitud": -63.1810 }
  }
}
```

### 22 · bc5.EntregaReprogramada

- **Descripción:** se programó un nuevo intento de entrega en el mismo día. Se avisa al paciente.
- **Productor:** MsLogisticDelivery
- **Consumidores:** MsNotificaciones

```json
{
  "eventId": "3f2a0022-9c1e-4b7a-a1d2-7e5f0c8b1022",
  "eventName": "bc5.EntregaReprogramada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-23T12:00:00Z",
  "source": "MsLogisticDelivery",
  "correlationId": "corr-entrega-2026-10-23",
  "data": {
    "idEntrega": "ENT-9902",
    "idPaquete": "PKG-20261023-00088",
    "idPaciente": "PAC-1031",
    "intento": 2,
    "fechaOriginal": "2026-10-23T07:50:00-04:00",
    "nuevaFechaHora": "2026-10-23T11:00:00-04:00",
    "idRutaNueva": "RUT-418",
    "motivo": "Reintento tras destinatario ausente"
  }
}
```

### 23 · bc5.EntregaNoConcretada

- **Descripción:** se agotaron los intentos de entrega; es el resultado final.
  - Catering aplica la compensación: un día adicional, que entra en un consolidado posterior.
  - Se avisa al paciente.
- **Productor:** MsLogisticDelivery
- **Consumidores:** MsCateringSubscription, MsNotificaciones

```json
{
  "eventId": "3f2a0023-9c1e-4b7a-a1d2-7e5f0c8b1023",
  "eventName": "bc5.EntregaNoConcretada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-23T15:20:00Z",
  "source": "MsLogisticDelivery",
  "correlationId": "corr-entrega-2026-10-23",
  "data": {
    "idEntrega": "ENT-9902",
    "idPaquete": "PKG-20261023-00088",
    "idCalendarioDia": "CD-70012",
    "idSuscripcion": "SUS-502",
    "idPaciente": "PAC-1031",
    "fechaEntrega": "2026-10-23",
    "intentosRealizados": 2,
    "motivoFinal": "DESTINATARIO_AUSENTE",
    "aplicaCompensacion": true,
    "tipoCompensacion": "DIA_ADICIONAL"
  }
}
```

## 4. Pendiente de acordar en el grupo (no va en el documento)

- **Broker único.** BC4 publica hoy en Kafka y Logística usa RabbitMQ (exchange
  `nurtricenter.eventos`). Hay que elegir uno, o poner un puente entre los dos.
- **Sobre común en el código.** Una vez aprobado, cada servicio debe adaptar su sobre al de este
  documento: BC4 usa `eventType`/`payload` y Logística usa `type`/`payload`.
- **Eventos que aún no existen en el código:**
  - BC4: los campos extra de `PaqueteListo` y `OrdenCompletada`.
  - Logística: `EntregaProgramada` por entrega, `RutaIniciada` y `RepartidorLlegoAParada`.
  - Catering: la publicación de todos sus eventos, que hoy solo se registran en el log.
