# Memoria de Progreso — Senior Cloud Architect Serverless

## Propósito del proyecto (fijado en la fase 0)
Sistema de gestión escolar (alumnos, cursos, asignaturas, exámenes/preguntas y respuestas
de los alumnos), replicando el dominio del curso "Microservicios con Spring Cloud y
Angular", reimplementado en clave 100% serverless AWS (Lambda, API Gateway, DynamoDB, S3,
Cognito) con Java + Maven, y frontend Angular apuntando a API Gateway.

## Fase actual
Fase: 2 (Cursos)
Día: 3 (cerrado) — pendiente Día 4 (cierre de Fase 2)

## Hecho hasta ahora
- Fase 0 — Diagnóstico (repo anterior vs. nivel senior), mapeo curso→AWS contrastado
  con el temario real, esqueleto de 6 fases definido, memoria de progreso creada.
- Fase 1, Día 1 — `StudentsTableConstruct` (CDK): tabla DynamoDB on-demand, clave
  compuesta PK/SK, tests de CDK `Template` assertions en verde.
- Fase 1, Día 2 — Dominio `Student` autovalidado (sin dependencias de AWS SDK), puerto
  `StudentRepository`, adaptador `DynamoDbStudentRepository` con SDK v2 de bajo nivel.
  Persistencia con `ConditionExpression` atómica para evitar duplicados.
- Fase 1, Día 3 — `RegisterStudentHandler` (Lambda, adaptador de entrada) + DTO
  `RegisterStudentRequest`. `StudentsApiConstruct` (CDK): Lambda Java 17 + HTTP API con
  ruta `POST /students`, permisos IAM de mínimo privilegio vía `grantWriteData`.
- Fase 1, Día 4 — Pipeline CI/CD self-mutating (CDK Pipelines): CodeStar Connection a
  GitHub, `PipelineStack`/`SchoolServerlessStage`/`StudentsStack`, CodeBuild con
  build/test/synth. **Desplegado y verificado en producción real**: `POST /students`
  responde 201 en la primera llamada y 409 (`ConditionExpression`) en la segunda,
  confirmado con `curl -i` contra el endpoint real de API Gateway.
  **Fase 1 completa cerrada.**
