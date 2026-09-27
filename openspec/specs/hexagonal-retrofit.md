# Spec: hexagonal-retrofit

**Version:** 1.0
**Status:** approved (Fase 2, Día 5)
**Reference:** guidelinesHexagonal-serverless.md

## Description

Migrate an existing bounded-context module from its current ad-hoc structure to the
hexagonal layout and conventions defined in `guidelinesHexagonal-serverless.md`,
without changing observable behavior.

## Inputs

- `bounded_context_module` (string) — the Maven module to migrate, e.g.
  `courses-service` or `students-service`.
- `source_files` (list<path>) — all `.java` files under `domain/`, `application/`,
  `infrastructure/` of that module.
- `conventions_reference` — `guidelinesHexagonal-serverless.md`, authoritative source
  for target package layout, naming, and error handling.

## Outputs

- `refactored_files` (list<path>) — same files, moved/renamed/edited per the target
  structure.
- `new_files` (list<path>) — Value Objects extracted, InMemoryRepositories added,
  UseCases extracted (if missing).
- `migration_report` (markdown) — one line per file: what changed and why, to paste
  into `memoria-progreso.md`.

## Expected behavior

- All existing tests must pass unchanged in assertions (only method/class names and
  exception types may change to match the new convention).
- No business rule, validation threshold, or persisted data format (DynamoDB item
  shape: PK/SK, attribute names) changes as a result of this migration.
- Domain classes end with zero imports from infrastructure or AWS SDK packages.
- Every custom exception extending `RuntimeException` with a single-purpose message is
  replaced by `ValidationError` (format/invariant) or `DomainError`
  (notFound/alreadyExists/other), per `guidelinesHexagonal-serverless.md` §6.
- Test method names no longer start with "should"; they describe the business rule in
  domain language (`guidelinesHexagonal-serverless.md` §8.3).

## Edge cases

- **A module with an already-deployed Lambda** (`students-service`): the migration
  must preserve the exact HTTP status codes currently returned (201/400/409) — the
  handler's error-mapping table is updated to use `DomainError.getType()`/
  `ValidationError`, but external behavior (status codes, response body shape) stays
  identical. Verified by existing `RegisterStudentHandlerTest`, unchanged assertions.
- **A module without a Lambda handler yet** (`courses-service`): no handler-level
  verification needed, but the domain/application refactor still must not break
  `EnrollStudentInCourseUseCaseTest`.
- **Value Objects with no reuse candidate yet** (`Student.firstName`/`lastName`): left
  as plain `String`, per `guidelinesHexagonal-serverless.md` §4.3 criterion (no VO
  without a reuse or invariant case).

## Sequencing constraint (project-specific, not technical)

Applied to `courses-service` first (Día 6) — no deployed Lambda, lowest risk.
`students-service` only after `courses-service`'s migration is verified green
end-to-end (Día 7) — has a Lambda already deployed and verified in production
(Fase 1), higher risk.
