# Memoria de Progreso — Senior Cloud Architect Serverless

## Propósito del proyecto (fijado en la fase 0)
Sistema de gestión escolar (alumnos, cursos, asignaturas, exámenes/preguntas y respuestas
de los alumnos), replicando el dominio del curso "Microservicios con Spring Cloud y
Angular", reimplementado en clave 100% serverless AWS (Lambda, API Gateway, DynamoDB, S3,
Cognito) con Java + Maven, y frontend Angular apuntando a API Gateway.

## Fase actual
Fase: 2 (Cursos)
Día: 1 (cerrado) — pendiente Día 2

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
  desde el inicio (lección de Fase 1 aplicada preventivamente, no esperada al día de la
  Lambda). `CoursesTableConstruct` (CDK): tabla DynamoDB con relación N:M Curso-Alumno
  modelada como adjacency list (Curso: PK=COURSE#<id>/SK=METADATA; Matrícula:
  PK=COURSE#<id>/SK=STUDENT#<id>), más GSI `StudentCoursesIndex` (GSI1PK/GSI1SK,
  proyección KEYS_ONLY) para resolver la consulta inversa "cursos de un alumno" sin
  escanear la tabla. 3 tests de CDK assertions en verde (billing, key schema, GSI,
  nombre de tabla).

## Decisiones técnicas ya tomadas (no reabrir sin motivo)
- Repo del proyecto: `proyecto-escolar-serverless`. Maven multi-módulo (`infra`,
  `commons`, `students-service`, `courses-service`), Java 17, CDK 2.260.0, AWS SDK v2
  2.25.0.
- Un microservicio por bounded context → una tabla DynamoDB por microservicio (5 tablas
  en total). PK/SK overloading como convención de todo el proyecto desde el Día 1.
