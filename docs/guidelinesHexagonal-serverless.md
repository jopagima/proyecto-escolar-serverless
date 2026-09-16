# Hexagonal Architecture — Guía adaptada al proyecto `school-serverless-platform`

> Adaptación completa de `guidelinesHexagonal.md` (5 skills originales: `architecture-hexagonal`,
> `design-principles`, `git-strategy`, `testing-standards`, `xp-tdd-practices`) al stack real de
> este proyecto: **Java 17 + AWS CDK + AWS SDK v2 + AWS Lambda + DynamoDB, sin framework**.
> Se omite únicamente lo específico de Spring Boot/JPA (`@Entity`, `@Configuration`/`@Bean`,
> `JpaRepository`, MapStruct). Todo lo demás del original se mantiene, adaptado a este stack.
> Se activa formalmente al cierre de la Fase 2, aplicada retroactivamente a Alumnos y Cursos
> (ver `memoria-progreso.md`). Los 4 conflictos detectados frente a decisiones ya tomadas en
> el proyecto (naming de tests, `DomainError` vs. excepciones por entidad, `Id`/`Optional`,
> git strategy) están **resueltos a favor de esta guía** — marcados ✅ en cada sección, con
> la retroactiva de código ya desplegado (Fase 1 y Fase 2 Día 1) programada para el cierre
> formal de la Skill, no antes.

---

## 1. Estructura de módulo (por bounded context)

Vertical slicing por dominio de negocio, no por capa técnica — cada microservicio
(`students-service`, `courses-service`, `exams-service`...) organiza así su interior:

```
<service>-service/
└── src/main/java/com/jopagima/school/<domain>/
    ├── domain/
    │   ├── entities/          # Entity.java — identidad + ciclo de vida
    │   ├── valueobjects/      # ValueObject.java — inmutables, definidos por atributos
    │   ├── services/          # DomainService.java — lógica que cruza varias entidades
    │   ├── errors/            # DomainError.java (ver §6 — ⚠️ conflicto con código actual)
    │   └── repositories/      # Repository.java (interfaz) + InMemoryRepository.java (mismo fichero)
    ├── application/
    │   ├── <UseCase>.java     # Orquesta el flujo de negocio, punto de entrada único (execute())
    │   ├── <Request>.java     # DTO de entrada (si el UseCase tiene >3 parámetros)
    │   ├── <Response>.java    # DTO de salida — solo tipos primitivos, sin tipos de dominio
    │   └── ports/             # Interfaces de servicios externos (notificaciones, S3...)
    ├── infrastructure/
    │   ├── adapters/          # DynamoDb<Entity>Repository.java — implementación real del puerto
    │   ├── lambda/            # <Action>Handler.java — adaptador de entrada (Lambda + API Gateway)
    │   └── factory/           # ServiceFactory.java — wiring explícito (ver §5)
    └── test/
        ├── unit/              # Domain + UseCases (con InMemoryRepository)
        ├── integration/       # Adaptadores reales (DynamoDbClient mockeado, ver §8 — ⚠️ matiz)
        └── e2e/                # curl contra el endpoint desplegado (patrón ya usado en Fase 1)
```

**Naming de fichero**: un fichero por clase/interfaz, nombre de fichero = nombre de clase
(PascalCase) — ya es la práctica del proyecto.

### Ejemplos de nombres de fichero por capa

```java
// Domain - entities/
Student.java, Course.java

// Domain - valueobjects/
Email.java, CourseCapacity.java, Id.java (si se adopta, ver §4.3)

// Domain - services/
EnrollmentEligibilityService.java  // ejemplo futuro (Fase 2, CourseEnrollment)

// Domain - repositories/
StudentRepository.java  // interfaz + InMemoryStudentRepository en el mismo fichero

// Application
RegisterStudentUseCase.java
CreateCourseUseCase.java
PhotoStoragePort.java   // puerto de servicio externo (futuro, S3)

// Infrastructure
DynamoDbStudentRepository.java   // adaptador de repositorio
RegisterStudentHandler.java      // Lambda handler (equivalente a "Controller")
StudentsServiceFactory.java      // wiring
```

---

## 2. Capas y responsabilidades

### 2.1 Domain (centro del hexágono)
- **Entities**: identidad + ciclo de vida (`Student`, `Course`). Autovalidadas — ya es la
  práctica del proyecto desde la Fase 1.
- **Value Objects**: inmutables, definidos por sus atributos. Se introducen cuando un campo
  tenga reglas de formato/invariante reutilizables entre entidades (ver §4.3).
- **Domain Services**: lógica que no pertenece a una única entidad (ver §4.4).
- **DomainError**: clase única de error con factory methods (ver §6 — ⚠️ conflicto).
- **Repositories**: interfaz del puerto + implementación `InMemory` en el mismo fichero
  (ver §4.5).
- Cero dependencias hacia `application` o `infrastructure`. Cero imports de
  `software.amazon.awssdk.*`, `com.amazonaws.*` ni ningún framework.

### 2.2 Application (casos de uso)
- **Use Cases**: orquestan la lógica de negocio — un único punto de entrada `execute(...)`,
  nunca varios métodos públicos en la misma clase (ver §4.2, "Single Entry Point").
- **Ports de servicios externos**: interfaces para lo que hoy resolvemos con SDK directo
  desde el adaptador (ej. futuro `PhotoStoragePort` para S3, `NotificationPort` para SNS).
- **DTOs de entrada/salida** (`Request`/`Response`): viven junto al UseCase que los usa
  (ver §4.1).
- Depende únicamente de Domain.
- Es la capa que **hoy no existe** en `students-service`/`courses-service` — el Lambda
  handler actúa como caso de uso mínimo. Introducirla es el cambio estructural principal
  de esta Skill.

### 2.3 Infrastructure (adaptadores)
- **Adapters**: `DynamoDbStudentRepository`, `DynamoDbCourseRepository` — SDK v2 de bajo
  nivel (decisión ya fijada, no se reabre).
- **Lambda handlers** (equivalente a "Controllers"/"Handlers" en el original): adaptador de
  entrada HTTP. Tras la Skill, invocan al UseCase, no construyen la entidad ni llaman al
  repositorio directamente (ver §7).
