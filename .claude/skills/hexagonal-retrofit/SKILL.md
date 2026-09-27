---
name: hexagonal-retrofit
description: Refactors a school-serverless-platform microservice module (courses-service, students-service, etc.) from its current flat domain/ structure to the hexagonal layout defined in guidelinesHexagonal-serverless.md, preserving all existing test assertions and runtime behavior. Use when explicitly asked to apply the hexagonal architecture migration/retrofit to a named module.
---

# hexagonal-retrofit

## Purpose

Refactor one Maven module of `school-serverless-platform` from its current flat
`domain/` structure to the hexagonal layout defined in
`guidelinesHexagonal-serverless.md`, preserving all existing test assertions and
runtime behavior. Full contract: `openspec/specs/hexagonal-retrofit.md`.

## Steps

1. Read `guidelinesHexagonal-serverless.md` sections 1 (module structure), 4
   (patterns), 6 (DomainError/ValidationError), 8.3 (test naming) as the source of
   truth. Read `openspec/specs/hexagonal-retrofit.md` for the full contract.
2. List every `.java` file in the target module's `domain/`, `application/`,
   `infrastructure/` packages.
3. Move entity classes into `domain/entities/`, ports into `domain/repositories/`,
   new Value Objects into `domain/valueobjects/`. Update package declarations and all
   imports referencing moved classes across the whole module.
4. Replace each custom exception class's throw sites with `ValidationError.create(...)`
   / `DomainError.createNotFound(...)` / `DomainError.createAlreadyExists(...)`,
   matching the original message text. Delete the now-unused exception class files.
5. Extract any Value Object named explicitly for this module (e.g. `CourseCapacity`,
   `Email`) from its parent entity, moving the corresponding validation logic into the
   VO's own `create()` factory.
6. Rename test methods removing the "should" prefix, rewording to domain language,
   without changing what each test asserts.
7. Run `mvn clean install` from the repo root after every discrete change (one file or
   one rule applied at a time) and stop immediately if any test fails — do not proceed
   to the next file with a red build.
8. Produce the `migration_report` listing every file touched and why.

## Must not

- Change any assertion value, business threshold (e.g. `maxCapacity > 0`), or
  DynamoDB attribute name/PK-SK format.
- Introduce new business rules, new validations, or new public methods beyond what
  the retrofit requires.
- Touch `students-service` until `courses-service`'s migration is verified green
  end-to-end (sequencing constraint from the project, not technical).
- Modify `RegisterStudentHandler`'s external contract (HTTP status codes, response
  shape) even though its internals change.
- Commit with a red test — one commit per rule/file applied and verified green, per
  the project's git-strategy discipline already in force (Conventional Commits,
  ≤50 chars, one commit per green test).
