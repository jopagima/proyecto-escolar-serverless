# school-serverless-platform

Serverless AWS migration of a Spring Boot + Spring Cloud microservices school
management system (originally built with Eureka, Zuul/Spring Cloud Gateway, Feign,
MySQL/MongoDB and Angular). This is a hands-on learning project to reinforce an AWS
Developer certification and progress toward a Senior Cloud Architect Serverless level,
rebuilding the same domain (students, courses, subjects, exams/questions, answers) as a
100% serverless architecture with strict cost control.

> Educational proof-of-concept, not a production system. Every architectural decision
> below is made with a PoC cost profile in mind — see [Cost control](#cost-control).

## Architecture overview

One microservice per bounded context, each backed by its own DynamoDB table
(`PAY_PER_REQUEST` billing), fronted by API Gateway HTTP APIs and secured with Amazon
Cognito. No service runs 24/7; there are no provisioned/always-on resources. Deployments
are fully automated through a self-mutating CDK Pipeline — no manual `cdk deploy` to
production once the pipeline exists.

| Original course (Spring Cloud) | Serverless AWS equivalent |
|---|---|
| Eureka (service discovery) | Not needed — API Gateway routes directly to each Lambda |
| Zuul / Spring Cloud Gateway | Amazon API Gateway (HTTP API) |
| Spring Cloud Load Balancer | Not needed — Lambda scales automatically |
| Feign (sync inter-service HTTP) | SDK-to-SDK sync calls, or EventBridge/SQS/SNS async |
| MySQL / MariaDB / PostgreSQL | DynamoDB on-demand (Aurora Serverless v2 discarded: fixed base cost + VPC/NAT) |
| MongoDB (answers service) | DynamoDB on-demand (DocumentDB discarded: same reason as above) |
| Blob/MultipartFile photo upload | S3 presigned URLs, direct upload from Angular |
| JPQL joins across services | Write-time denormalization (no distributed sync joins) |
| Curso-Alumno N:M relation (join table) | DynamoDB adjacency list pattern + GSI for the inverse query |
| Angular + Angular Material SPA | Unchanged, served from S3 + CloudFront |

Full mapping, rationale and cost trade-offs are tracked in `memoria-progreso.md`.

## Tech stack

- **Language:** Java 17
- **Build:** Maven, multi-module
- **IaC:** AWS CDK 2.260.0 (Java), CDK Pipelines (self-mutating)
- **AWS SDK:** AWS SDK for Java v2, 2.25.0
- **Compute:** AWS Lambda, one function per operation, packaged as a shaded/uber jar
- **API layer:** Amazon API Gateway (HTTP API), Lambda proxy integration
- **CI/CD:** AWS CodePipeline + AWS CodeBuild, triggered on push via GitHub CodeStar Connection
- **Testing:** JUnit 5, Mockito, CDK `Template` assertions
- **Frontend (later phase):** Angular + Angular Material

## Module structure

```
school-serverless-platform/
├── infra/               # CDK constructs, stacks and the pipeline — the only module aware of CloudFormation
├── commons/              # Shared Java utilities across services
├── students-service/    # Alumnos bounded context (active, deployed)
├── courses-service/     # Cursos bounded context (active — table modeled, domain pending)
├── exams-service/       # Exámenes/Preguntas bounded context (created at Phase 3)
├── answers-service/     # Respuestas bounded context (created at Phase 4)
└── subjects-service/    # Asignaturas bounded context (created at Phase 5)
```

Each service module follows a hexagonal layout: `domain/entities` (autovalidated
entities), `domain/valueobjects` (e.g. `CourseCapacity`), `domain/repositories` (ports
+ InMemory implementations, one file each in the same package),
`domain/services` (pure Domain Services), `application` (Use Cases, single public
`execute(...)` entry point, primitive parameters), `infrastructure/adapters` (DynamoDB
repositories) and `infrastructure` (Lambda handlers). Both `courses-service` (Fase 2
Día 5) and `students-service` (Fase 2 Día 6) are migrated to this layout; see
[`openspec/specs/hexagonal-retrofit.md`](./openspec/specs/hexagonal-retrofit.md) for the
retrofit contract.

## Design conventions

- **One DynamoDB table per bounded context**, with `PK`/`SK` key overloading as a
  project-wide convention, even where a domain currently has a single item type.
- **N:M relationships use the adjacency list pattern**: the parent entity and its
  relation items share the same table/item collection (same PK) — e.g. a Course item
  (`PK=COURSE#<id>`, `SK=METADATA`) and its enrollment items
  (`PK=COURSE#<id>`, `SK=STUDENT#<studentId>`). A GSI answers the inverse query (e.g.
  "courses for a student") without a table scan, using `KEYS_ONLY` projection when full
  attribute duplication isn't needed.
- **`PAY_PER_REQUEST` billing** everywhere — no fixed/hourly-cost resource is introduced
  without an explicit justification and cost estimate.
- **Identity as a Value Object**: `Id` (backed by a real UUID, in `commons`) with two
  factory methods — `create(...)`/`generateUniqueIdentifier()` for new entities (server
  generates the id, not the client) and `reconstitute(...)` for rebuilding an entity
  already read from persistence, without revalidating.
- **Two error types, not one**: `ValidationError` (format/invariant violations, always
  HTTP 422) and `DomainError` (`notFound`/`alreadyExists`/`other`, mapped per type) —
  Replaces per-entity exception classes.
- **TDD**, reinforced for any code touching the AWS SDK: business logic is unit-tested
  against mocked SDK clients (Mockito); every adapter has a dedicated contract test.
- **Domain purity:** entities and ports never import AWS SDK, Lambda, or JSON
  serialization types.
- **One Lambda per operation**, not a shared router.
- **Least-privilege IAM** via CDK `grant*` methods, never manually authored policies.
  One documented exception: the photo-upload function carries an explicit
  `s3:PutObject`-only statement, because `grantPut` also adds retention, legal-hold and
  multipart-abort actions it does not need; a test asserts the absence of read and
  delete actions.
- **Every Lambda handler ships a public no-arg constructor** wiring the real adapter
  (e.g. `DynamoDbClient.create()`), required by the Lambda Java runtime's reflection-based
  instantiation — the test constructor (accepting a mocked port) is separate.
- **Every service module configures `maven-shade-plugin` from creation**, not only when
  its first Lambda is implemented: the Lambda runtime has no access to the local Maven
  repository, so all runtime dependencies must be bundled into the deployed artifact.
- Code, comments, and identifiers are always in English; design rationale and daily
  session material are documented in Spanish (see `memoria-progreso.md`).

## Cost control

This project substitutes AWS services only when a cheaper alternative delivers the same
functional/learning value (e.g. DynamoDB over Aurora Serverless v2/DocumentDB, API
Gateway HTTP API over REST API). Services with real but low cost are kept when they add
genuine architectural learning (e.g. Step Functions Express, Cognito, CDK Pipelines,
CodePipeline/CodeBuild — the project's only fixed-cost service, ~$1/month, accepted
explicitly for its learning value). Any new resource with non-trivial cost is called out
explicitly, with an estimate, before being introduced.

## Roadmap

| Phase | Scope | Status |
|---|---|---|
| 0 | Setup & diagnosis | ✅ Closed |
| 1 | Students (CRUD, DynamoDB, Lambda, API Gateway, CI/CD pipeline) | ✅ Closed — deployed and verified end-to-end in production |
| 2 | Courses — hexagonal architecture | 🔄 In progress (Día 7 of 9 closed — both services migrated to hexagonal; student photo upload via S3 presigned URL deployed and verified) |
| 3 | Exams / Questions | ⏳ Pending |
| 4 | Answers | ⏳ Pending |
| 5 | Subjects (parent/child hierarchy, cursor pagination) | ⏳ Pending |
| 6 | Angular frontend (hexagonal frontend activated) | ⏳ Pending |

Day-by-day progress, technical decisions and open questions are tracked in
[`memoria-progreso.md`](./memoria-progreso.md) — read it first when resuming work.

## Building and testing

```bash
# From the repository root — installs all modules to the local Maven repo,
# required for `infra`'s isolated (-f) invocation of cdk synth to resolve
# sibling service modules as provided dependencies
mvn clean install

# Run tests for a single module
mvn test -pl students-service
mvn test -pl courses-service
mvn test -pl infra
```

## Deploying

```bash
# One-time bootstrap per AWS account/region
cdk bootstrap aws://<account-id>/<region> --profile <your-profile>

# One-time manual deploy of the pipeline itself
cdk deploy SchoolServerlessPipelineStack --profile <your-profile>
```

From then on, every push to `main` triggers the pipeline automatically — no further
manual `cdk deploy` is needed for business stacks (`StudentsStack`, etc.).

## Status

**Phase 1 (Students) closed and verified in production.** `POST /students` is live on
API Gateway, backed by a Lambda (Java 17) writing to DynamoDB with an atomic conditional
write. The pipeline (Source → Synth → SelfMutate → Assets → Deploy) runs green
end-to-end.

**Phase 2 (Courses) in progress — Día 7 of 9 closed.** Both `courses-service` and
`students-service` are fully migrated to the hexagonal layout, applied via the
`hexagonal-retrofit` Claude Code Skill (`.claude/skills/hexagonal-retrofit/`) —
`courses-service` in one batched commit
(no Lambda deployed yet, low risk), `students-service` with one approved commit per
verified step (Lambda already live). Two deliberate API contract changes shipped with
the `students-service` retrofit and verified against the live endpoint: `Id` is now a
real UUID generated server-side (`RegisterStudentRequest` no longer accepts a
client-supplied `id`; the `201` response body now returns the generated
`{"id": "<uuid>"}`), and validation failures now return **422** instead of 400, with no
exception — re-verified with `curl` against production
(`422 firstName cannot be blank`, `201 {"id":"<uuid>"}`). `courses-service` has no
Lambda handler yet — no HTTP endpoint deployed for that bounded context; a first
`findById` for `students-service` still needs `Student.reconstitute`/
`Email.reconstitute` and a decision on Phase-1-era non-UUID student ids already in the
table.

**Día 7 — student photo upload (deployed and verified).** A `PhotoStoragePort` with an
S3 presigned-PUT adapter, a use case that validates the student id and builds the object
key, and `RequestStudentPhotoUploadHandler` (200 with `uploadUrl`, 400 for a missing path
parameter, 422 for a malformed id; any other error propagates as a 500). In `infra/`, a
private photos bucket (all public access blocked), a second Lambda and the route
`GET /students/{studentId}/photo-upload-url`. The Lambda's role carries a single
`s3:PutObject` statement scoped to the `students/*` prefix (the one documented exception
to `grant*`). Verified against production after the pipeline deployed it: `GET` returns
`200` with a presigned URL, a `PUT` to that URL is accepted by S3 (`200`, the object
lands under `students/<uuid>/photo`), and a malformed id returns `422`. Known
limitations: the endpoint signs a URL for any well-formed UUID, whether or not that
student exists, and JSON responses are served as `text/plain`. Next: Cognito role
groups (Día 8), then observability with CloudWatch and X-Ray (Día 9).