- **Factory**: wiring explícito (ver §5).
- Depende de Application y Domain. Es la única capa que puede importar el SDK de AWS.

## 3. Regla de dependencias

```
Infrastructure → Application → Domain
```

Las dependencias siempre apuntan hacia el centro. Domain nunca importa de Application ni de
Infrastructure. Application nunca importa de Infrastructure. Idéntico al original — es el
principio universal de hexagonal, no depende del framework.

### Comunicación entre módulos (microservicios)

**Se puede compartir** (vía módulo `commons`, ya existente): entidades de dominio y value
objects, si dos bounded contexts necesitan el mismo concepto.

**No se puede compartir**: Use Cases — un UseCase nunca invoca a otro UseCase, ni de su
propio módulo ni de otro. La comunicación entre microservicios, si hiciera falta, sigue el
ADR ya fijado: SDK-to-SDK síncrono o EventBridge/SQS/SNS asíncrono.

---

## 4. Patrones detallados por capa

### 4.1 DTOs (Application)

Viven junto al UseCase que los usa, no en un paquete `dto/` transversal:

```
students-service/application/
├── RegisterStudentUseCase.java
├── RegisterStudentRequest.java   # Request DTO (si el UseCase tiene >3 parámetros)
└── StudentDTO.java               # Response DTO
```

**Características de un DTO:**
- Objeto plano: sin métodos, sin comportamiento.
- Solo tipos primitivos: `String`, `double`, `boolean`, arrays, DTOs anidados.
- Sin tipos de dominio: nunca `Money`, `Id`, ni una entidad.
- Serializable directamente (ej. vía Jackson a JSON).

```java
// MAL — contiene tipos de dominio
public class StudentDTO {
    public Id id;          // tipo de dominio
    public Email email;    // tipo de dominio
}

// BIEN — solo primitivos
public class StudentDTO {
    public String id;
    public String email;
}
```

**Mapeo dentro del UseCase:**

```java
public class GetStudentUseCase {

    private final StudentRepository studentRepository;

    public GetStudentUseCase(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public StudentDTO execute(String studentId) {
        return studentRepository.findById(Id.create(studentId))
                .map(this::toDTO)
                .orElseThrow(() -> DomainError.createNotFound("Student " + studentId + " not found"));
    }

    private StudentDTO toDTO(Student student) {
        return student.toPrimitives();
    }
}
```

### 4.2 Use Cases (Application)

**Estructura**, con `toPrimitives()` como único contrato de serialización (ver §4.3):

```java
public class RegisterStudentUseCase {

    private final StudentRepository studentRepository;

    public RegisterStudentUseCase(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public StudentDTO execute(String id, String firstName, String lastName, String email) {
        Student student = Student.create(id, firstName, lastName, email);

        studentRepository.save(student);

        return toDTO(student);
    }

    private StudentDTO toDTO(Student student) {
        return student.toPrimitives();
    }
}
```

**Parámetros pensados para terminal (Terminal-Friendly Parameters)**: preferir parámetros
primitivos directos frente a un objeto `Request` innecesario cuando hay ≤3 parámetros.

```java
// PEOR — objeto Request complejo para pocos parámetros
public StudentDTO execute(RegisterStudentRequest request) { ... }

// MEJOR — parámetros primitivos
public StudentDTO execute(String id, String firstName, String lastName, String email) { ... }
```

```java
// TAMBIÉN BIEN — cuando hay más de 3 parámetros, sí usar Request
public class CreateCourseRequest {
    public String id;
    public String name;
    public int maxCapacity;
    public String description;
}
```

**Único punto de entrada (Single Entry Point)**: cada UseCase expone un único método
público `execute(...)`. Nunca varios métodos (`create()`, `update()`, `delete()`) en la
misma clase — eso son 3 UseCases distintos.

```java
// PEOR — múltiples puntos de entrada
class StudentUseCase {
    void register() { }
    void update() { }
    void delete() { }
}

// MEJOR — responsabilidad única
class RegisterStudentUseCase {
    public StudentDTO execute(String id, String firstName, String lastName, String email) { }
}
class DeleteStudentUseCase {
    public void execute(String id) { }
}
```

**Dependencias**: un UseCase depende de puertos (repositorios de dominio + ports de
servicios externos), nunca de un adaptador concreto:

```java
public class ProcessEnrollmentUseCase {

    private final CourseRepository courseRepository;   // Port
    private final NotificationPort notificationPort;   // Port (servicio externo)

    public ProcessEnrollmentUseCase(CourseRepository courseRepository, NotificationPort notificationPort) {
        this.courseRepository = courseRepository;
        this.notificationPort = notificationPort;
    }

    public void execute(String courseId, String studentId) {
        Course course = courseRepository.findById(Id.create(courseId))
                .orElseThrow(() -> DomainError.createNotFound("Course not found"));

        course.enroll(studentId);

        courseRepository.save(course);
        notificationPort.notifyEnrollment(studentId, courseId);
    }
}
```

### 4.3 Value Objects (Domain)

Inmutables, definidos por sus atributos, no por identidad.

**El Value Object `Id`** — ✅ **resuelto: se adopta**, según la guía original. Un único VO
genérico `Id` para todos los identificadores de entidad, en vez de `StudentId`/`CourseId`
separados — comparten el mismo comportamiento (`create`, `generate`, `equals`,
`toPrimitives`) y la seguridad de tipos cruzada la atrapa la lógica de dominio, no el
sistema de tipos. **Hoy el proyecto usa `String id` plano** en `Student`/`Course` — la
migración a `Id` se aplica en la retroactiva de la Skill (cierre de Fase 2, §14), no antes.

```java
public final class Id {

    private final String value;

    private Id(String value) {
        this.value = value;
    }

    public static Id create(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw DomainError.createValidation("Id cannot be empty");
        }
        return new Id(value);
    }

    public static Id generate() {
        return new Id(UUID.randomUUID().toString());
    }

    public boolean equals(Id other) {
        return this.value.equals(other.value);
    }

    public String toPrimitives() {
        return this.value;
    }

    public String value() {
        return value;
    }
}
```

