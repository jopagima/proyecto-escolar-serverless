# Memoria de Progreso — Senior Cloud Architect Serverless

## Propósito del proyecto (fijado en la fase 0)
Sistema de gestión escolar (alumnos, cursos, asignaturas, exámenes/preguntas y respuestas
de los alumnos), replicando el dominio del curso "Microservicios con Spring Cloud y
Angular", reimplementado en clave 100% serverless AWS (Lambda, API Gateway, DynamoDB, S3,
Cognito) con Java + Maven, y frontend Angular apuntando a API Gateway.

## Fase actual
Fase: 2 (Cursos)
Día: 2 (en curso)

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
- Fase 2, Día 2 — En curso: dominio `Course` autovalidado (incluyendo el invariante de
  negocio `maxCapacity > 0`, primer invariante numérico del proyecto, no solo formato),
  puerto `CourseRepository`, adaptador `DynamoDbCourseRepository` con TDD reforzado.
  `CourseEnrollment` explícitamente fuera de alcance de este día, reservado para uno
  propio.

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
  de dominio. Decisión de Fase 2 Día 2.
- Invariantes de negocio (ej. `maxCapacity > 0`) se validan en el constructor de
  dominio, no solo en el frontend — el dominio es la única fuente de verdad de las
  reglas de negocio.
- Preguntas embebidas dentro del Item de Examen (no tabla separada).
- Jerarquía Asignatura padre/hija resuelta con GSI sobre `parentId`.
- Joins distribuidos (Alumno+Pregunta+Examen en Respuestas) resueltos con
  denormalización en escritura, sustituyendo el Feign síncrono del curso original.
- Persistencia con SDK v2 de bajo nivel (`DynamoDbClient`), no Enhanced Client.
- Escrituras con `ConditionExpression` (`attribute_not_exists(PK)`) en vez de
  leer-antes-de-escribir — verificado en producción (409 real) en Fase 1.
- DTOs de entrada propios por frontera (ej. `RegisterStudentRequest`), separados de las
  entidades de dominio.
- Integración Lambda-API Gateway de tipo proxy (no mapping templates VTL).
- Una Lambda por operación (no un router interno con varias rutas).
- Permisos IAM vía métodos `grant*` de CDK, nunca políticas manuales.
- Cognito con grupos de roles: activado desde la Fase 1 — aún no implementado, pendiente.
- Todo módulo de servicio con Lambdas incluye `maven-shade-plugin` desde su creación
  (no se espera al día de implementar la Lambda) — genera un fat/uber jar con todas las
  dependencias de ejecución, porque el runtime de AWS Lambda no tiene acceso al `~/.m2`
  local.
- Todo handler Lambda necesita un constructor público sin argumentos: el runtime de
  Lambda instancia la clase handler por reflexión y no conoce el constructor de test.
  El constructor sin argumentos construye el adaptador real (`DynamoDbClient.create()`
  + `System.getenv("TABLE_NAME")`).
- **Guía de estilo de arquitectura hexagonal**: fichero `guidelinesHexagonal.md` (base
  de conocimiento del Project). Se sigue la cronología ya fijada — se aplica
  formalmente a partir de la Skill de hexagonal dedicada, en el cierre de la Fase 2 (no
  desde hoy), retroactivamente a Alumnos y Cursos y de diseño para el resto de fases.
- Estándar de código fijo: Java, TDD y Clean Code en todo el proyecto (no se reabre).
- Código/comentarios/TODOs siempre en inglés; explicación y paso a paso siempre en
  español (no se reabre).
- Arquitectura hexagonal backend: se activa formalmente al cierre de la Fase 2,
  aplicada retroactivamente a Alumnos y Cursos, siguiendo `guidelinesHexagonal.md`.
- Arquitectura hexagonal frontend: se activa de forma incremental al inicio de la Fase 6.
- Pipeline CI/CD (CodeStar Connection GitHub + CodeBuild + CDK Pipelines
  self-mutating): operativo y verificado desde el cierre de Fase 1. Cualquier
  despliegue nuevo pasa por él, sin `cdk deploy` manual salvo cambios estructurales del
  propio pipeline.
- Control de costes: PoC formativo, criterio de sustituir (no eliminar) — DynamoDB en
  vez de Aurora Serverless v2/DocumentDB, API Gateway HTTP API en vez de REST API,
  Step Functions Express mantenido por valor de aprendizaje en el flujo de corrección
  de examen (no se reabre). CodePipeline+CodeBuild: único coste fijo del proyecto
  (~$1/mes + minutos de build), aceptado explícitamente por su valor de aprendizaje.
- TDD reforzado en código con SDK de AWS: todo adaptador SDK lleva tests de contrato
  con mocks/stubs, sin dependencia real de AWS en los tests unitarios (no se reabre).
  Un getter que solo devuelve una constante literal (sin lógica ni riesgo de Token de
  CDK sin resolver) no requiere test dedicado — criterio aplicado con razonamiento
  explícito, no por omisión.
- `cdk.json` vive en la raíz del repo (no dentro de `infra/`), invocando
  `mvn -e -q -f infra compile exec:java -Dexec.mainClass=...`.
