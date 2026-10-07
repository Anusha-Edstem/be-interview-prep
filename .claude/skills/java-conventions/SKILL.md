---
name: java-conventions
description: >-
  Engineering standard for the Java code in this repo. States the bar code is held to, what a
  review flags as blocking, and the gate commands that must pass. Loaded on demand by /start-issue,
  /pr-review and /fix-review-comments for issues and diffs that touch Java code; never pre-read.
---

# Java conventions

This skill is a **standard, not a description**: it states the bar this repo holds `Java` code to,
not a narration of what the code currently does. Apply it to the lines a change **adds or modifies**;
pre-existing violations in untouched code are backlog issues, not blockers. Surrounding code is never
precedent for a new violation.

## How this skill is used

- `/start-issue` loads it when an issue touches this stack, before any code is written.
- `/pr-review` and `/pr-review-ci` hand it to the **Reviewer Agents**, which report only the
  violations listed under **Blocking in review** below.
- `/fix-review-comments` verifies each finding against it before changing code.
- `.claude/conventions/comment-conventions.md` applies to every source file in this stack and is a
  gate: a comment in a source file fails the change.

## What a blocking finding is

A review flags a change only for: a correctness bug, a security hole, data loss or a crash; a
regression the issue did not ask for; a break of a contract or interface without its accompanying
change; a rule in **Blocking in review** below; or an unmet acceptance criterion. Style preferences,
alternative designs and "consider" remarks are not findings and are never posted.

## Project-specific

**Package Manager** and **Gate Commands** are registered once in `CLAUDE.md` under
`## Project-specific`, with **Stack** naming every stack in the repo. Read them there; they are not
restated here.

What lives here is the standard itself — the **Scope** this skill governs and the rules a change is
held to. Keep it free of counts, versions, issue numbers, file inventories and dates; those move.

- **Scope**: `src/main/java/`, `src/test/java/`, `src/main/resources/`

**Architecture rules**:
- One package root for the whole service; every feature is a package under it, named for the domain
  it owns rather than for the layer it sits in.
- A feature is layered `controller/ -> service/ -> repository/ -> entity/`, with `dto/` (split into
  `request/` and `response/`), `mapper/`, `exception/` and `config/` as needed. A layer calls only
  the layer below it.
- A controller maps the request, delegates, and maps the response. Business logic lives in a service;
  persistence lives in a repository. A controller never touches a repository or an entity directly.
- An entity never crosses the controller boundary. Requests arrive as request DTOs and leave as
  response DTOs, mapped explicitly.
- A class is injected through its constructor, not through a field. A new cross-feature dependency is
  a deliberate decision, not a convenience import.

**State, data and API rules**:
- Schema changes move through versioned migrations only. Hibernate's `ddl-auto` stays `none` or
  `validate`; it never writes schema.
- A migration is immutable once merged: a correction is a new migration, never an edit to an applied
  one, and it stays backward compatible for as long as running code still reads what it changes.
- A read-only service method carries `@Transactional(readOnly = true)`; a method that writes carries
  `@Transactional`. A transaction never spans a remote call it cannot roll back.
- Queries address N+1 risk with `@EntityGraph`, `JOIN FETCH` or batch fetching. A `@Query` binds its
  parameters and is never assembled by string concatenation.
- Paths sit under `/api/v1/{resource}` with the REST verb the operation actually is, and a list
  endpoint is paged with `Pageable`. The response shape of an endpoint is consistent across the
  service, errors included.
- Money is a fixed-precision type — `BigDecimal` with an explicit scale, or an integer minor unit —
  never `double` or `float`. Timestamps are stored and compared in UTC.
- Every custom exception extends one documented base exception carrying a status and an error code,
  and is translated to a response by a single `@RestControllerAdvice`. No bare `RuntimeException`, no
  `.get()` on an `Optional` where `.orElseThrow(...)` belongs, no empty catch.
- The caller identity behind any authorization decision comes from the authenticated principal, never
  from the request body, a path variable or a header the client controls.
- Secrets, API keys and credentials come from config placeholders (`${ENV_VAR}`), never from source,
  and never from a committed properties file.
- Enums persist as `@Enumerated(EnumType.STRING)`. Logging is parameterized, never string
  concatenation, and carries no credential or personal data.

**Testing rules**:
- Every code path a change adds or modifies has a test.
- Repository tests use `@DataJpaTest`; service tests use `@ExtendWith(MockitoExtension.class)`;
  controller tests use `@WebMvcTest` with `MockMvc`. A test never reaches a real network or a shared
  database.
- A test arranges, acts and asserts in that order, and shows the three phases through blank lines and
  named locals rather than `// Given` / `// When` / `// Then` markers, which the comment conventions
  ban like any other comment.
- A test name states the behaviour it pins, so a failure reads as a sentence about the system.
- Exceptions are asserted with `assertThrows`. Async work is waited on with Awaitility or mocked
  time, never `Thread.sleep`.
- Edge cases are covered, not only the happy path.

**Blocking in review**:
- A caller identity taken from the request body, path or a client-controlled header to make an
  authorization decision.
- A hardcoded secret, API key, password or connection string.
- A new endpoint left unauthenticated by its security configuration without the issue asking for it.
- A controller parameter without its input validation (`@Valid`, `@NotNull`, `@Size`).
- A token, password or personal data written to a log at any level.
- An error response that leaks a stack trace, SQL, or another user's identifiers.
- A schema change made outside a migration, a duplicate or reused migration version, or a change that
  breaks code still reading the column.
- Money held as `double` or `float`, or a naive local timestamp where an instant is required.
- A `@Query` or any SQL assembled by string concatenation.
- A swallowed exception, a bare `Optional.get()`, or a custom exception outside the documented
  hierarchy.
- An entity returned directly from a controller, or business logic placed in one.
- A write path without `@Transactional`, or a transaction held open across a remote call.
- An unbounded list endpoint — no `Pageable`, no limit.
- A changed code path with no test.
- Any comment in a source file, per `.claude/conventions/comment-conventions.md`.

**Not flagged**:
- Generated sources and Lombok-generated boilerplate.
- Migration file formatting.
- OpenAPI/Swagger annotation verbosity.
- Deployment, container and infrastructure configuration, which is reviewed separately.
- Test data setup verbosity in `@BeforeEach`.

**Definition of done**:
- The gate commands are green.
- Layering holds, and no entity crosses the controller boundary.
- The change is secure by default.
- Schema moved only through a migration.
- Every changed path has a test.
