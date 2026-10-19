# PROMPT_TESTS.md — Skill / prompt usado para generar la capa de testing

Este documento deja constancia de **cómo se generó (o se debe generar) el
código de testing** de `ms-nutrition-planner` con ayuda de IA, tal como pide
la consigna de la actividad ("si se usa código generado, incluir las skills y
reglas de proyecto que se utilizaron").

- **Herramienta usada:** Claude (Anthropic) y el Agente de IA Antigravity, vía chat con acceso a herramientas de archivo/terminal sobre el propio repositorio del proyecto.
- **Alcance generado con IA:** configuración de JaCoCo/Failsafe en el
  `pom.xml`, `application.properties` de test (H2), unit tests del dominio
  completo, un unit test de ejemplo de la capa `application` (Mockito) y uno
  de `presentation` (MockMvc standalone), y los 2 integration tests de flujo
  completo. Finalmente, el Agente Antigravity fue instruido para generar el 
  resto de las pruebas unitarias en las capas de aplicación, presentación e
  infraestructura para superar el 80% de cobertura en JaCoCo.
- **Lo que NO reemplaza:** el criterio del estudiante. Todo test generado fue
  revisado antes de darlo por válido: se corrió `mvn test` / `mvn verify`, se
  leyó el reporte de JaCoCo y se completaron a mano los casos que la IA no
  cubrió (ver sección 4).

---

## 1. Reglas de proyecto (contexto que se le dio a la IA)

Estas son las convenciones fijas del proyecto que cualquier test generado
—por IA o a mano— debe respetar, porque ya están reflejadas en el skeleton
que se entrega en este repo:

1. **Paquete raíz:** `com.nutrition.msnutritionplanner`. Los tests replican
   la misma estructura de paquetes que el código de producción
   (`src/test/java/.../domain/...`, `.../application/...`, etc.), **un
   archivo de test por clase de producción**, con el sufijo `Test`
   (`RecetaTest` para `Receta`).
2. **Los tests de `domain/*`** son POJOs puros: no usan Spring, no usan
   Mockito, no acceden a base de datos. Se instancia la entidad/VO
   directamente y se llama a sus métodos de negocio.
   - Los tests de entidades **internas** de un agregado (`Dia`,
     `TiempoDeComida`, con métodos *package-private*) se ubican en el mismo
     paquete Java que la clase (`domain.planbase`), para poder invocar esos
     métodos sin exponerlos públicamente fuera del agregado.
   - Para validar excepciones se usa
     `org.assertj.core.api.Assertions.assertThatThrownBy(...).isInstanceOf(...)`,
     no `try/catch` manual.
   - Cada clase de test lleva `@DisplayName` a nivel de clase y de método,
     en español, describiendo el comportamiento verificado (no el nombre
     técnico del método).
3. **Los tests de `application/*` (casos de uso)** usan
   `@ExtendWith(MockitoExtension.class)` y mockean **solo el puerto de
   repositorio** (la interfaz de dominio, p. ej. `IngredienteRepository`),
   nunca una clase concreta de infraestructura. Se verifica:
   - Que el caso de uso llame al repositorio con los datos correctos
     (`verify(...)`, `ArgumentCaptor` cuando haga falta revisar el estado
     del objeto de dominio que se guardó).
   - Que las excepciones de dominio (`EntityNotFoundException`,
     `DomainValidationException`, `BusinessRuleException`) se propaguen tal
     cual hacia el caller.
4. **Los tests de `presentation/*` (controllers)** usan
   `MockMvcBuilders.standaloneSetup(controller)` (NO `@WebMvcTest` ni
   `@SpringBootTest`, para que sean rápidos y no dependan del contexto
   completo), registrando siempre
   `.setControllerAdvice(new GlobalExceptionHandler())` para poder probar
   también los códigos de error HTTP. Se mockea el caso de uso
   correspondiente con Mockito.
5. **Los integration tests** van en el paquete
   `com.nutrition.msnutritionplanner.integration`, con sufijo **`IT`** (no
   `Test`), para que Maven los separe de los unit tests
   (`maven-surefire-plugin` los excluye, `maven-failsafe-plugin` los
   incluye). Usan `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)` +
   `@Transactional` (para que cada test revierta
   sus datos) y corren contra la base H2 en memoria configurada en
   `src/test/resources/application.properties`. Por problemas de inyección
   en el test-slice de Spring Boot 4.1.0, el `MockMvc` se debe configurar
   manualmente usando `MockMvcBuilders.webAppContextSetup(wac).build()` en
   un `@BeforeEach` (inyectando `WebApplicationContext`), y el `ObjectMapper`
   debe ser instanciado con `new ObjectMapper()`. Cada clase de integration
   test representa **un flujo de negocio completo**, de punta a punta,
   sobre la API HTTP real (no se llama a los casos de uso directamente).
6. **Nombres, formato y estilo:** Java 21, `AssertJ` para las aserciones
   (`assertThat(...)`), bloques de texto (`"""..."""`) para el JSON de los
   `MockMvc`, sin librerías adicionales fuera de las ya declaradas en el
   `pom.xml` (JUnit 5, Mockito, AssertJ, MockMvc, H2, JaCoCo).

## 2. Prompt base (reutilizable) para pedirle a la IA el resto de los tests

Copia y adapta este prompt reemplazando `<Clase>` y `<paquete>` por la clase
puntual que falte cubrir. Está pensado para pegarse en el chat de la IA
**con el archivo de la clase de producción adjunto/visible**, y con este
mismo repositorio como contexto (para que la IA pueda leer las convenciones
ya aplicadas en los tests existentes):

```
Actúa como un desarrollador Java senior escribiendo tests para el proyecto
ms-nutrition-planner (arquitectura DDD + hexagonal, Spring Boot, Java 21).

Quiero que generes el unit test de la clase <paquete>.<Clase>.

Reglas obligatorias (ya aplicadas en el resto del repo, revísalas antes de
escribir código):
1. Sigue exactamente las convenciones descritas en PROMPT_TESTS.md de este
   repositorio (estructura de paquetes, JUnit 5 + AssertJ, @DisplayName en
   español, Mockito solo para los puertos de repositorio si es un caso de
   uso, MockMvc standalone si es un controller).
2. Si <Clase> es una entidad o Value Object de dominio (domain/*), NO uses
   Spring ni Mockito: instánciala directamente y prueba sus reglas de
   negocio (casos válidos, casos inválidos que deben lanzar
   DomainValidationException/BusinessRuleException, y los casos límite
   mencionados en los comentarios Javadoc del código).
3. Cubre TODOS los métodos públicos (y package-private si el test queda en
   el mismo paquete) de la clase, incluyendo las ramas de error.
4. No inventes comportamiento que no esté en el código: si una regla no es
   clara, pregúntame en vez de asumir.
5. El objetivo es alcanzar como mínimo 80% de cobertura de instrucciones
   (JaCoCo) en el proyecto completo; prioriza cubrir las ramas condicionales
   (if/else, validaciones, excepciones) por sobre casos redundantes.

Código de la clase a testear:
<pega aquí el contenido de la clase>
```

### Variante para casos de uso (application)

Agrega esta instrucción extra al prompt anterior:

```
Esta clase es un caso de uso (capa application). Usa
@ExtendWith(MockitoExtension.class), mockea únicamente el/los puertos de
repositorio de dominio que reciba por constructor (@Mock + constructor
manual en @BeforeEach, no @InjectMocks), y verifica con verify(...) que se
llame al repositorio con los argumentos esperados. No mockees clases de
dominio (Ingrediente, Receta, etc.): son objetos reales, constrúyelos con
sus factory methods (crear/reconstruir).
```

### Variante para controllers (presentation)

```
Esta clase es un controller REST. Usa
MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new
GlobalExceptionHandler()).build() en un @BeforeEach, mockea el caso de uso
correspondiente, y cubre: 201/200 en el camino feliz, 400 por validación
Bean Validation (@Valid) cuando aplique, 404 cuando el caso de uso lance
EntityNotFoundException, y 204 en los endpoints DELETE.
```

### Prompt para integration tests (flujos nuevos)

```
Genera un integration test (@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) +
@Transactional, sufijo IT, paquete
com.nutrition.msnutritionplanner.integration) que cubra el siguiente flujo
de negocio de punta a punta, usando MockMvc contra los endpoints REST
reales (no llames directamente a los casos de uso). Configura MockMvc
manualmente en un @BeforeEach con MockMvcBuilders.webAppContextSetup(wac) e 
instancia ObjectMapper con new ObjectMapper() (debido a problemas en SB 4.1.0):

<describe el flujo paso a paso, ej: "dar de baja una receta y verificar que
ya no aparece en el listado pero sigue existiendo en la base de datos con
activo=0">

Sigue el mismo estilo que
PlanAlimentarioBaseFlujoCompletoIT.java / PlanAlimentarioPersonalizadoFlujoCompletoIT.java
ya presentes en el repo (helpers privados para crear datos de prueba,
bloques de texto para el JSON, JsonNode + ObjectMapper para extraer ids
generados de las respuestas).
```

## 3. Cómo se verificó lo generado (no se aceptó "a ciegas")

1. `mvn test` → todos los unit tests en verde + reporte JaCoCo en
   `target/site/jacoco/index.html`.
2. Se revisó manualmente el % de cobertura por paquete y se identificaron
   los paquetes por debajo del 80% para reforzarlos primero (ver sección 4).
3. `mvn verify` → corre además los `*IT` contra H2 y aplica el
   `jacoco:check` (falla el build si baja de 80%).
4. Cada test generado se leyó línea por línea: se verificó que el
   `@DisplayName` describiera realmente lo que el código comprueba, y que
   ninguna aserción fuera trivial (`assertThat(true).isTrue()` o similar).

## 4. Estado Final de Cobertura

Se completó la implementación de las pruebas unitarias para todas las capas requeridas:
1. **Casos de Uso (`application/*`):** `PlanAlimentarioBaseUseCasesTest`, `PlanAlimentarioPersonalizadoUseCasesTest`, `RecetaUseCasesTest` e `IngredienteUseCasesTest`.
2. **Mappers e Infraestructura (`infrastructure/*`):** `PlanAlimentarioMapperTest`, `IngredienteMapperTest`, `RecetaMapperTest`, `IngredienteRepositoryImplTest`, `RecetaRepositoryImplTest`, `PlanAlimentarioBaseRepositoryImplTest`, `PlanAlimentarioPersonalizadoRepositoryImplTest`.
3. **Controladores y Manejo de Errores (`presentation/*`):** `PlanAlimentarioBaseControllerTest`, `PlanAlimentarioPersonalizadoControllerTest`, `RecetaControllerTest`, `IngredienteControllerTest`, `GlobalExceptionHandlerTest`.

Tras ejecutar `mvn clean test jacoco:report`, el reporte generado por JaCoCo confirma una cobertura total de **92.09%** de las instrucciones del proyecto (superando ampliamente la meta mínima del 80%).