- **Relaciones N:M se modelan con el patrón adjacency list**: la entidad principal y su
  relación viven en la misma tabla/Item collection (misma PK), no en una tabla de
  unión separada — ej. Curso (PK=COURSE#<id>/SK=METADATA) y Matrícula
  (PK=COURSE#<id>/SK=STUDENT#<id>). Un GSI resuelve la consulta inversa sin escanear
  (ej. `StudentCoursesIndex` para "cursos de un alumno"), con proyección `KEYS_ONLY`
  cuando no se necesitan atributos completos duplicados en el índice — decisión de
  Fase 2 Día 1, patrón a repetir en futuras relaciones N:M del proyecto.
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
- **Todo módulo de servicio con Lambdas incluye `maven-shade-plugin` desde su creación**
  (no se espera al día de implementar la Lambda) — genera un fat/uber jar con todas las
  dependencias de ejecución, porque el runtime de AWS Lambda no tiene acceso al `~/.m2`
  local. Aplicado ya en `students-service` y `courses-service`.
- **Todo handler Lambda necesita un constructor público sin argumentos**: el runtime de
  Lambda instancia la clase handler por reflexión y no conoce el constructor de test
  (el que recibe el repositorio para Mockito). El constructor sin argumentos construye
  el adaptador real (`DynamoDbClient.create()` + `System.getenv("TABLE_NAME")`).
- Estándar de código fijo: Java, TDD y Clean Code en todo el proyecto (no se reabre).
- Código/comentarios/TODOs siempre en inglés; explicación y paso a paso siempre en
  español (no se reabre).
- Arquitectura hexagonal backend: se activa formalmente al cierre de la Fase 2,
  aplicada retroactivamente a Alumnos y Cursos.
- Arquitectura hexagonal frontend: se activa de forma incremental al inicio de la Fase 6.
- Pipeline CI/CD (CodeStar Connection GitHub + CodeBuild + CDK Pipelines
  self-mutating): **operativo y verificado**, desde el cierre de Fase 1. Cualquier
  despliegue nuevo pasa por él, sin `cdk deploy` manual salvo cambios estructurales del
  propio pipeline.
- Control de costes: PoC formativo, criterio de sustituir (no eliminar) — DynamoDB en
  vez de Aurora Serverless v2/DocumentDB, API Gateway HTTP API en vez de REST API,
  Step Functions Express mantenido por valor de aprendizaje en el flujo de corrección
  de examen (no se reabre). CodePipeline+CodeBuild: único coste fijo del proyecto
  (~$1/mes + minutos de build), aceptado explícitamente por su valor de aprendizaje.
- TDD reforzado en código con SDK de AWS: todo adaptador SDK lleva tests de contrato
  con mocks/stubs, sin dependencia real de AWS en los tests unitarios (no se reabre).
  Matiz de Fase 2 Día 1: un getter que solo devuelve una constante literal (sin lógica
  ni riesgo de Token de CDK sin resolver) no requiere test dedicado — criterio aplicado
  con razonamiento explícito, no por omisión.
- `cdk.json` vive en la raíz del repo (no dentro de `infra/`), invocando
  `mvn -e -q -f infra compile exec:java -Dexec.mainClass=...`.
- El buildspec del `CodeBuildStep` de síntesis usa `mvn clean install` (no `package`):
  el `-f infra` en modo aislado necesita que los módulos hermanos (`students-service`,
  `courses-service`, etc.) estén instalados en el repositorio Maven local/del
  contenedor, no solo compilados en su `target/`.
- La ruta del asset del jar de Lambda (`Code.fromAsset(...)`) se resuelve probando dos
  candidatos (relativo a la raíz del repo, relativo al módulo `infra`), porque el
  directorio de trabajo del proceso difiere según si invoca `cdk synth` (raíz, vía
  `cdk.json`) o Maven Surefire testeando el módulo `infra` (cwd=`infra/`).

## Incidentes relevantes (lecciones operacionales, no repetir)
- **`git clean -xfd` sin comprobar `git status` primero** borró todo el trabajo no
  comiteado del Día 4 de Fase 1 (la flag `-x` ignora el `.gitignore`). Reconstruido
  íntegramente. Lección: comitear al cerrar cada bloque de TODOs resueltos; nunca usar
  `-x` sin verificar `git status` antes.
- **Bloqueo circular (deadlock) de self-mutation**: un buildspec roto en `Synth` impide
  que `UpdatePipeline/SelfMutate` llegue a ejecutarse, y por tanto el buildspec nunca se
  actualiza con el fix — hay que romper el círculo con un `cdk deploy` manual del
  propio `PipelineStack` para forzar la actualización del recurso CodeBuild.
- **Orden de build de Maven con `-f` (modo aislado)**: al invocar un submódulo con `-f`,
  Maven no ve el reactor completo y resuelve su `<parent>`/dependencias `provided`
  contra el repositorio local, no contra otros módulos hermanos recién compilados —
  de ahí la necesidad de `mvn clean install` (no `package`) antes de `cdk synth`.
- **Jar delgado sin dependencias desplegado a Lambda** → `NoClassDefFoundError` en
  producción (Jackson no encontrado). Fix: `maven-shade-plugin`, ahora configurado
  preventivamente en cada módulo de servicio desde su creación.
- **Lambda sin constructor sin argumentos** → fallo de inicialización silencioso en
  producción (`Internal Server Error` genérico). Fix: todo handler expone un
  constructor público sin argumentos que construye el adaptador real.

## Servicios AWS de la hoja de ruta original ya acoplados
- DynamoDB — Fase 1, Día 1.
- AWS Lambda — Fase 1, Día 3.
- Amazon API Gateway (HTTP API) — Fase 1, Día 3.
- AWS CodePipeline + AWS CodeBuild — Fase 1, Día 4, operativo y verificado.
- (Cognito y S3 presigned: aún no implementados, pendientes)

## Pendiente / próximo día
Fase 2, Día 2: dominio `Course` y `CourseEnrollment` autovalidados (sin dependencias de
AWS SDK), puerto de repositorio, adaptador `DynamoDbCourseRepository` con TDD reforzado
(tests de contrato con mocks del `DynamoDbClient`), siguiendo el mismo patrón que
`Student`/`DynamoDbStudentRepository` de la Fase 1.

## Notas y dudas abiertas
- Evaluar en Fase 2, una vez asentado el pipeline, si se introduce X-Ray/CloudWatch EMF
  como capa transversal de observabilidad (servicio de la hoja de ruta original).
- Cognito con grupos de roles sigue pendiente de implementar — decidir si se aborda en
  esta fase o se pospone.
- Ningún recurso con coste fijo por tiempo salvo CodePipeline/CodeBuild (aceptado) —
  no aplica la salvaguarda de `cdk destroy` entre sesiones más allá de eso.