**Estructura general** (ejemplo `Email`, candidato inmediato para Fase 2/3 si el campo se
repite entre `Student` y otras entidades futuras):

```java
public final class Email {

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email create(String value) {
        if (!value.contains("@")) {
            throw DomainError.createValidation("Invalid email format");
        }
        return new Email(value);
    }

    public boolean equals(Email other) {
        return this.value.equals(other.value);
    }

    public String toPrimitives() {
        return this.value;
    }

    public String value() {
        return value;
    }
}
```

**Inmutabilidad**: las operaciones devuelven nuevas instancias, nunca mutan la original.

```java
// PEOR — muta el value object
class Money {
    private double amount;
    public void add(Money other) { this.amount += other.amount; }
}

// MEJOR — devuelve nueva instancia
class Money {
    private final double amount;
    public Money add(Money other) { return new Money(this.amount + other.amount, currency); }
}
```

**Value Objects comunes** a considerar en fases futuras: `Id` (identificador genérico),
`Money`/`Price` (si se introduce pago en el futuro), `DateRange`/`Period` (fechas de
matrícula), `Email`/`PhoneNumber` (contacto), `Quantity`/`Percentage` (ej. `CourseCapacity`).

### 4.4 Domain Services

Lógica que no pertenece a una única entidad — funciones estáticas puras, sin estado,
agrupadas en un módulo por dominio (no una clase por función):

```java
package com.jopagima.school.courses.domain.services.enrollment;

/**
 * Domain Service: enrollment eligibility spans two aggregates (Course, CourseEnrollment),
 * so it does not belong to either entity individually.
 */
public final class EnrollmentEligibilityService {

    private EnrollmentEligibilityService() {
        // Prevent instantiation
    }

    public static boolean canEnroll(Course course, int currentEnrollmentCount) {
        return currentEnrollmentCount < course.getMaxCapacity();
    }
}
```

**Domain Service vs. Use Case**

| Domain Service | Use Case |
|---|---|
| Funciones puras | Orquesta el flujo de la aplicación |
| Sin dependencias externas | Usa puertos (repositorios, servicios externos) |
| Vive en `domain/services/` | Vive en `application/` |
| Llamado por entidades o casos de uso | Punto de entrada desde infrastructure |

### 4.5 Repositories (Domain) — interfaz + InMemory co-localizados

**`Maybe<T>`** — ✅ **resuelto: se adopta el principio (nunca `null` en el tipo de
retorno), materializado como `java.util.Optional<T>`**, el equivalente idiomático en Java
al `Maybe<T>` propio del original (`Some`/`None` ≈ `Optional.of`/`Optional.empty`,
`isSome()`/`isNone()` ≈ `isPresent()`/`isEmpty()`, `getOrThrow()` ≈ `orElseThrow()`,
`fold()`/`map()` ≈ `map()`/`orElseGet()`). No se implementa un tipo `Maybe` propio — sería
reinventar lo que el JDK ya ofrece con el mismo comportamiento estructural. **El proyecto
hoy no tiene ningún `findById` implementado todavía**, así que esta decisión se aplica
desde el primer `findById` que se escriba, sin necesidad de migración retroactiva.

```java
// src/students-service/domain/repositories/StudentRepository.java
public interface StudentRepository {

    void save(Student student);

    Optional<Student> findById(Id id);

    List<Student> findByCourse(Id courseId);
}
```

```java
// InMemory (mismo fichero) — sustituye al mock de Mockito en tests de UseCase
public class InMemoryStudentRepository implements StudentRepository {

    private final Map<String, Student> students = new HashMap<>();

    public InMemoryStudentRepository(List<Student> initialStudents) {
        for (Student student : initialStudents) {
            this.students.put(student.getId(), student);
        }
    }

    public InMemoryStudentRepository() {
        this(new ArrayList<>());
    }

    @Override
    public void save(Student student) {
        this.students.put(student.getId(), student);
    }

    @Override
    public Optional<Student> findById(Id id) {
        return Optional.ofNullable(this.students.get(id.value()));
    }

    @Override
    public List<Student> findByCourse(Id courseId) {
        // filtrado según relación (adjacency list ya fijada, Fase 2 Día 1)
        return List.of();
    }
}
```

**Características del repositorio**: lenguaje de dominio, no de base de datos (`save`,
`findById` — no `insert`, `findOne`); centrado en la entidad, nunca en primitivos/DTOs;
la interfaz no revela el mecanismo de almacenamiento (nada de `DynamoDb` en el nombre
del puerto — eso vive en el nombre del adaptador, `DynamoDbStudentRepository`).

**Cuándo usar cada implementación:**

| Contexto | Implementación |
|---|---|
| Test unitario de UseCase | `InMemory<Entity>Repository` |
| Test de contrato del adaptador | Adaptador real + `DynamoDbClient` mockeado (ver §8) |
| Verificación end-to-end | Adaptador real, desplegado |
| Producción | Adaptador real |

---

## 5. Wiring Policy — Factory (reemplaza `@Configuration`/`@Bean`)

Sin contenedor de inyección de dependencias: una clase `Factory` estática por
microservicio. El punto de entrada real del wiring es el **constructor sin argumentos
del Lambda handler** (patrón ya fijado en Fase 1, no se reabre):

```java
// infrastructure/factory/StudentsServiceFactory.java
package com.jopagima.school.students.infrastructure.factory;

import com.jopagima.school.students.application.RegisterStudentUseCase;
import com.jopagima.school.students.domain.repositories.StudentRepository;
import com.jopagima.school.students.infrastructure.adapters.DynamoDbStudentRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class StudentsServiceFactory {

    private StudentsServiceFactory() {
    }

    public static RegisterStudentUseCase createRegisterStudentUseCase() {
        return new RegisterStudentUseCase(getStudentRepository());
    }

    private static StudentRepository getStudentRepository() {
        DynamoDbClient client = DynamoDbClient.create();
        String tableName = System.getenv("TABLE_NAME");
        return new DynamoDbStudentRepository(client, tableName);
    }
}
```

```java
// infrastructure/lambda/RegisterStudentHandler.java (constructor adaptado)
public RegisterStudentHandler() {
    this(StudentsServiceFactory.createRegisterStudentUseCase());
}
```

