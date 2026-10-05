# Memoria de Progreso — Senior Cloud Architect Serverless

## Propósito del proyecto (fijado en la fase 0)
Sistema de gestión escolar (alumnos, cursos, asignaturas, exámenes/preguntas y respuestas
de los alumnos), replicando el dominio del curso "Microservicios con Spring Cloud y
Angular", reimplementado en clave 100% serverless AWS (Lambda, API Gateway, DynamoDB, S3,
Cognito) con Java + Maven, y frontend Angular apuntando a API Gateway.

## Fase actual
Fase: 2 (Cursos)
Día: 7 (en curso — S3 presigned: dominio y handler casi cerrados, cableado CDK pospuesto)

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
- Fase 2, Día 4 (en curso) — `Id` (UUID real, en `commons`) adoptado en `Course`, con
  una variante propia: `Course.create(name, maxCapacity)` es un factory method que
  **genera el `Id` internamente** (constructor privado). Confirmado como alineado con
  `guidelinesHexagonal-serverless.md` §9.3 ("si hay lógica de validación al crear,
  mover a un factory method y hacer el constructor privado"). **Bloqueo de
  reconstitución desde persistencia resuelto**: se añade `Course.reconstitute(Id id,
  String name, int maxCapacity)`, segundo factory público sin revalidación (los datos
  vienen de la propia tabla DynamoDB, ya de confianza; la invariante ya se garantizó en
  el `create()` original) — nombre decidido tras valorar alternativas
  (`fromPersistence`, `restore`, `of`), José mantiene `reconstitute` (término DDD
  estándar, Vaughn Vernon). `InvalidCourseException`/`InvalidCourseEnrollmentException`
  se mantienen por ahora (migración a `ValidationError`/`DomainError` pospuesta
  explícitamente por José, sin fecha fija todavía — se aplica igual al
  `EnrollStudentInCourseUseCase` de hoy). `CourseEnrollment` y sus repositorios
  (`CourseEnrollmentRepository`/`InMemoryCourseEnrollmentRepository`/
  `DynamoDbCourseEnrollmentRepository`) migrados a `Id`. `findById` en
  `DynamoDbCourseRepository` usando `Course.reconstitute(...)`, con sus 2 tests
  (`findsCourseByIdWhenItExists`/`returnsEmptyWhenCourseDoesNotExist`) en verde.
  **`EnrollStudentInCourseUseCase` completo** (TODOs 10-13): orquesta
  `courseRepository.findById(courseId)` → `countEnrollments(courseId)` →
  `EnrollmentEligibilityService.canEnroll(...)` →
  `courseEnrollmentRepository.enroll(...)`, con sus 3 tests en verde
  (`enrollsStudentWhenCourseHasCapacity`, `doesNotAllowEnrollmentWhenCourseDoesNotExist`,
  `doesNotAllowEnrollmentWhenCourseIsAtFullCapacity`), todos declarando las
  dependencias por el tipo de puerto (`CourseRepository`/`CourseEnrollmentRepository`),
  nunca por la implementación `InMemory` concreta. **`CoursesServiceFactory`
  pospuesta**: sin ningún Lambda handler de Cursos todavía que la consuma, no se crea
  infraestructura sin consumidor real (misma regla de "microservicio a microservicio,
  no todo de golpe") — se implementará el día que se construya el endpoint HTTP de
  matrícula. `commons` compila con 5 tests en verde (`IdTest`). `courses-service`:
  21 tests en verde, `BUILD SUCCESS` en reactor completo.
- Fase 2, Día 5 — Definida la Skill `hexagonal-retrofit`: spec en
  `openspec/specs/hexagonal-retrofit.md` y Skill de Claude Code en
  `.claude/skills/hexagonal-retrofit/SKILL.md` (commiteadas: `docs: add
  hexagonal-retrofit OpenSpec and skill definition`). La CLI `openspec` no está
  instalada y se decide no instalarla (la spec funciona como documentación versionada,
  sin herramienta extra). Claude Code aplicó la Skill a `courses-service` en 11 pasos
  con `mvn clean install` en verde tras cada uno, sin tocar `students-service`,
  `commons` ni `infra`. Resultado: `Course`/`CourseEnrollment` en `domain/entities/`,
  puertos e InMemory en `domain/repositories/`, adaptadores en
  `infrastructure/adapters/`, `CourseCapacity` como Value Object en
  `domain/valueobjects/`, las 4 excepciones específicas sustituidas por
  `ValidationError`/`DomainError`, tests renombrados sin `should`. 21 tests en verde
  antes y después. **Desviación consciente de la regla de un commit por paso**: José
  decide comitear el retrofit como un único commit (`refactor(courses): apply
  hexagonal retrofit`) en vez de aplicar 11 parches uno a uno — los cambios ya estaban
  en el working tree y reconstruir el historial granular no le aportaba nada en un
  refactor mecánico sin Lambda desplegada. Para `students-service` (Lambda en
  producción) se mantiene el criterio de historial granular, pidiendo al agente que
  comitee él mismo cada paso.
  **Bug encontrado por el informe del agente**: `DynamoDbCourseRepository.save()` no
  escribía el atributo `id` que `findById()` lee (habría dado `NullPointerException`
  contra la tabla real). El test `findsCourseByIdWhenItExists` lo ocultaba porque
  mockea la respuesta de DynamoDB a mano. Corregido en un commit aparte
  (`fix(courses): persist id attribute in Course item`), con aserción nueva en
  `savesCourseWithCompositeKeyAndConditionExpression`. 21 tests en verde.
- Fase 2, Día 6 — **Retrofit hexagonal de `students-service`**, aplicado con la Skill
  `hexagonal-retrofit` por Claude Code. A diferencia del Día 5, con **un commit por
  paso y aprobación explícita de José antes de cada uno** (paso 8 de la Skill, añadido
  hoy mismo tras pedirlo José — ver decisión más abajo). 8 commits (`f4c4873`..
  `2ba81c1`), `mvn clean install` en verde tras cada uno; `students-service` pasa de
  10 a 12 tests. `infra/` no se tocó, sin `cdk deploy` del agente (verificado con
  `git diff f4c4873^ 004c6c2e -- infra/`, vacío).
  `migration_report`: `Student` → `domain/entities/`, `Id` generado en `create()`;
  `Email` extraído como Value Object; `StudentRepository` → `domain/repositories/` +
  `InMemoryStudentRepository`; `InvalidStudentException`/`StudentAlreadyExistsException`
  → `ValidationError`/`DomainError.createAlreadyExists`; `RegisterStudentUseCase`
  extraído del handler; `DynamoDbStudentRepository` (renombrado desde
  `DynamoDBStudentRepository`) → `infrastructure/adapters/`; `StudentsServiceFactory`
  nuevo; `RegisterStudentHandler` se queda en `infrastructure/` (no
  `infrastructure/lambda/`, su FQCN está hardcodeado en `StudentsApiConstruct`); tests
  renombrados sin `should`.
  **Decisión abierta del Día 5, resuelta → opción (B)**: `ValidationError` pasa de 400
  a **422** en `students-service`, sin excepción — José cambia de opinión en la propia
  sesión del Día 6 (había elegido mantener 400 primero) para ser coherente con la guía
  sin casos especiales. Duplicado sigue en 409; JSON malformado/body vacío siguen en
  400 (son errores de infraestructura, no `ValidationError`).
  **Segundo cambio de contrato, no discutido antes de hoy pero necesario**: el servidor
  genera el `id` (`Id.generateUniqueIdentifier()`); el cliente ya no lo envía
  (`RegisterStudentRequest` con `@JsonIgnoreProperties({"id"})`, lo ignora si lo
  manda); el `201` devuelve `{"id":"<uuid>"}` en el body (antes vacío) — sin esto el
  cliente no tendría forma de saber qué alumno se creó.
  **Verificado contra producción real** tras `git push` y `Deploy` en verde
  (CodePipeline, commit `004c6c2e`): `curl -i` con datos inválidos → `422
  firstName cannot be blank`; `curl -i` con datos válidos → `201
  {"id":"69a8726d-38f1-4267-a11e-a280a7a67f0d"}`. Mismo rigor de reverificación que
  cerró la Fase 1.
  **Incidente durante la verificación**: el primer intento de `curl` (local y en
  CloudShell) devolvió `403 Forbidden` con cabeceras de CloudFront — la URL usaba el ID
  de otra API REST del mismo account (`2ltxzx0x97`, `PortFolioApi`, otro proyecto), no
  el de `StudentsHttpApi` (`n3p81ul131`). No era un fallo de AWS ni del retrofit, solo
  un ID de API equivocado copiado de una sesión anterior.
  **Commits sueltos detectados en el `git log` y aclarados**: `fd4c498`/`251ea31`
  (mismo mensaje, 9s de diferencia) eran dos partes del mismo commit de docs partido
  por un `git add` incompleto (`docs/memoria-progreso.md`+`mvnj.bat` y `README.md`
  respectivamente) — sin pérdida de contenido. `da2b335` ("docs: does not needed
  anymore") borra `mvnj.bat`, comiteado por error y corregido enseguida — no toca
  `openspec/` ni `.claude/skills/`.
  **Nota de ubicación de ficheros**: `memoria-progreso.md` vive en `docs/`, no en la
  raíz (confirmado por José) — `README.md` sí en la raíz.
  **Nueva preferencia permanente, añadida en medio del Día 6**: aprobación humana
  explícita antes de cada `git commit` en cualquier trabajo delegado a un agente — ver
  entrada propia en "Decisiones técnicas".
  **Pendiente para el primer `findById` de `students-service`**: `Student.reconstitute`
  + `Email.reconstitute` (mismo patrón que `Course`), atributo `id` en el item de
  DynamoDB, y qué hacer con los alumnos ya guardados en Fase 1 con IDs no-UUID
  (`"s-001"`), que `Id.generateFromPlainTextIdentifier` rechazaría con `ValidationError`
  — dato real ya en la tabla de producción, no solo un caso hipotético.
- Fase 2, Día 7 (en curso) — **S3 presigned URL para foto de alumno**. Reordenado el
  día original "S3+Cognito" en tres: Día 7 = S3, Día 8 = Cognito, Día 9 = Observabilidad
  (confirmado por José). `StudentsPhotoBucketConstruct` (CDK, bucket privado,
  `BlockPublicAccess.BLOCK_ALL`), `PhotoStoragePort` (puerto en
  `application/ports/`), `RequestStudentPhotoUploadUseCase`, `S3PhotoStorageAdapter`
  (con `S3Presigner`, `signatureDuration` 10 min). 14 tests en verde en
  `students-service`, 7 en `infra`.
  **Bug real detectado por José, no por el test**: el `UseCase` construía la
  `objectKey` de S3 a partir del `studentId` recibido **sin validarlo como `Id`** —
  cualquier string, bien formado o no, generaba una presigned URL válida. El test de
  partida usaba `"s-abc-123"` como placeholder, que nunca sería un UUID real, y no lo
  detectó porque tampoco el propio `UseCase` validaba. Añadida regla explícita a
  `guidelinesHexagonal-serverless.md` (§8.3b nueva, y §13): toda fixture de
  identificador en tests usa `Id.generateUniqueIdentifier().toString()`, nunca un
  placeholder inventado; todo identificador recibido como `String` se convierte a `Id`
  antes de usarlo.
  **Firma de `RequestStudentPhotoUploadUseCase.execute`**: José probó primero
  `execute(Id)` (conversión y 422 en el handler, como excepción a §4.2) y, tras revisar
  el coste de dejar los UseCases no uniformes, **volvió a `execute(String)`** con la
  conversión a `Id` dentro. Los tres UseCases del proyecto reciben primitivos, sin
  excepciones. `doesNotAllowMalformedStudentId` vive en el test del UseCase; el test del
  handler solo verificará que `ValidationError` se traduce a 422.
  Estado al cierre de la sesión: **16 tests en verde en `students-service`**.
  Hecho: bucket CDK (test en verde), `PhotoStoragePort`, `RequestStudentPhotoUploadUseCase`
  (2 tests), `S3PhotoStorageAdapter`, `RequestStudentPhotoUploadHandler` con su primer
  test, constructor sin argumentos y `StudentsServiceFactory.
  createRequestStudentPhotoUploadUseCase()`. La variable de entorno del bucket es
  **`PHOTO_BUCKET_NAME`** (renombrada desde `BUCKET_NAME`); la clave en el `.environment`
  de CDK tendrá que ser exactamente esa. El path parameter se llama **`studentId`**: la
  ruta de CDK debe ser `GET /students/{studentId}/photo-upload-url`, o el handler
  recibirá `null` en producción.
  Pendiente en el handler: tests `returns400WhenPathParameterMissing` y
  `returns422WhenStudentIdIsMalformed` (uno por commit) y sustituir el JSON
  concatenado a mano por `ObjectMapper`.
  **Cableado de CDK pospuesto por José a otro día** (TODOs 6-7: Lambda, ruta, `grantPut`
  y la variable de entorno en `StudentsApiConstruct`, y cambio de firma con el `Bucket`
  en `StudentsStack`). Hasta entonces el handler está en el jar pero ninguna ruta
  apunta a él: subirlo al pipeline no expone nada. Al retomarlo, revisar el
  `git diff -- infra/` antes de `cdk deploy`/push (permisos IAM nuevos sobre el bucket),
  como en el Día 6. Observado de paso en `StudentsApiConstruct`: la Lambda de registro
  ya tiene `Tracing.ACTIVE` (X-Ray), adelantando parte del Día 9, con un comentario en
  español y una marca de cita `[6]` pegada que conviene limpiar.

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
- **4 conflictos entre la guía hexagonal y el código ya escrito, resueltos por José**:
  (1) adoptar `Id` (UUID real, no string legible) — **con impacto de contrato de API
  no trivial**: el servidor pasa a generar el ID, el cliente deja de decidirlo; se
  aplica solo a `courses-service` por ahora (Fase 2 Día 4), la migración retroactiva de
  `students-service` (Lambda ya en producción) queda para un día dedicado futuro que
  deberá incluir el cambio de contrato; (2) `Optional<T>` para lecturas opcionales,
  sin migración retroactiva pendiente (no había `findById` implementado); (3) **dos
  tipos de error único, no uno solo**: `ValidationError` (formato/invariante, siempre
  HTTP 422) y `DomainError` (`notFound`→404, `alreadyExists`→409, `other`→400) —
  decisión final de José, ajustada respecto a la propuesta inicial de un único
  `DomainError` con `ErrorType.validation`; (4) naming de tests sin prefijo `should`
  para tests nuevos, renombrado retroactivo de los ya escritos pendiente. Migración de
  `Course`/`CourseEnrollment` a `Id`+`ValidationError`/`DomainError`: en curso, parcial
  (ver Día 4 arriba) — `Course` usa un factory method (`Course.create(name,
  maxCapacity)`) que genera el `Id` internamente (constructor privado), variante propia
  de José sobre lo propuesto originalmente (constructor público recibiendo `Id`).
- **Aprobación humana por commit, en cualquier trabajo delegado a un agente**: José no
  delega la decisión de comitear — antes de cada `git commit`, el agente debe presentar
  qué cambió, un resumen breve del porqué, y el mensaje de commit propuesto, y esperar
  confirmación explícita antes de ejecutar el commit. Aplica a toda la Skill
  `hexagonal-retrofit` (añadido como paso 8 obligatorio en
  `.claude/skills/hexagonal-retrofit/SKILL.md`, Fase 2 Día 6) y, por extensión, a
  cualquier Skill futura que comitee en nombre del usuario.
- **Patrón de reconstitución de entidades desde persistencia**: cuando una entidad
  tiene un factory method público con constructor privado (ej. `Course.create(...)`,
  que valida y genera `Id`), la reconstrucción desde el adaptador de persistencia usa
  un **segundo factory público**, `reconstitute(Id, ...)`, sin revalidar (los datos ya
  son de confianza) — no un constructor público expuesto sin más. Decidido en Fase 2
  Día 4, apoyado en `guidelinesHexagonal-serverless.md` §9.3 ("Organización de clase":
  admite más de un constructor/factory público) y §9.1 (nombres autoexplicativos que
  distingan intención: `create` = nuevo, `reconstitute` = ya existente). Nombre
  evaluado frente a alternativas (`fromPersistence`, `restore`, `of`); José mantiene
  `reconstitute` (término DDD estándar, Vaughn Vernon). Patrón a repetir en cualquier
  entidad futura con factory method + Id autogenerado.
- **`CourseCapacity` (Value Object) y reorganización `domain/` en subpaquetes**:
  decisión tomada en Fase 2 Día 4, **programada para la migración retroactiva al
  cierre de Fase 2** (no aplicada hoy). La guía nombra explícitamente
  `Quantity`/`Percentage` (ej. `CourseCapacity`) como candidato de VO (§4.3) y ya
  define la estructura `domain/entities/`+`domain/valueobjects/`+`domain/services/`+
  `domain/repositories/` en §1 — hoy `Course`/`CourseEnrollment` viven planos en
  `domain/`, desviación pendiente de corregir. Alcance ampliado a `students-service`:
  `Email` (VO) para `Student`, ya anticipado por la guía como "candidato inmediato...
  si el campo se repite" (§4.3). `firstName`/`lastName` se quedan como `String` plano
  (sin regla propia más allá de no-blanco, no hay caso de VO todavía).
- **Git Strategy**: Conventional Commits con descripción ≤50 caracteres, adoptado desde
  ya para commits nuevos. **Granularidad reforzada (decisión de José, Fase 2 Día 4)**:
  un commit por cada test individual que pasa a verde, nunca agrupado por bloque/día —
  aplicación estricta de §10 de la guía (*"Commit en cada test en verde — historial
  granular y reversible"*), que hasta ahora se venía dando agrupado por feature/día.
  Feature branches: se mantiene el flujo actual de commits
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

- Un test con la respuesta de DynamoDB mockeada a mano puede pasar en verde mientras el
  código real está roto: `findById` leía un atributo `id` que `save()` nunca escribía
  (Fase 2 Día 5). Los tests de adaptador que leen deben construir su respuesta a partir
  de lo que escribe el propio `save()`, o al menos aserciones cruzadas sobre los
  atributos escritos. Lo detectó el informe de Claude Code, no la suite.
- Parches generados por un agente sobre un working tree que ya contiene sus cambios no
  se pueden aplicar con `git apply` (el contexto "antes" ya no existe). Se comprueba con
  `git apply --reverse --check`. Si el árbol ya está en el estado final, lo más simple
  es comitear ese estado en vez de reconstruir el historial.
- Un `403 Forbidden` con cabeceras de CloudFront contra una HTTP API regional (que no
  debería pasar por CloudFront) es señal de estar llamando a la URL equivocada, no de
  un fallo de permisos real — José tenía varias APIs en la cuenta (`PortFolioApi` de
  otro proyecto) y copió el ID incorrecto (Fase 2 Día 6). Verificar el ID de API contra
  la consola (**API Gateway → APIs**) antes de asumir un problema de IAM/CORS/WAF.

## Servicios AWS de la hoja de ruta original ya acoplados
- DynamoDB — Fase 1, Día 1.
- AWS Lambda — Fase 1, Día 3.
- Amazon API Gateway (HTTP API) — Fase 1, Día 3.
- AWS CodePipeline + AWS CodeBuild — Fase 1, Día 4, operativo y verificado.
- (Cognito y S3 presigned: aún no implementados, pendientes)

## Pendiente / próximo día
**Fase 2, Día 5 — cerrado** (Skill definida, `courses-service` migrado, bug del
atributo `id` corregido, mensaje duplicado limpiado).

**Resuelto, sin commit propio**: `EnrollStudentInCourseUseCase` ya llama a
`EnrollmentEligibilityService.canEnroll(...)` en vez de comparar
`currentEnrollments`/`maxCapacity` inline — el cambio quedó incluido dentro del commit
`refactor(courses): apply hexagonal retrofit` en vez de en un commit aparte (José lo
confirma tras revisar el código ya comiteado, no fue un paso deliberado de ese commit).

**Fase 2, Día 6 — cerrado.** Retrofit de `students-service` aplicado, commiteado paso a
paso con aprobación explícita, desplegado vía pipeline y verificado con `curl -i` real
(422 en validación, 201 con `id` generado por el servidor). Ver detalle completo en el
diario ("Hecho hasta ahora") y las dos decisiones que quedan ahí documentadas
(422 sin excepción; `id` en el body de respuesta).

**Cierre de las dos tareas menores, antes de abrir el Día 7:**
- **Javadoc de `CourseRepository`**: corregido — el texto ya no sugiere que las
  operaciones de matrícula son un pendiente ("not part of this port yet"), ahora explica
  que viven permanentemente en un puerto distinto (`CourseEnrollmentRepository`) por
  tratarse de un agregado independiente. Commit: `docs(courses): fix stale
  CourseRepository javadoc`.
- **`Student.reconstitute`/`Email.reconstitute`**: **pospuesto**, mismo criterio que
  `CoursesServiceFactory` (no se construye infraestructura sin consumidor real — hoy
  no hay ningún UseCase ni handler que llame a `findById` en `students-service`). Se
  implementará el día que exista ese consumidor.
- **Decisión sobre los alumnos de Fase 1 con IDs no-UUID** (`"s-001"` y similares):
  **resuelta**. Son datos de prueba generados con `curl` durante la verificación de la
  Fase 1, sin valor de negocio que preservar. Cuando se implemente `findById` en
  `students-service`, esos ítems antiguos simplemente no serán recuperables por ese
  método (`Id.generateFromPlainTextIdentifier` los rechazará con `ValidationError`) —
  se documenta como limitación conocida y aceptada, sin migración de datos. Si llegan a
  estorbar, se borran de la tabla a mano.

**Fase 2, Día 7 (en curso) — S3 presigned URL (foto de alumno)**: dominio, UseCase,
adaptador y handler con su primer test cerrados (16 tests en `students-service`).
Pendiente en `students-service`: los tests `returns400WhenPathParameterMissing` y
`returns422WhenStudentIdIsMalformed` del handler, y el `ObjectMapper`. Pendiente en
`infra/` (pospuesto por José a otro día): TODOs 6-7 de `StudentsApiConstruct` (Lambda,
ruta `GET /students/{studentId}/photo-upload-url`, `grantPut`, variable
`PHOTO_BUCKET_NAME`) y el cambio de firma con el `Bucket` en `StudentsStack`. Al
retomarlo: revisar `git diff -- infra/`, `mvn clean install`, desplegar y reverificar
con una petición real contra el endpoint, mismo rigor que el Día 6. El día no se cierra
hasta que esa petición real funcione.

**Fase 2, Día 8 — Cognito (grupos de roles)**: separado de S3 en un día propio
(reordenamiento confirmado por José). Pendiente desde la Fase 1. El curso de
certificación AWS Developer (`Developing on AWS`, módulo 12) confirma que es un bloque
de examen con peso real.

**Fase 2, Día 9 — Observabilidad (CloudWatch + X-Ray)**: módulo 14 de 15 del mismo
curso. Alcance a definir (métricas custom vía EMF, trazas X-Ray sobre API Gateway →
Lambda → DynamoDB).

**`CoursesServiceFactory`** sigue pospuesta hasta que exista el Lambda handler de
matrícula (no se crea infraestructura sin consumidor).

## Notas y dudas abiertas
- Race condition entre `countEnrollments()` y `enroll()` en `CourseEnrollment` (Fase 2
  Día 3): aceptada conscientemente para el volumen de tráfico de este PoC. Solución
  correcta identificada para el futuro: `TransactWriteItems` con contador atómico en el
  Item de Curso, si el proyecto evolucionara hacia tráfico concurrente real.
- Ningún recurso con coste fijo por tiempo salvo CodePipeline/CodeBuild (aceptado) —
  no aplica la salvaguarda de `cdk destroy` entre sesiones más allá de eso.
- Contrastado contra el material oficial de certificación AWS Developer (`Developing on
  AWS`, guía de estudiante v4.6.4): confirma compatibilidad total con el stack del
  proyecto (IAM, S3, DynamoDB con GSI, Lambda, API Gateway, Cognito, CloudWatch/X-Ray).
  Única discrepancia sin impacto: el curso usa AWS SAM como IaC de referencia en su
  capstone; este proyecto mantiene CDK (decisión ya fijada, no se reabre) — si el
  examen pregunta por sintaxis SAM específica, el código CDK no prepara para ese
  detalle, aunque los conceptos de IaC sí son transferibles.
