# ms-nutrition-planner

Microservicio REST del contexto delimitado **"Planes Nutricionales y Recetas"**,
uno de los 5 microservicios identificados en el dominio de **Nur-tricenter**
(caso de estudio). Implementado con **Java + Spring Boot**, **DDD** (Domain
Driven Design), **JPA/Hibernate** sobre **MySQL**, y baja lógica en toda
eliminación.

## 1. Alcance del contexto delimitado

Este microservicio es responsable de:

- La elaboración del **plan alimentario base** (servicio de Asesoramiento
  Nutricional: plan de 15 días con tiempos de comida y recetas asignadas).
- Las **versiones personalizadas** de un plan (cuando, dentro del servicio de
  Catering, el paciente o el profesional requieren cambios sobre la
  planificación original).
- El catálogo de **recetas** y sus **ingredientes**.

Quedan fuera de este contexto (y corresponderían a los otros 4 microservicios
del dominio): pacientes y evaluaciones corporales, contratación/facturación de
servicios, calendario de entrega y direcciones, y producción/logística de
entrega (órdenes de producción, rutas, geolocalización).

## 2. Arquitectura

Paquete raíz: `com.nutrition.msnutritionplanner`

```
domain/                     Entidades de dominio, Value Objects, reglas de negocio, puertos (interfaces de repositorio)
  shared/                   Hora, Orden, EstadoPlan, TipoIngrediente, AggregateRoot, ValueObject
  exceptions/                DomainException y subclases
  ingrediente/               Agregado Ingrediente
  receta/                     Agregado Receta
  planbase/                   Agregado PlanAlimentarioBase (+ Dia, TiempoDeComida internos)
  planpersonalizado/         Agregado PlanAlimentarioPersonalizado (extiende PlanAlimentarioBase)

application/                Casos de uso (orquestan dominio + repositorios) y DTOs (records)
  ingrediente/ , receta/ , planbase/ , planpersonalizado/   (cada uno con su sub-paquete dto)

infrastructure/
  persistence/model/          Entidades JPA (@Entity)
  persistence/repositories/   Repositorios Spring Data + adaptadores que implementan los puertos del dominio
  shared/                     ModelMapperConfig

presentation/                Controladores REST + manejador global de excepciones
config/                       Configuración de la app (OpenAPI/Swagger)
```

Se sigue **arquitectura hexagonal**: el dominio no depende de Spring ni de
JPA; solo define **puertos** (`IngredienteRepository`, `RecetaRepository`,
`PlanAlimentarioBaseRepository`, `PlanAlimentarioPersonalizadoRepository`) que
la capa de infraestructura implementa.

### 2.1 Agregados y límites de consistencia

- **Ingrediente** (raíz de agregado)
- **Receta** (raíz de agregado) — referencia a `Ingrediente` **solo por id**
  (`ingredientesIds`), nunca mantiene la entidad completa, respetando el
  límite de consistencia entre agregados.
- **PlanAlimentarioBase** (raíz de agregado) — contiene, por composición,
  `Dia` (1..N, ordenados) y cada `Dia` contiene `TiempoDeComida` (1..N). Los
  `TiempoDeComida` referencian `Receta` solo por id.
- **PlanAlimentarioPersonalizado extends PlanAlimentarioBase** — tal como en
  el modelo de dominio adjunto (relación de generalización), es una nueva
  versión de un plan, derivada de un `PlanAlimentarioBase` (o de otro
  personalizado) ya `COMPLETO`, agregando `idPlanBase` y
  `descripcionCambios`.

### 2.2 Por qué `PlanAlimentarioBase` y `PlanAlimentarioPersonalizado` comparten tabla

Se mantuvo el diseño de tabla única `plan_alimentario` con auto-referencia
`id_plan_base` (igual que en el script original adjunto): si `id_plan_base`
es `NULL`, la fila es un plan **base**; si no, es una versión
**personalizada** derivada de ese plan. Esto se refleja en el dominio como
herencia (`PlanAlimentarioPersonalizado extends PlanAlimentarioBase`) y en
JPA como una única `PlanAlimentarioJpaEntity` con relación `@ManyToOne`
hacia sí misma. Los repositorios (`PlanAlimentarioBaseRepositoryImpl` /
`PlanAlimentarioPersonalizadoRepositoryImpl`) filtran por
`planBase IS NULL` / `IS NOT NULL` y reconstruyen el subtipo de dominio
correspondiente.

### 2.3 Mapeo dominio ↔ persistencia (sin getters/setters en el dominio)

Las entidades de dominio **no exponen setters** (son ricas en
comportamiento, las reglas de negocio viven en sus métodos). Para el mapeo:

- **Ingrediente** y **Receta** (campos escalares): `ModelMapper`, configurado
  en `ModelMapperConfig` con `setFieldMatchingEnabled(true)` y
  `setFieldAccessLevel(PRIVATE)` — es decir, mapea por **acceso directo a
  campos**, no por getters/setters.
- **Receta → ingredientes** y todo el agregado **PlanAlimentarioBase**
  (`Dia`, `TiempoDeComida`): mapeo **manual** en `RecetaMapper` /
  `PlanAlimentarioMapper`, porque el dominio usa Value Objects (`Hora`,
  `Orden`) y colecciones de **ids** mientras que JPA usa tipos primitivos y
  relaciones `@ManyToMany`/`@OneToMany` completas; además se necesita una
  estrategia de **merge por id** (agregar/actualizar/eliminar filas) para no
  perder la identidad de las filas existentes al guardar un agregado
  completo.

### 2.4 Baja lógica