### `get` (cacheado/singleton) vs. `create` (nueva instancia)

| `get` (cacheado) | `create` (nueva instancia) |
|---|---|
| `DynamoDbClient` | `UseCase` |
| Adaptadores de repositorio | Lambda handler (una instancia por cold start, gestionada por el runtime) |
| Clientes de servicios externos (S3, SNS) | Objetos de request/response |

### Testing con Factory

Para tests, se crean factories de test separadas que usan `InMemory<Entity>Repository`
en vez de adaptadores reales — no se testea a través de la `Factory` de producción.

---

## 6. Error Handling — `DomainError` con factory methods

✅ **Resuelto: se adopta `DomainError` según el original.** Una **única clase
`DomainError`** con métodos factory (`createNotFound`, `createValidation`, `create`),
sustituyendo la jerarquía de excepciones específicas por caso. **El código actual tiene
excepciones separadas por entidad y caso** (`InvalidStudentException`,
`StudentAlreadyExistsException`, `InvalidCourseException`, `CourseAlreadyExistsException`)
— eso es exactamente lo que el original desaconseja, y se migra en la retroactiva de la
Skill (cierre de Fase 2, §14): `InvalidStudentException` → `DomainError.createValidation`,
`StudentAlreadyExistsException` → un tipo `alreadyExists` nuevo en `ErrorType` (el
original solo define `notFound`/`validation`/`other`; este proyecto necesita un cuarto
tipo para el conflicto de duplicados que ya usamos con `ConditionExpression` — se añade
`alreadyExists` a `ErrorType`, manteniendo el espíritu de la guía con la extensión mínima
que el dominio del proyecto requiere).

```java
public class DomainError extends RuntimeException {

    private final ErrorType type;

    private DomainError(ErrorType type, String message) {
        super(message);
        this.type = type;
    }

    public static DomainError createNotFound(String message) {
        return new DomainError(ErrorType.notFound, message);
    }

    public static DomainError createValidation(String message) {
        return new DomainError(ErrorType.validation, message);
    }

    public static DomainError createAlreadyExists(String message) {
        return new DomainError(ErrorType.alreadyExists, message);
    }

    public static DomainError create(String message) {
        return new DomainError(ErrorType.other, message);
    }

    public ErrorType getType() {
        return type;
    }
}

public enum ErrorType {
    notFound, validation, alreadyExists, other
}
```

| Factory method | Tipo | Uso | HTTP status (§7) |
|---|---|---|---|
| `createNotFound()` | `notFound` | La entidad no existe | 404 |
| `createValidation()` | `validation` | Invariante violado, estado inválido | 422 |
| `createAlreadyExists()` | `alreadyExists` | Conflicto de duplicado (extensión del proyecto sobre `ConditionExpression`) | 409 |
| `create()` | `other` | Otros errores de dominio | 400 |

**Migración retroactiva (cierre de Fase 2, §14)**: `InvalidStudentException`/
`InvalidCourseException` → `DomainError.createValidation(...)`;
`StudentAlreadyExistsException`/`CourseAlreadyExistsException` →
`DomainError.createAlreadyExists(...)`. Afecta a código ya desplegado y verificado en
producción (Fase 1) — se aplica microservicio a microservicio, no de golpe, siguiendo la
regla ya fijada del proyecto.

**Mensajes de error**: incluir descripción humana + contexto relevante (IDs, valores).
Nunca incluir stack traces, detalles de implementación interna, nombres de tabla
DynamoDB, ni rutas de fichero.

```java
// BIEN
throw DomainError.createNotFound("Student " + studentId + " not found");

// MAL — expone detalles internos
throw DomainError.createNotFound("Student not found in 'StudentsTable' (DynamoDbStudentRepository.findById)");
```

**Cuándo lanzar vs. cuándo no:**
- Lanzar: invariantes violados, entidad no encontrada (cuando es requerida), transición
  de estado inválida, fallos de validación.
- No lanzar: valores opcionales (devolver `Optional.empty()`/`Maybe.none()`), resultados
  vacíos esperados (devolver lista vacía).

**Errores de dominio vs. errores técnicos:**

| Tipo | Ejemplos | Manejo |
|---|---|---|
| Dominio | no encontrado, validación fallida | Capturar y devolver el HTTP status apropiado |
| Técnico | fallo de conexión DynamoDB, timeout | Dejar burbujear, loguear, devolver HTTP 500 |

---

## 7. Lambda Handlers (equivalente a "HTTP Controllers")

**Estructura**, invocando al UseCase en vez de construir la entidad directamente:

```java
public class RegisterStudentHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final RegisterStudentUseCase useCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegisterStudentHandler() {
        this(StudentsServiceFactory.createRegisterStudentUseCase());
    }

    public RegisterStudentHandler(RegisterStudentUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            RegisterStudentRequest request = objectMapper.readValue(event.getBody(), RegisterStudentRequest.class);
            StudentDTO result = useCase.execute(request.getId(), request.getFirstName(), request.getLastName(), request.getEmail());
            return successResponse(201, result);
        } catch (Exception error) {
            return handleError(error);
        }
    }

    private APIGatewayV2HTTPResponse handleError(Exception error) {
        if (error instanceof DomainError domainError) {
            Map<String, Integer> statusMap = Map.of(
                    "notFound", 404, "validation", 422, "alreadyExists", 409, "other", 400);
            int status = statusMap.getOrDefault(domainError.getType().name(), 400);
            return errorResponse(status, domainError.getMessage());
        }
        // Error técnico: loguear y devolver 500, nunca exponer el stack trace al cliente
        return errorResponse(500, "Internal server error");
    }
}
```

**Validación de entrada**: en el handler solo se valida tipo/nulidad; las reglas de
negocio se validan en el dominio (el UseCase las delega a la entidad/value object).

```java
// MAL — duplica la validación de dominio en el handler
if (maxCapacity <= 0) {
    return badRequest("maxCapacity must be positive");
}

// BIEN — deja que Course.create()/CourseCapacity.create() lo valide;
// el UseCase propagará el DomainError.createValidation() correspondiente
```

