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
Cognito. No service runs 24/7; there are no provisioned/always-on resources.

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
| Angular + Angular Material SPA | Unchanged, served from S3 + CloudFront |

Full mapping, rationale and cost trade-offs are tracked in `memoria-progreso.md`.

## Tech stack

- **Language:** Java 17
- **Build:** Maven, multi-module
- **IaC:** AWS CDK 2.260.0 (Java)
- **AWS SDK:** AWS SDK for Java v2, 2.25.0
- **Testing:** JUnit 5, Mockito, CDK `Template` assertions
- **Frontend (later phase):** Angular + Angular Material

## Module structure

```
school-serverless-platform/
├── infra/               # CDK constructs and stacks — the only module aware of CloudFormation
├── commons/              # Shared Java utilities across services
├── students-service/    # Alumnos bounded context (active)
├── courses-service/     # Cursos bounded context (created at Phase 2)
├── exams-service/       # Exámenes/Preguntas bounded context (created at Phase 3)
├── answers-service/     # Respuestas bounded context (created at Phase 4)
└── subjects-service/    # Asignaturas bounded context (created at Phase 5)
```

Each service module (from Phase 2 onward, applied retroactively to `students-service`)
follows a hexagonal layout: `domain` (entities, ports — zero AWS SDK dependencies),
`infrastructure` (adapters implementing those ports), and the Lambda handler as the
entry-point adapter.

## Design conventions

- **One DynamoDB table per bounded context**, with `PK`/`SK` key overloading as a
  project-wide convention, even where a domain currently has a single item type.
- **`PAY_PER_REQUEST` billing** everywhere — no fixed/hourly-cost resource is introduced
  without an explicit justification and cost estimate.
- **TDD**, reinforced for any code touching the AWS SDK: business logic is unit-tested
  against mocked SDK clients (Mockito); every adapter has a dedicated contract test
  covering edge cases (not-found, validation errors, failed conditional writes).
- **Domain purity:** entities and ports never import AWS SDK or Lambda types.
- Code, comments, and identifiers are always in English; design rationale and daily
  session material are documented in Spanish (see `memoria-progreso.md`).

## Cost control

This project substitutes AWS services only when a cheaper alternative delivers the same
functional/learning value (e.g. DynamoDB over Aurora Serverless v2/DocumentDB, API
Gateway HTTP API over REST API). Services with real but low cost are kept when they add
genuine architectural learning (e.g. Step Functions Express, Cognito, CDK Pipelines).
Any new resource with non-trivial cost is called out explicitly, with an estimate, in
the corresponding session's material before being introduced.

## Roadmap

| Phase | Scope | Status |
|---|---|---|
| 0 | Setup & diagnosis | ✅ Closed |
| 1 | Students (+ CI/CD pipeline setup at close) | 🔄 In progress |
| 2 | Courses (hexagonal backend activated retroactively at close) | ⏳ Pending |
| 3 | Exams / Questions | ⏳ Pending |
| 4 | Answers | ⏳ Pending |
| 5 | Subjects (parent/child hierarchy, cursor pagination) | ⏳ Pending |
| 6 | Angular frontend (hexagonal frontend activated) | ⏳ Pending |

Day-by-day progress, technical decisions and open questions are tracked in
[`memoria-progreso.md`](./memoria-progreso.md) — read it first when resuming work.

## Building and testing

```bash
# From the repository root
mvn clean install

# Run tests for a single module
mvn test -pl students-service
mvn test -pl infra
```

## Status

Currently in **Phase 1 (Students)**: DynamoDB table provisioned via CDK (tested with
`Template` assertions), `Student` domain entity and `DynamoDbStudentRepository` adapter
in progress. Next: Lambda handler + API Gateway integration.
