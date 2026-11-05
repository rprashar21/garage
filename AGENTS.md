# AGENTS.md

This file provides guidance to Codex (claude.ai/code) when working with code in this repository.

## Project

Backend API for a single independent mechanic's website (BMW focus). Java 21, Spring Boot 4.1, Gradle, PostgreSQL, Flyway, Lombok. Base package `com.garage.garage_api`.

Only the `Enquiry` entity (`enquiry/model/`) and its Flyway migration exist. There is no repository, service, controller or DTO yet. Everything else described in `architecture diagram/` is a proposal.

## Commands

```bash
./gradlew bootRun                                  # run the app (needs a reachable Postgres)
./gradlew build                                    # compile + test
./gradlew test                                     # all tests
./gradlew test --tests 'com.garage.garage_api.GarageApiApplicationTests'   # single class
./gradlew test --tests '*.ClassName.methodName'    # single method
```

## Database and configuration

- `application.properties` reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (see `.env.example`). Spring does not load `.env` itself: export the variables or set them in the IDE run configuration.
- The default URL uses port 5432, but local dev has used a Docker Postgres on **5433** (`jdbc:postgresql://localhost:5433/garage`, container `garage-db`). `.env.example` and `JdbcSample` disagree on the port, so check which one you are running against.
- `@SpringBootTest` (`contextLoads`) starts the full context, so it fails without a live database.
- Schema is owned by Flyway (`src/main/resources/db/migration`). Hibernate runs with `ddl-auto=validate`, so entity and migration must match exactly. Never edit an applied migration; add a new `V<n>__*.sql`.
- `spring.jpa.open-in-view=false`: lazy loading outside a transaction will fail.

## Architecture notes

- Enum values (`EnquiryType`, `EnquiryStatus`, `PreferredContact`) are stored as strings and duplicated in SQL `CHECK` constraints. Changing an enum requires a new migration that updates the constraint.
- The `enquiry` table enforces business rules in the database: preferred contact method requires the matching phone or email, and name/message cannot be blank. `submission_key` (UUID, unique) is the planned idempotency key for `POST /api/v1/enquiries`; `reference` is the unique customer-facing id.
- `Enquiry` uses `@NoArgsConstructor(access = PROTECTED)` and only `@Getter`, so it needs an explicit creation path (planned as the next roadmap slice). `createdAt`/`updatedAt` are set by `@PrePersist`/`@PreUpdate`.
- Planned layering under `com.garage.garage_api.enquiry`: `model` (exists), `dto`, `repository`, `service`, `controller`. Do not expose entities in API requests or responses, and avoid single-implementation interfaces.
- `sample/JdbcSample.java` is a standalone learning `main()` using raw JDBC. It is not part of the app.

## Roadmap docs

`architecture diagram/` is the project roadmap and design record (system design, acceptance criteria, scaling, chatbot/RAG). Its working agreement: the developer writes application code in small steps, and the assistant explains and reviews unless asked to implement. Tick a roadmap checkbox only after the check passes, and update the roadmap and affected design when requirements change.

`configuration.md` is pasted terminal output from a previous session, not documentation.