**Formato de respuesta:**
- Éxito: devolver el recurso directamente (`200`/`201`), o sin cuerpo en `204`.
- Error: `{"error": "mensaje"}`, con el código HTTP mapeado desde el tipo de `DomainError`.

---

## 8. Testing Standards

### 8.1 Principios FIRST

- **Fast**: los tests deben ejecutarse rápido. Tests lentos rompen el ciclo de feedback.
- **Isolated**: cada test es independiente. Sin estado compartido, sin dependencia de
  orden de ejecución.
- **Repeatable**: mismo resultado siempre, en cualquier entorno.
- **Self-validating**: resultado claro de pass/fail. Sin inspección manual.
- **Timely**: escrito en el momento correcto (antes del código, en TDD).

### 8.2 Pirámide de tests

```
        /\
       /  \  E2E (pocos) — flujos HTTP completos, solo rutas críticas
      /----\
     /      \ Integration (algunos) — adaptadores de repositorio, servicios externos
    /--------\
   /          \
  /------------\ Unit (muchos) — entidades, VOs, servicios de dominio, UseCases con InMemory
```

### 8.3 Naming de tests — ✅ resuelto a favor de la guía

Nombres en inglés, que representen **reglas de negocio**, no detalles de implementación;
evitar verbos técnicos (`returns`, `calls`, `throws`, y en adelante también `should`) y
usar lenguaje de dominio (`calculates`, `validates`, `allows`, `considers`).

**El código del proyecto desde la Fase 1 usa el prefijo `should`**:
`shouldRejectBlankFirstName`, `shouldCreateOnDemandDynamoDbTableWithCompositeKey`... — se
sustituye por el estilo de dominio a partir de la retroactiva de la Skill (§14).

```java
// Estilo anterior del proyecto (should + técnico) — se retira
void shouldRejectZeroCapacity() { ... }

// Estilo adoptado (lenguaje de dominio)
void doesNotAllowZeroCapacity() { ... }
// o, siguiendo el ejemplo del original:
void appliesDiscountForOrdersAboveThreshold() { ... }
```

**Aplicación**: los tests **nuevos**, a partir de hoy, adoptan el estilo de dominio sin
prefijo `should`. Los tests **ya escritos** (Fase 1 y Fase 2 Día 1) se renombran en la
migración retroactiva de la Skill, microservicio a microservicio (§14) — no se tocan
antes, para no reabrir código ya verificado en producción sin ese trabajo planificado.

### 8.4 Estructura AAA (Arrange-Act-Assert)

Separar visualmente las tres secciones con línea en blanco:

```java
@Test
void calculatesPriceWithDiscountAppliedToGivenProduct() {

    // Arrange
    double originalPrice = 100;
    double discountPercentage = 10;

    // Act
    double finalPrice = calculateDiscountedPrice(originalPrice, discountPercentage);

    // Assert
    assertEquals(90, finalPrice);
}
```

### 8.5 Política de mocks

- Usar `InMemoryRepositories` para tests de UseCase — **nunca mocks de Mockito sobre
  repositorios** de dominio.
- Stubs/spies permitidos en puertos de servicios externos (ej. futuro `NotificationPort`).
- **Nota específica de este proyecto**: los tests de contrato del adaptador
  (`DynamoDbStudentRepositoryTest`, `DynamoDbCourseRepositoryTest`) **sí mockean
  `DynamoDbClient` con Mockito** — esto no contradice la política, porque esos tests
  **no son tests de UseCase**, son tests de integración/contrato del propio adaptador
  (categoría "Repository Adapters" en la tabla de abajo), donde mockear el cliente SDK
  es el patrón correcto y ya validado en Fase 1/2.

### 8.6 Estrategia de test por capa

| Capa | Tipo de test | Dependencias | Sufijo de fichero (adaptado a Java/Maven Surefire) |
|---|---|---|---|
| Domain (entidades, VOs) | Unit | Ninguna | `*Test.java` |
| Domain Services | Unit | Ninguna | `*Test.java` |
| UseCases | Unit | `InMemoryRepositories` | `*Test.java` |
| Repository Adapters | Integration (contrato, `DynamoDbClient` mockeado) | Mockito sobre SDK | `*Test.java` (ya así en el proyecto — ver nota §8.5) |
| Lambda Handlers | Integration/E2E | UseCase real o mock | `*Test.java` (verde en local) + `curl` en producción |

### 8.7 Tests de integración (adaptadores)

Adaptado del patrón `Testcontainers` del original a nuestro stack: dado que DynamoDB no
se testea con un contenedor real en este proyecto (se mockea `DynamoDbClient`, decisión
ya fijada en Fase 1), no se usa `MongoDBContainer` ni equivalente — el "test de
integración" del proyecto es el test de contrato con Mockito, ya cubierto en §8.6.
**DynamoDB Local** queda anotado en la memoria de progreso como alternativa disponible si
en el futuro se quiere una integración más real sin coste — no implementada todavía.

### 8.8 Reglas no negociables de testing

- Nunca borrar un test existente; arreglar la implementación en su lugar.
- Nunca modificar un test para que pase la implementación.
- Nunca mockear repositorios en tests de UseCase (usar InMemory).
- Nunca usar mocks sin justificar el motivo primero.
- Siempre estructura AAA con líneas en blanco entre secciones.
- Siempre usar `InMemoryRepositories` para tests unitarios de UseCase.
- Siempre arreglar la implementación cuando un test falla, nunca el test.
- Siempre usar el test runner configurado del proyecto (Maven/Surefire), nunca bypassearlo.

---

## 9. Design Principles — Naming, Funciones, Clases

### 9.1 Naming

- Pronunciable en inglés, sin abreviaturas técnicas (salvo en lambdas de scope reducido).
- Evitar prefijos/sufijos redundantes (`I`, `Impl`, `Abstract`).
- Nombres concretos; evitar palabras cajón (`helper`, `util`, `manager`) salvo que el
  dominio lo requiera.