- El buildspec del `CodeBuildStep` de síntesis usa `mvn clean install` (no `package`):
  el `-f infra` en modo aislado necesita que los módulos hermanos estén instalados en
  el repositorio Maven local/del contenedor, no solo compilados en su `target/`.
- La ruta del asset del jar de Lambda (`Code.fromAsset(...)`) se resuelve probando dos
  candidatos (relativo a la raíz del repo, relativo al módulo `infra`), porque el
  directorio de trabajo del proceso difiere según si invoca `cdk synth` (raíz, vía
  `cdk.json`) o Maven Surefire testeando el módulo `infra` (cwd=`infra/`).

## Incidentes relevantes (lecciones operacionales, no repetir)
- `git clean -xfd` sin comprobar `git status` primero borró todo el trabajo no
  comiteado del Día 4 de Fase 1 (la flag `-x` ignora el `.gitignore`). Reconstruido
  íntegramente. Lección: comitear al cerrar cada bloque de TODOs resueltos; nunca usar
  `-x` sin verificar `git status` antes.
- Bloqueo circular (deadlock) de self-mutation: un buildspec roto en `Synth` impide
  que `UpdatePipeline/SelfMutate` llegue a ejecutarse, y por tanto el buildspec nunca se
  actualiza con el fix — hay que romper el círculo con un `cdk deploy` manual del
  propio `PipelineStack` para forzar la actualización del recurso CodeBuild.
- Orden de build de Maven con `-f` (modo aislado): al invocar un submódulo con `-f`,
  Maven no ve el reactor completo y resuelve su `<parent>`/dependencias `provided`
  contra el repositorio local, no contra otros módulos hermanos recién compilados —
  de ahí la necesidad de `mvn clean install` (no `package`) antes de `cdk synth`.
- Jar delgado sin dependencias desplegado a Lambda → `NoClassDefFoundError` en
  producción (Jackson no encontrado). Fix: `maven-shade-plugin`, ahora configurado
  preventivamente en cada módulo de servicio desde su creación.
- Lambda sin constructor sin argumentos → fallo de inicialización silencioso en
  producción (`Internal Server Error` genérico). Fix: todo handler expone un
  constructor público sin argumentos que construye el adaptador real.

## Servicios AWS de la hoja de ruta original ya acoplados
- DynamoDB — Fase 1, Día 1.
- AWS Lambda — Fase 1, Día 3.
- Amazon API Gateway (HTTP API) — Fase 1, Día 3.
- AWS CodePipeline + AWS CodeBuild — Fase 1, Día 4, operativo y verificado.
- (Cognito y S3 presigned: aún no implementados, pendientes)

## Pendiente / próximo día
Cerrar Fase 2, Día 2: verificar TODOs de `Course`/`DynamoDbCourseRepository` resueltos
y tests en verde. Después, día dedicado a `CourseEnrollment` (matrícula), y al cierre
de la Fase 2, activación formal de la Skill de arquitectura hexagonal siguiendo
`guidelinesHexagonal.md`, aplicada retroactivamente a Alumnos y Cursos.

## Notas y dudas abiertas
- Evaluar en Fase 2, una vez asentado el pipeline, si se introduce X-Ray/CloudWatch EMF
  como capa transversal de observabilidad (servicio de la hoja de ruta original).
- Cognito con grupos de roles sigue pendiente de implementar — decidir si se aborda en
  esta fase o se pospone.
- Ningún recurso con coste fijo por tiempo salvo CodePipeline/CodeBuild (aceptado) —
  no aplica la salvaguarda de `cdk destroy` entre sesiones más allá de eso.

## Decisiones resueltas sobre guidelinesHexagonal-serverless.md (conflictos cerrados)
- **`Id` value object**: se adopta (sustituye `String id` en `Student`/`Course`) —
  migración retroactiva al cierre de Fase 2.
- **`Optional<T>`** (equivalente Java del `Maybe<T>` del original): se adopta desde el
  primer `findById` que se escriba — no requiere migración retroactiva porque hoy no
  existe ningún `findById` en el proyecto.
- **`DomainError` único con factory methods**: se adopta, sustituyendo
  `InvalidStudentException`/`StudentAlreadyExistsException`/`InvalidCourseException`/
  `CourseAlreadyExistsException`. Se añade un cuarto `ErrorType` (`alreadyExists` → 409)
  sobre los tres del original (`notFound`/`validation`/`other`), extensión mínima
  necesaria para cubrir el conflicto de `ConditionExpression` ya verificado en
  producción. Migración retroactiva al cierre de Fase 2.
- **Naming de tests sin prefijo `should`**: se adopta lenguaje de dominio
  (`doesNotAllowZeroCapacity` en vez de `shouldRejectZeroCapacity`) para tests nuevos
  desde ya; los tests ya escritos (Fase 1, Fase 2 Día 1) se renombran en la retroactiva
  de la Skill, no antes.
- **Git Strategy (Conventional Commits, 50 caracteres)**: se adopta desde ya para
  mensajes de commit nuevos. Feature branches: se mantiene el flujo actual de commits
  directos a `main` (recomendación (b) en la guía) para no romper el trigger del
  pipeline self-mutating de Fase 1 Día 4, que apunta explícitamente a `main` — nota
  abierta si en el futuro se prefiere adoptar feature branches con merge por hito.
