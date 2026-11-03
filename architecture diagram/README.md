# Garage website: architecture and development guide

Updated: 3 November 2026. These are proposed designs unless explicitly marked as present in source. This folder is the project roadmap; update it as requirements and implementation change.

## Start here

Read the [product specification](spec.md) first: it explains what a spec is, the customer journey, requirements and release boundaries.

1. [System design](system-design.md): components, layers, data model, deployment, and interview discussion.
2. [Acceptance criteria and roadmap](acceptance%20criteria/roadmap.md): ordered implementation slices and checks.
3. [Application improvements](application%20improvements/scaling.md): performance, asynchronous work, and distributed systems experiments.
4. [AI agentic flow](ai%20agentic%20flow/chatbot-rag.md): future chatbot, retrieval, safety, and cost controls.

## Current evidence

Present in source: Java 21 Gradle configuration; Spring Web MVC, JPA, validation, Flyway, PostgreSQL, Actuator and Lombok dependencies; `V1__create_enquiry_table.sql`; `enquiry/model/Enquiry.java` and three enums. The entity has lifecycle timestamp callbacks and a protected empty constructor.

Not found in the inspected source: enquiry repository, service, controller, request/response DTOs, frontend, notification outbox, or AI integration. Migration success was reported by the developer in chat; no database connection, build, or application test was run for this documentation update.

## Next task

Implement slice 2 of the roadmap: an explicit entity creation path, repository, service, and PostgreSQL integration tests. The actual entity package is `com.garage.garage_api.enquiry.model`; examples must import that package.

## Working agreement

- Developer writes application code in small steps; assistant explains and reviews unless asked to implement.
- Each task has a purpose, acceptance criteria, verification evidence, and a next step.
- Tick a checkbox only after the check passes; source presence alone does not prove runtime behaviour.
- For a requirement change, update this roadmap, the affected design, and tests. Add a new Flyway migration for an already-applied schema change.
- Record unresolved decisions rather than silently treating a proposal as approved.

Open decisions: garage name/location, real service catalogue, domain, hosting budget, notification provider, privacy/retention policy, frontend confirmation, and AI provider/budget.