- Sin información de tipo en el nombre (el IDE ya lo muestra).
- Un concepto, un nombre — sin alias ni sinónimos.
- Combinar bien con la gramática: `isPaidInvoice`, `sumOfNumbersIn(expression)`.
- Preferir "not" en el nombre sobre el operador de negación.
- Distinguir sustantivos (clases, módulos) de verbos (métodos).
- Sufijos genéricos permitidos: `DTO`, `Repository`, `Factory`, `Handler`, `UseCase`,
  `Service` (adaptado: `Mapper` se mantiene si se introduce, `Controller` se sustituye
  por `Handler` en este stack).
- Constantes en `camelCase`, no `SCREAMING_SNAKE_CASE`.
- Nunca prefijo de guion bajo para miembros privados — usar `private`.
- Evitar strings mágicos, preferir enums para conjuntos fijos de valores.

```java
// PEOR
int d = 42; // days
UsrCtrl usrCtrl = new UsrCtrl();

// MEJOR
int daysUntilExpiration = 42;
UserAuthenticator userAuthenticator = new UserAuthenticator();
```

### 9.2 Funciones

- **Tamaño contenido y responsabilidad única**: cada función hace exactamente lo que su
  nombre indica. 10-15 líneas es señal de más de una responsabilidad, no una regla dura.
- **Naming impecable**: nombres de método son verbos que describen la acción con precisión.
- **Firma mínima necesaria**: aridad 0-3 parámetros ideal; agrupar en objeto si se excede.
- **Evitar parámetros de configuración**: sin flags booleanos que cambien comportamiento;
  preferir métodos específicos (`show()`/`hide()`).
- **Parámetros opcionales con moderación**: máximo uno.
- **Flujo de control simple**: guard clauses para casos límite, salida temprana.
- **Condiciones legibles**: abstraer expresiones booleanas combinadas en variables/métodos
  explicativos; priorizar condiciones afirmativas; evitar `else`.
- **Separar flujo de control y lógica de negocio**: extraer iteraciones/ramas.
- **Estilo declarativo cuando mejora legibilidad**: `map`/`filter`/`reduce` con criterio,
  no como dogma.
- **Funciones puras y transparencia referencial**: evitar efectos secundarios cuando sea
  posible.
- **CQS (Command-Query Separation)**: comandos mutan estado y devuelven `void`; queries
  devuelven valor sin mutar.
- **Sin comentarios en métodos** (ni Javadoc, ni descripción de parámetros).
- **Colecciones inmutables** por defecto.
- **Constantes cerca de su uso**: dentro del método que las usa, no como campo de clase
  lejano.
- **Usar `Optional`/`Maybe` para valores opcionales**: nunca `null` en tipos de retorno
  (ver §4.5 sobre la decisión `Maybe` vs `Optional` pendiente).

```java
// PEOR — flag booleano
public void render(boolean showDetails) {
    if (showDetails) { /* ... */ } else { /* ... */ }
}

// MEJOR — métodos específicos
public void renderWithDetails() { /* ... */ }
public void renderSummary() { /* ... */ }
```

```java
// PEOR — query que muta estado (viola CQS)
public double totalWithDiscount(double percentage) {
    this.appliedDiscount = percentage;
    return this.total * (1 - percentage / 100);
}

// MEJOR — comando y query separados
public void applyDiscount(double percentage) { this.appliedDiscount = percentage; }
public double calculateTotal() { return this.baseTotal * (1 - this.appliedDiscount / 100); }
```

### 9.3 Clases y módulos

- **Scope mínimo para máxima cohesión**: si un método solo usa una constante y nadie más
  la usa, ponla dentro del método.
- **Constructores simples**: si tienen lógica de validación al crear, mover a un factory
  method y hacer el constructor privado. Si no hay validación compleja, no crear factory
  methods innecesarios.
- **Organización de clase**: constructor(es) público(s) primero, luego constructor
  privado, luego API pública, finalmente métodos privados.
- **Encapsulación por defecto**: `private` para métodos, exponer solo lo necesario.
- **Ley de Demeter y Tell, Don't Ask**.
- **Evitar modelos anémicos**: las clases encapsulan comportamiento, salvo en fronteras
  de aplicación donde se usan DTOs.
- **Objetos completos al construirse**: sin setters, sin inicialización parcial.
- **Nunca getters/setters de Java como patrón por defecto**: preferir métodos explícitos
  (`calculateTotal()`, `uptime()`) salvo que un framework lo requiera.
- **Nunca prefijo de guion bajo** en miembros privados.
- **Nunca singletons**: si una instancia tiene estado global, gestionarla vía la Factory.
- **Composición sobre herencia**.
- **Tipos específicos de dominio**: construir clases con comportamiento.

```java
// PEOR — Ask (decidimos fuera)
if (course.instructor().department().name().equals("CS")) {
    course.applyDiscount(10);
}

// MEJOR — Tell (le decimos qué hacer)
course.applyDiscountForDepartment("CS", 10);

// PEOR — violación de Ley de Demeter (cadena larga)
String city = student.address().contact().location().city();

// MEJOR — Ley de Demeter (un solo punto)
String city = student.city();
```

```java
// PEOR — modelo anémico
class Course {
    List<Student> enrolledStudents;
    int maxCapacity;
}
// lógica fuera:
boolean isFull = course.enrolledStudents.size() >= course.maxCapacity;

// MEJOR — modelo rico
class Course {
    private final List<Student> enrolledStudents;
    private final int maxCapacity;

    public boolean isFull() {
        return enrolledStudents.size() >= maxCapacity;
    }
}
```

### 9.4 Comentarios y formato

- El código debe ser autoexplicativo; los comentarios señalan código poco claro.
- Solo comentar el POR QUÉ, nunca el QUÉ.
- Sin Javadoc, sin descripción de parámetros.
- Borrar código comentado — para eso existe el control de versiones.
- Dejar el formato al formateador (ej. Checkstyle/Spotless, si se introduce).
- Sin líneas en blanco dentro de métodos; solo entre métodos.

### 9.5 Reglas no negociables de diseño

- Nunca escribir código de producción sin nombres autoexplicativos.
- Nunca usar nombres de variable genéricos (`x`, `data`, `temp`, `info`).
- Nunca usar flags booleanos que cambien el comportamiento del método.
- Nunca exponer colecciones internas directamente.
- Nunca usar getters/setters como sustituto de comportamiento, ni prefijos de guion bajo.
- Nunca exponer stack traces ni detalles internos en mensajes de error.
- Siempre usar factory methods para `DomainError` (si se adopta, ver §6).
- Siempre seguir CQS: comandos mutan (`void`), queries devuelven (sin mutar).
- Siempre preferir composición sobre herencia.

