# be-interview-prep

Backend interview prep exercises. Each question is built on its own branch, reviewed as a pull
request, and squashed into `main`.

## Stack

- Java 17
- Spring Boot 3.5.16
- Spring Web, Spring Data JPA, Bean Validation
- Flyway migrations over H2 (in-memory, PostgreSQL compatibility mode)
- JUnit 5, MockMvc, Mockito

H2 runs in memory so the project clones and runs with no database to install. Flyway owns the
schema and Hibernate is set to `validate`, so the mapping is checked against the migrations on
every start.

## Running

```bash
./mvnw spring-boot:run
```

The service listens on `http://localhost:8080`.

## Verifying

```bash
./mvnw spotless:apply
./mvnw test
```

## Questions

| # | Branch | Scope |
|---|---|---|
| 1 | `feature/q1-task-api` | Task Manager API |
| 2 | `feature/q2-urlshortener` | URL shortener |
| 3 | `feature/q3-auth` | Authentication |
| 4 | `feature/q4-product-catalog` | Product catalog |
| 5 | `feature/q5-order-service` | Order service |

## Walkthrough video

Added after the final merge.