Ninguna operación de borrado ejecuta `DELETE`. Todas las tablas principales
(`ingrediente`, `receta`, `plan_alimentario`) tienen una columna `activo`
(`TINYINT(1)`, default `1`). Los endpoints `DELETE /api/v1/...` invocan el
método de dominio `darDeBaja()` de la raíz de agregado y luego la guardan
(`UPDATE ... SET activo = 0`). Las listas (`GET` sin id) solo devuelven
registros activos.

## 3. Modelo de dominio (resumen)

| Agregado | Entidades/VOs internos | Reglas de negocio destacadas |
|---|---|---|
| `Ingrediente` | — | nombre y tipo obligatorios; no se edita si está dado de baja |
| `Receta` | — | al menos 1 ingrediente; no se edita si está dada de baja |
| `PlanAlimentarioBase` | `Dia` (Orden), `TiempoDeComida` (Hora) | solo editable en estado `INICIADO`; no se permiten horas duplicadas en un mismo día; un día/tiempo no puede quedar vacío; `completar()` requiere al menos un día; transiciones de estado `INICIADO → COMPLETO/DESCARTADO` |
| `PlanAlimentarioPersonalizado` | (hereda de `PlanAlimentarioBase`) | solo se puede crear a partir de un plan de origen en estado `COMPLETO` |

## 4. Endpoints principales

- `POST/GET/PUT/DELETE /api/v1/ingredientes`
- `POST/GET/PUT/DELETE /api/v1/recetas`, `POST|DELETE /api/v1/recetas/{id}/ingredientes[/{idIngrediente}]`
- `POST/GET/DELETE /api/v1/planes-base`, `.../{id}/dias`, `.../{id}/dias/{idDia}/tiempos-comida`, `.../{id}/completar`, `.../{id}/descartar`
- `POST/GET/DELETE /api/v1/planes-personalizados` (análogo al anterior, `GET` requiere `?idPlanBase=`)

Documentación interactiva: `http://localhost:7000/swagger-ui.html`

## 5. Base de datos

Script completo (DDL + datos de ejemplo) en `db/FOOD_CREATION.sql`. Ejecutarlo
directamente en MySQL 8 **antes** de levantar la aplicación (se configuró
`spring.jpa.hibernate.ddl-auto=validate` para que Hibernate no modifique el
esquema). Si se prefiere que Hibernate genere/actualice el esquema
automáticamente en desarrollo, cambiar esa propiedad a `update` y no es
obligatorio correr el script (aunque sí se recomienda para tener datos de
ejemplo).

## 6. Cómo ejecutar

```bash
mysql -u root -p < db/FOOD_CREATION.sql
mvn spring-boot:run
```

La API queda disponible en `http://localhost:7000`.

## 7. Testing

### 7.1 Unit tests (`mvn test`)

Cubren `domain/*` (100% de las clases, sin Spring ni Mockito), un caso de
uso de ejemplo en `application/*` (Mockito) y un controller de ejemplo en
`presentation/*` (MockMvc *standalone*). El resto de casos de uso y
controllers siguen el mismo patrón (ver `PROMPT_TESTS.md`).

```bash
mvn test
```

Genera el reporte de cobertura de JaCoCo en `target/site/jacoco/index.html`
(ábrelo en el navegador). El plugin está configurado para exigir un mínimo
de **80% de instrucciones cubiertas** al correr `mvn verify` (ver 7.3).

### 7.2 Integration tests (`mvn verify`)

Dos flujos completos, de punta a punta, contra la API HTTP real (Spring
context completo + base de datos H2 en memoria, ver
`src/test/resources/application.properties`):

- `PlanAlimentarioBaseFlujoCompletoIT`: ingredientes → receta → plan base →
  día → tiempo de comida → completar → verificación final.
- `PlanAlimentarioPersonalizadoFlujoCompletoIT`: plan base `COMPLETO` →
  plan personalizado derivado → día/tiempo propios → completar →
  verificación del enlace `idPlanBase` y de que el plan base original no
  se vio afectado.

```bash
mvn verify
```

(`mvn test` NO los ejecuta: `maven-surefire-plugin` excluye `*IT`, y
`maven-failsafe-plugin` los corre en la fase `integration-test` dentro de
`mvn verify`.)

### 7.3 Code coverage

```bash
mvn verify   # corre unit + integration tests y aplica jacoco:check (≥80%)
open target/site/jacoco/index.html   # (o abrir manualmente en el navegador)
```

Si el build falla en `jacoco-check`, el reporte HTML indica exactamente qué
paquete/clase está por debajo del umbral.

### 7.4 Tests generados con IA

Ver **`PROMPT_TESTS.md`** para el detalle de qué se generó con ayuda de IA,
las reglas de proyecto que se le dieron como contexto, y los prompts
reutilizables para generar el resto de los tests siguiendo la misma
convención.

## 8. Decisiones y supuestos adicionales

- El estado del plan (`EstadoPlan`: `INICIADO`, `COMPLETO`, `DESCARTADO`) es
  distinto de la baja lógica (`activo`): el primero es una regla de negocio
  del ciclo de vida del plan; el segundo es un borrado administrativo.
- `TiempoDeComida.idTiempo` se generó como `AUTO_INCREMENT` (en el script
  original no lo era); se asume que es un identificador técnico y no un
  catálogo fijo de tiempos de comida (desayuno/almuerzo/cena), ya que el
  dominio no definía ese catálogo explícitamente.
- Se agregaron las entidades/reglas faltantes en el modelo de dominio
  adjunto: baja lógica en los 3 agregados raíz, transición de estados del
  plan, validación de unicidad de hora por día, y la regla de que un plan
  personalizado solo puede originarse desde un plan `COMPLETO`.