---

## 10. Git Strategy — ✅ resuelto a favor de la guía, con un ajuste obligado por el pipeline

Se adopta Conventional Commits con el límite de 50 caracteres en la descripción, y la
disciplina de commit en cada test en verde, desde hoy en adelante.

**Ajuste no evitable sobre feature branches**: el pipeline CI/CD self-mutating (Fase 1
Día 4) tiene su etapa `Source` apuntando explícitamente a la rama `main` vía la conexión
CodeStar — es una restricción de infraestructura ya desplegada, no una preferencia de
estilo. Adoptar feature branches sin más rompería el disparo automático del pipeline en
cada commit de un feature branch (que no es `main`). Dos formas de conciliarlo, a decidir
sin bloquear el trabajo de hoy: (a) trabajar en feature branches y hacer merge a `main`
solo al cerrar cada día/hito, disparando el pipeline una vez por día en vez de por commit;
o (b) mantener commits directos a `main` como hasta ahora, adoptando igualmente
Conventional Commits de 50 caracteres, sin feature branches. Se recomienda (b) para no
alterar el comportamiento ya verificado del pipeline sin necesidad real — anotado como
nota abierta en la memoria de progreso.

### Modelo de ramas (propuesto por el original)

```
main
 └── feat/add-student-photo-upload
 └── fix/course-capacity-validation
 └── refactor/extract-enrollment-service
```

Formato: `<type>/<short-description>`, minúsculas, guiones (no guion bajo ni camelCase),
máximo 50 caracteres en la descripción.

| Tipo | Uso |
|---|---|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de bug |
| `refactor` | Reestructuración sin cambio de comportamiento |
| `chore` | Tooling, configuración, dependencias |
| `docs` | Solo documentación |
| `test` | Añadir o corregir tests |

### Conventional Commits

Formato: `<type>(<scope>): <description>` — imperativo, minúsculas, sin punto final,
máximo 50 caracteres en la descripción, sin artículos ni relleno.

```
feat(students): add photo upload endpoint
fix(courses): validate capacity before enroll
test(courses): add zero capacity case
chore: bump aws-sdk to 2.26.0
```

**Nota de adaptación**: los mensajes de commit que hemos usado en el proyecto hasta
ahora (`feat: implement StudentsTableConstruct with on-demand billing and PK/SK
schema`) superan los 50 caracteres — priorizan claridad sobre brevedad estricta. No se
reescriben los commits ya hechos; se decide al activar la Skill si los commits nuevos
adoptan el límite de 50 caracteres o mantienen el estilo actual, más descriptivo.

### Disciplina de commit en TDD

Commit en cada test en verde — historial granular y reversible:

```
RED    → escribir test que falla       → sin commit
GREEN  → hacer pasar el test           → COMMIT
REFACTOR → mejorar el código           → COMMIT (si hay cambios)
```

| Fase TDD | Tipo de commit | Ejemplo |
|---|---|---|
| GREEN (primer test) | `feat` o `fix` | `feat(students): add capacity validation` |
| GREEN (caso adicional) | `test` | `test(students): add empty name case` |
| REFACTOR | `refactor` | `refactor(students): extract validator` |

### Reglas no negociables de git

- Nunca comitear en rojo (tests fallando).
- Nunca saltarse el commit tras verde — cada test que pasa tiene su commit.
- Nunca usar pasado o gerundio en la descripción del commit.
- Nunca comitear directamente a `main` **(⚠️ contradice el flujo actual del proyecto)**.
- Siempre usar formato Conventional Commits.
- Siempre modo imperativo en la descripción.

---

## 11. XP & TDD Practices

### 11.1 Rol: Navigator + Driver

- **Navigator**: piensa estratégicamente, observa el panorama general, identifica code
  smells, considera el diseño.
- **Driver**: implementa el código, escribe los tests, ejecuta el ciclo Red-Green-Refactor.

El humano es el **Tech Lead** — se consulta solo en planificación: aclaración de
requisitos, decisiones arquitectónicas cuando hay varios enfoques válidos, trade-offs
importantes que requieren decisión de negocio. Durante la implementación, se trabaja
autónomamente siguiendo las reglas ya establecidas — sin pedir confirmación de
decisiones ya cubiertas por las reglas.

### 11.2 Valores XP

1. **Comunicación**: explicar el razonamiento constantemente.
2. **Simplicidad**: buscar siempre la solución más simple que funcione (YAGNI).
3. **Feedback**: aplicar TDD estrictamente para feedback inmediato.
4. **Coraje**: identificar activamente code smells y problemas de diseño.
5. **Respeto**: valorar las ideas del Tech Lead, explicar el "por qué".

### 11.3 Ciclo TDD (5 pasos, commit en cada verde)

**0. REASON** — antes de cualquier código: aclarar requisitos, crear lista de casos como
TODO en el fichero de test, ordenar de más simple a más complejo (happy path → casos
alternativos → edge cases/excepciones), validar la lista antes de empezar.

**1. RED** — escribir el test antes que el código de producción: tomar el primer caso
(más simple), escribir el test (no compila), escribir el mínimo código para compilar,
ejecutar el test (falla por comportamiento incorrecto).

**2. GREEN** — implementar el mínimo para pasar el test, siguiendo TPP (§11.4), sin
optimizaciones prematuras.

**3. REFACTOR** — una vez el test pasa: ¿se puede simplificar? ¿hay duplicación que
eliminar (Rule of Three — esperar a verlo 3 veces antes de abstraer)? ¿los nombres son
claros? Seguir §9 durante el refactor.

**4. RE-EVALUATE** — revisar la lista de casos pendientes, confirmar que el siguiente
caso sigue siendo el paso más simple, reordenar si hace falta, marcar el caso completado,
volver al paso 1.

### 11.4 Transformation Priority Premise (TPP)

Guía para el paso GREEN: elegir la transformación más simple que hace pasar el test.