- Fase 2, Día 1 — Módulo `courses-service` creado con `maven-shade-plugin` configurado
  desde el inicio. `CoursesTableConstruct` (CDK): tabla DynamoDB con relación N:M
  Curso-Alumno modelada como adjacency list (Curso: PK=COURSE#<id>/SK=METADATA;
  Matrícula: PK=COURSE#<id>/SK=STUDENT#<id>), más GSI `StudentCoursesIndex`
  (GSI1PK/GSI1SK, proyección KEYS_ONLY). 3 tests de CDK assertions en verde.
- Fase 2, Día 2 — Dominio `Course` autovalidado (invariante `maxCapacity > 0`, primer
  invariante numérico del proyecto), puerto `CourseRepository`, adaptador
  `DynamoDbCourseRepository` con `ConditionExpression` atómica. 7 tests en verde.
  `CourseEnrollment` explícitamente fuera de alcance de ese día.
- Fase 2, Día 3 — `CourseEnrollment` (agregado de dominio independiente de `Course`,
  identidad = par courseId+studentId). Primer Domain Service del proyecto,
  `EnrollmentEligibilityService` (función pura sin estado, sin dependencias externas —
  corregido durante la sesión tras un intento inicial que violaba la regla de no
  importar Infrastructure desde Domain). `DynamoDbCourseEnrollmentRepository`: escritura
  condicional (mismo patrón que Alumnos/Cursos) + `countEnrollments()` vía Query con
  `begins_with(SK, "STUDENT#")` y `Select.COUNT` sobre el adjacency list del Día 1.
  16 tests en verde en `courses-service` en total.

## Decisiones técnicas ya tomadas (no reabrir sin motivo)
- Repo del proyecto: `proyecto-escolar-serverless`. Maven multi-módulo (`infra`,
  `commons`, `students-service`, `courses-service`), Java 17, CDK 2.260.0, AWS SDK v2
  2.25.0.
- Un microservicio por bounded context → una tabla DynamoDB por microservicio (5 tablas
  en total). PK/SK overloading como convención de todo el proyecto desde el Día 1.
- Relaciones N:M se modelan con el patrón adjacency list: la entidad principal y su
  relación viven en la misma tabla/Item collection (misma PK), no en una tabla de
  unión separada. Un GSI resuelve la consulta inversa sin escanear, con proyección
  `KEYS_ONLY` cuando no se necesitan atributos completos duplicados en el índice.
- `Course` y `CourseEnrollment` se modelan como agregados de dominio independientes
  aunque compartan tabla física — el modelo de persistencia NoSQL no dicta el modelo
  de dominio.
- Invariantes de negocio (ej. `maxCapacity > 0`) se validan en el constructor de
  dominio, no solo en el frontend.
- Domain Services son funciones estáticas puras, sin estado, sin dependencias de
  Infrastructure ni de Application — regla verificada con un caso real corregido en
  Fase 2 Día 3 (`EnrollmentEligibilityService`).
- Preguntas embebidas dentro del Item de Examen (no tabla separada).
- Jerarquía Asignatura padre/hija resuelta con GSI sobre `parentId`.
- Joins distribuidos (Alumno+Pregunta+Examen en Respuestas) resueltos con
  denormalización en escritura, sustituyendo el Feign síncrono del curso original.
- Persistencia con SDK v2 de bajo nivel (`DynamoDbClient`), no Enhanced Client.
- Escrituras con `ConditionExpression` (`attribute_not_exists(PK)`) en vez de
  leer-antes-de-escribir — verificado en producción (409 real) en Fase 1.
- Lecturas de conteo sobre adjacency list vía `Query` con `begins_with` + `Select.COUNT`
  (Fase 2 Día 3) — evita traer atributos completos cuando solo se necesita el número.
- DTOs de entrada propios por frontera, separados de las entidades de dominio.
- Integración Lambda-API Gateway de tipo proxy (no mapping templates VTL).
- Una Lambda por operación (no un router interno con varias rutas).
- Permisos IAM vía métodos `grant*` de CDK, nunca políticas manuales.
- Cognito con grupos de roles: activado desde la Fase 1 — aún no implementado, pendiente.
- Todo módulo de servicio con Lambdas incluye `maven-shade-plugin` desde su creación.
- Todo handler Lambda necesita un constructor público sin argumentos (runtime Lambda
  instancia por reflexión).
- **Guía de estilo de arquitectura hexagonal**: fichero `guidelinesHexagonal-serverless.md`
  (base de conocimiento del Project) — versión propia adaptada del original
  (`guidelinesHexagonal.md`, Spring/JPA) a este stack (CDK/Lambda/DynamoDB, sin
  framework). Cubre las 5 skills originales completas (arquitectura hexagonal, design
  principles, git strategy, testing standards, XP/TDD). Se activa formalmente al cierre
  de la Fase 2 (Día 4), aplicada retroactivamente a Alumnos y Cursos.
- **4 conflictos entre la guía y el código ya escrito, resueltos a favor de la guía**
  (decisión explícita del usuario): (1) adoptar `Id` value object en vez de `String id`;
  (2) adoptar `Optional<T>` (equivalente Java de `Maybe<T>`) para lecturas opcionales,
  sin migración retroactiva porque no existe ningún `findById` todavía; (3) adoptar
  `DomainError` único con factory methods, sustituyendo
  `InvalidStudentException`/`StudentAlreadyExistsException`/`InvalidCourseException`/
  `CourseAlreadyExistsException`/`InvalidCourseEnrollmentException`/
  `EnrollmentAlreadyExistsException` — se añade un cuarto `ErrorType` (`alreadyExists`
  → 409) sobre los tres del original; (4) adoptar naming de tests sin prefijo `should`
  (lenguaje de dominio) para tests nuevos desde ya, con renombrado retroactivo de los
  tests ya escritos en la migración de la Skill. Migraciones 1, 3 y 4 aplican
  retroactivamente al cierre de Fase 2 (Día 4), no antes.
- **Git Strategy**: Conventional Commits con descripción ≤50 caracteres, adoptado desde
  ya para commits nuevos. Feature branches: se mantiene el flujo actual de commits
  directos a `main`, porque el pipeline self-mutating de Fase 1 Día 4 tiene su etapa
  `Source` apuntando explícitamente a `main` — cambiarlo rompería el trigger automático
  por commit. Nota abierta si en el futuro se prefiere adoptar feature branches con
  merge por hito.
- Arquitectura hexagonal backend: se activa formalmente al cierre de la Fase 2 (Día 4),
  con `EnrollStudentInCourseUseCase` como primer UseCase real, aplicada
  retroactivamente a Alumnos y Cursos.
- Arquitectura hexagonal frontend: se activa de forma incremental al inicio de la Fase 6.
- Pipeline CI/CD (CodeStar Connection GitHub + CodeBuild + CDK Pipelines
  self-mutating): operativo y verificado desde el cierre de Fase 1.
- Control de costes: PoC formativo, criterio de sustituir (no eliminar) — DynamoDB en
  vez de Aurora Serverless v2/DocumentDB, API Gateway HTTP API en vez de REST API,
  Step Functions Express mantenido por valor de aprendizaje. CodePipeline+CodeBuild:
  único coste fijo del proyecto (~$1/mes + minutos de build), aceptado explícitamente.
- TDD reforzado en código con SDK de AWS: todo adaptador SDK lleva tests de contrato
  con mocks/stubs, sin dependencia real de AWS en los tests unitarios (no se reabre).
  Un getter que solo devuelve una constante literal no requiere test dedicado.
- `cdk.json` vive en la raíz del repo, invocando
  `mvn -e -q -f infra compile exec:java -Dexec.mainClass=...`.
- El buildspec del `CodeBuildStep` de síntesis usa `mvn clean install` (no `package`):
  el `-f infra` en modo aislado necesita que los módulos hermanos estén instalados en
  el repositorio Maven local/del contenedor.
- La ruta del asset del jar de Lambda (`Code.fromAsset(...)`) se resuelve probando dos
  candidatos (relativo a la raíz del repo, relativo al módulo `infra`), porque el
  directorio de trabajo del proceso difiere según el invocador.
- Entorno de desarrollo local: José ha pasado de Linux a Windows durante la Fase 2 —
  vigilar diferencias de comportamiento de rutas/`cdk.json` si reaparecen errores ya
  vistos en Linux al volver a tocar `infra`/`cdk synth`/`deploy`.

## Incidentes relevantes (lecciones operacionales, no repetir)
- `git clean -xfd` sin comprobar `git status` primero borró todo el trabajo no
  comiteado del Día 4 de Fase 1 (la flag `-x` ignora el `.gitignore`). Reconstruido
  íntegramente. Lección: comitear al cerrar cada bloque de TODOs resueltos; nunca usar
  `-x` sin verificar `git status` antes.
- Bloqueo circular (deadlock) de self-mutation: un buildspec roto en `Synth` impide
  que `UpdatePipeline/SelfMutate` llegue a ejecutarse — hay que romper el círculo con
  un `cdk deploy` manual del propio `PipelineStack`.
- Orden de build de Maven con `-f` (modo aislado): necesita `mvn clean install` (no
  `package`) antes de `cdk synth`, para que los módulos hermanos estén en `~/.m2`.
- Jar delgado sin dependencias desplegado a Lambda → `NoClassDefFoundError` en
  producción. Fix: `maven-shade-plugin`, configurado preventivamente desde la creación
  de cada módulo de servicio.
- Lambda sin constructor sin argumentos → fallo de inicialización silencioso en
  producción. Fix: todo handler expone un constructor público sin argumentos.
- Domain Service con dependencia de Infrastructure inyectada (Fase 2 Día 3): un primer
  intento de `EnrollmentEligibilityService` importaba
  `DynamoDbCourseEnrollmentRepository` directamente, violando la regla de dependencias
  y además no compilando (campo `static final` asignado desde constructor de
  instancia). Corregido a función estática pura sin estado. Lección: un Domain Service
  nunca resuelve sus propios datos — los recibe como parámetros de quien lo invoca
  (el futuro UseCase).
- Caché de compilación de Maven: una clase de test nueva puede no ejecutarse si
  Surefire reporta "Nothing to compile - all classes are up to date" sin haber
  recompilado de verdad. Verificar siempre el número total de tests ejecutados contra
  el esperado; si no cuadra, repetir con `mvn clean test`, no asumir que el build está
  realmente al día solo porque no dio error.

## Servicios AWS de la hoja de ruta original ya acoplados
- DynamoDB — Fase 1, Día 1.
- AWS Lambda — Fase 1, Día 3.
- Amazon API Gateway (HTTP API) — Fase 1, Día 3.
- AWS CodePipeline + AWS CodeBuild — Fase 1, Día 4, operativo y verificado.
- (Cognito y S3 presigned: aún no implementados, pendientes)

## Pendiente / próximo día
Fase 2, Día 4 (cierre de fase): `EnrollStudentInCourseUseCase` — primer UseCase real del
proyecto, orquestando `countEnrollments()` → `EnrollmentEligibilityService.canEnroll()` →
`enroll()`. Activación formal de la Skill de hexagonal (`guidelinesHexagonal-serverless.md`)
con migración retroactiva completa: `Id` value object, `DomainError` único, renombrado de
tests sin `should`, extracción de UseCases de los handlers de Alumnos y Cursos,
`InMemoryStudentRepository`/`InMemoryCourseRepository`, `StudentsServiceFactory`/
`CoursesServiceFactory`.

## Notas y dudas abiertas
- Race condition entre `countEnrollments()` y `enroll()` en `CourseEnrollment` (Fase 2
  Día 3): aceptada conscientemente para el volumen de tráfico de este PoC. Solución
  correcta identificada para el futuro: `TransactWriteItems` con contador atómico en el
  Item de Curso, si el proyecto evolucionara hacia tráfico concurrente real.
- Evaluar en Fase 2/3 si se introduce X-Ray/CloudWatch EMF como capa transversal de
  observabilidad (servicio de la hoja de ruta original).
- Cognito con grupos de roles sigue pendiente de implementar.
- Ningún recurso con coste fijo por tiempo salvo CodePipeline/CodeBuild (aceptado) —
  no aplica la salvaguarda de `cdk destroy` entre sesiones más allá de eso.
