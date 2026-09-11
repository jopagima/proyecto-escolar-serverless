# Memoria de Progreso — Senior Cloud Architect Serverless

## Propósito del proyecto (fijado en la fase 0)
Sistema de gestión escolar (alumnos, cursos, asignaturas, exámenes/preguntas y respuestas
de los alumnos), replicando el dominio del curso "Microservicios con Spring Cloud y
Angular", reimplementado en clave 100% serverless AWS (Lambda, API Gateway, DynamoDB, S3,
Cognito) con Java + Maven, y frontend Angular apuntando a API Gateway.

## Fase actual
Fase: 1 (Alumnos)
Día: 3 (cerrado, verificado con `mvn clean package` completo — 12 tests en verde en todo el reactor) — pendiente Día 4

## Hecho hasta ahora
- Fase 0 — Diagnóstico (repo anterior vs. nivel senior), mapeo curso→AWS contrastado
  con el temario real, esqueleto de 6 fases definido, memoria de progreso creada.
- Fase 1, Día 1 — `StudentsTableConstruct` (CDK): tabla DynamoDB on-demand, clave
  compuesta PK/SK, tests de CDK `Template` assertions en verde.
- Fase 1, Día 2 — Dominio `Student` autovalidado (sin dependencias de AWS SDK), puerto
  `StudentRepository`, adaptador `DynamoDbStudentRepository` con SDK v2 de bajo nivel.
  Persistencia con `ConditionExpression` atómica para evitar duplicados.
- Fase 1, Día 3 — `RegisterStudentHandler` (Lambda, adaptador de entrada) + DTO
  `RegisterStudentRequest` propio de la frontera HTTP. `StudentsApiConstruct` (CDK):
  Lambda Java 17 + HTTP API con ruta `POST /students`, permisos IAM de mínimo
  privilegio vía `grantWriteData`. Primer flujo end-to-end desplegable de la Fase 1
  (HTTP → Lambda → DynamoDB) completado.

## Decisiones técnicas ya tomadas (no reabrir sin motivo)
- Repo del proyecto: `proyecto-escolar-serverless`. Maven multi-módulo (`infra`,
  `commons`, `students-service`), Java 17, CDK 2.260.0, AWS SDK v2 2.25.0. Punto de
  partida validado en Fase 0; dependencias reales añadidas en Fase 1 Día 1.
- Un microservicio por bounded context → una tabla DynamoDB por microservicio (5 tablas
  en total). PK/SK overloading como convención de todo el proyecto desde el Día 1,
  aunque el dominio Alumnos hoy solo tenga un tipo de ítem (`SK = "METADATA"`).
- Preguntas embebidas dentro del Item de Examen (no tabla separada) — refleja la
  relación bidireccional del curso original.
- Jerarquía Asignatura padre/hija resuelta con GSI sobre `parentId`.
- Joins distribuidos (Alumno+Pregunta+Examen en Respuestas) resueltos con
  denormalización en escritura, sustituyendo el Feign síncrono del curso original.
- Persistencia con SDK v2 de bajo nivel (`DynamoDbClient`), no Enhanced Client, para no
  acoplar las entidades de dominio a anotaciones del SDK de AWS — decisión de Fase 1
  Día 2, aplicable al resto de dominios.
- Escrituras con `ConditionExpression` (`attribute_not_exists(PK)`) en vez de
  leer-antes-de-escribir, para evitar race conditions — patrón a repetir en el resto
  de repositorios.
- DTOs de entrada propios por frontera (ej. `RegisterStudentRequest`), separados de las
  entidades de dominio — decisión de Fase 1 Día 3: el dominio nunca conoce JSON/HTTP.
- Integración Lambda-API Gateway de tipo proxy (no mapping templates VTL): toda la
  traducción HTTP↔dominio vive en Java testable, no en configuración VTL sin tests.
- Una Lambda por operación (no un router interno con varias rutas): cada operación
  ajusta memoria/timeout de forma independiente; revisable si el coste de cold starts
  se vuelve un problema real (no esperado a este volumen de PoC).
- Permisos IAM vía métodos `grant*` de CDK (ej. `grantWriteData`), nunca políticas
  manuales, aplicando mínimo privilegio real (no `grantReadWriteData` "por si acaso").
- Cognito con grupos de roles: activado desde la Fase 1 (no retroactivo en Fase 6) —
  aún no implementado, pendiente en próximos días de esta fase.
- Estándar de código fijo: Java, TDD y Clean Code en todo el proyecto (no se reabre).
- Código/comentarios/TODOs siempre en inglés; explicación y paso a paso siempre en
  español (no se reabre).
- Arquitectura hexagonal backend: se activa formalmente al cierre de la Fase 2,
  aplicada retroactivamente a Alumnos y Cursos. Desde Fase 1 se siembra la separación
  de paquetes (`domain`/`infrastructure` dentro del mismo módulo Maven) para que el
  refactor sea mecánico. Pendiente de decidir en esa Skill si se separa además en
  submódulos Maven distintos (`students-domain`/`students-infrastructure`) para que la
  regla de dependencia sea un error de compilación, no solo convención.
- Arquitectura hexagonal frontend: se activa de forma incremental al inicio de la Fase 6.
- Pipeline CI/CD (Git + CodeBuild + CodePipeline/CDK Pipelines): aún no preparado —
  previsto para la Fase 1, Día 4, ya que el primer microservicio desplegable (Alumnos)
  ya existe desde el cierre del Día 3.
- Control de costes: PoC formativo, criterio de sustituir (no eliminar) — DynamoDB en
  vez de Aurora Serverless v2/DocumentDB, API Gateway HTTP API en vez de REST API,
  Step Functions Express mantenido por valor de aprendizaje en el flujo de corrección
  de examen (no se reabre).
- TDD reforzado en código con SDK de AWS: todo adaptador SDK lleva tests de contrato
  con mocks/stubs, sin dependencia real de AWS en los tests unitarios (no se reabre).

## Servicios AWS de la hoja de ruta original ya acoplados
- DynamoDB — Fase 1, Día 1 — primera tabla real del proyecto (Alumnos).
- AWS Lambda — Fase 1, Día 3 — `RegisterStudentHandler`, adaptador de entrada HTTP.
- Amazon API Gateway (HTTP API) — Fase 1, Día 3 — ruta `POST /students`, integración
  proxy con Lambda.
- (Cognito y S3 presigned: planificados para próximos días de Fase 1, aún no
  implementados; CodeBuild/CodePipeline previsto para el Día 4)

## Pendiente / próximo día
Fase 1, Día 4: preparación del pipeline CI/CD (repo Git → CodeBuild build/test →
despliegue vía CDK), apoyándose en el primer microservicio ya desplegable (Alumnos).
A partir de este día, cualquier despliegue nuevo debe pasar por el pipeline, no por
pasos manuales.

## Notas y dudas abiertas
- Evaluar en Fase 2, una vez exista el pipeline, si se introduce X-Ray/CloudWatch EMF
  como capa transversal de observabilidad (servicio de la hoja de ruta original).
- Ningún recurso con coste fijo por tiempo introducido hasta ahora — no aplica todavía
  la salvaguarda de `cdk destroy` entre sesiones.