| # | Transformación | Descripción |
|---|---|---|
| 1 | `{} → nil` | De sin código a devolver null |
| 2 | `nil → constant` | De null a devolver un valor literal |
| 3 | `constant → constant+` | De un literal simple a uno más complejo |
| 4 | `constant → scalar` | De un valor literal a una variable |
| 5 | `statement → statements` | Añadir más líneas sin condicionales |
| 6 | `unconditional → if` | Introducir un condicional |
| 7 | `scalar → array` | De variable simple a colección |
| 8 | `array → container` | De colección a contenedor |
| 9 | `statement → recursion` | Introducir recursión |
| 10 | `if → while` | Convertir condicional en bucle |
| 11 | `expression → function` | Reemplazar expresión por llamada a función |
| 12 | `variable → assignment` | Mutar el valor de una variable |

**Principio**: en cada ciclo GREEN, elegir la transformación con el número más bajo que
hace pasar el test.

### 11.5 Desarrollo Inside-Out

```
Domain (lógica pura) → UseCase → Repository Adapter → HTTP (Lambda handler)
```

**Progresión por capas:**

```
1. Value Objects de dominio       → ciclo TDD por cada comportamiento
2. Entidades de dominio            → ciclo TDD por cada comportamiento
3. InMemoryRepositories            → ciclo TDD para el comportamiento del repositorio
4. Domain Services (si hace falta) → ciclo TDD para lógica de dominio compleja
5. Use Cases                       → ciclo TDD usando InMemoryRepositories
6. Adaptadores de repositorio      → tests de contrato con SDK mockeado (adaptado, ver §8.7)
7. Adaptadores de servicios externos → tests de integración con sandbox real (si aplica)
8. Wiring HTTP/Lambda handlers      → tests E2E de flujo completo (curl, ya usado en Fase 1)
```

Esto garantiza: lógica de negocio pura primero, sin decisiones prematuras de
infraestructura, testable desde el núcleo, flujo de dependencias claro.

### 11.6 Diseño simple

El código cumple diseño simple si:
1. Pasa todos los tests.
2. Expresa claramente la intención.
3. No tiene duplicación (de conocimiento) — esperar a verla 3 veces antes de abstraer.
4. Tiene el número mínimo de elementos.

### 11.7 Cuándo consultar al Tech Lead

Consultar (con análisis previo) cuando: hay decisiones de arquitectura con opciones A/B;
requisitos ambiguos; trade-offs importantes; dudas sobre tecnologías/dependencias;
validación de un diseño ya alcanzado. Formato de consulta: contexto, análisis de
opciones consideradas, pregunta específica, recomendación (si la hay).

### 11.8 Reglas no negociables de XP/TDD

- Nunca escribir código de producción sin un test antes.
- Nunca empezar sin una lista de ejemplos/casos.
- Nunca escribir más de un test a la vez.
- Nunca tener más de un test fallando.
- Nunca mockear repositorios en tests de UseCase (usar InMemory).
- Nunca empezar el desarrollo desde infraestructura (outside-in).
- Nunca usar nombres de variable genéricos.
- Nunca implementar funcionalidad "por si acaso" (YAGNI).
- Nunca optimizar prematuramente.
- Siempre empezar por Domain, luego UseCase, luego Infrastructure.
- Siempre usar `InMemoryRepositories` para tests unitarios de UseCase.
- Siempre sugerir el código más simple.
- Siempre identificar y señalar code smells.
- Siempre intentar refactorizar después de cada test en verde.

---

## 12. Convención de nombres — resumen consolidado

| Capa | Sufijos permitidos |
|---|---|
| Domain | (implícito para entidades), `ValueObject`, `DomainService`, `Repository` (interfaz), `DomainError` |
| Application | `UseCase`, `Request`, `Response`/`DTO`, `Port` |
| Infrastructure | `Repository` (implementación, prefijo `DynamoDb`), `Handler` (Lambda), `Factory`, `Client` |

---

## 13. Reglas no negociables — consolidado de arquitectura

- ❌ Nunca importar el SDK de AWS ni ningún tipo de Lambda en Domain.
- ❌ Nunca importar de Application o Infrastructure en Domain.
- ❌ Nunca importar de Infrastructure en Application.
- ❌ Nunca importar un UseCase desde otro módulo (bounded context).
- ❌ Un UseCase nunca invoca a otro UseCase.
- ❌ Nunca poner lógica de negocio en un adaptador ni en un Lambda handler.
- ❌ Nunca crear clases "God" (`StudentManager`, `CourseHelper`).
- ✅ Siempre crear el puerto (interfaz) antes que su adaptador.
- ✅ Domain siempre libre de dependencias externas.
- ✅ Validar la regla de dependencias en cada import nuevo.
- ✅ Agrupar código por bounded context (módulo Maven), no por capa técnica transversal.
- ✅ Consultar al Tech Lead antes de crear un nuevo módulo (bounded context).

---

## 14. Migración retroactiva (Alumnos y Cursos)

Al activar esta Skill formalmente (cierre de Fase 2), con los 4 conflictos ya resueltos
a favor de esta guía (§4.3, §4.5, §6, §8.3, §10):

1. Migrar `String id` → `Id` value object en `Student` y `Course` (§4.3).
2. Migrar `InvalidStudentException`/`InvalidCourseException` →
   `DomainError.createValidation(...)`; `StudentAlreadyExistsException`/
   `CourseAlreadyExistsException` → `DomainError.createAlreadyExists(...)` (§6).
3. Renombrar los tests existentes de `should*` a lenguaje de dominio (§8.3).
4. Extraer `RegisterStudentUseCase`/`CreateCourseUseCase` del código actual del handler.
5. El handler pasa a depender del UseCase, no directamente del `Repository`, y su mapeo
   de errores usa el `statusMap` de `DomainError.getType()` (§7).
6. Añadir `InMemoryStudentRepository`/`InMemoryCourseRepository` junto a la interfaz.
7. Crear `StudentsServiceFactory`/`CoursesServiceFactory`.
8. Se añaden tests de UseCase nuevos, usando el repositorio InMemory.

Este refactor se hace **microservicio a microservicio**, no todo de golpe (regla ya
fijada en las instrucciones del proyecto).
