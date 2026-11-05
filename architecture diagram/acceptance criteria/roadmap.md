# Development roadmap and acceptance criteria

Checklists below are verification gates, not claims that work has passed. For each completed slice record date, command/test, result, and any remaining limitation. Dependencies run in order unless stated otherwise.

## Slice 1 — Database and model foundation

Present in source; runtime verification remains to be recorded.

- [ ] PostgreSQL starts with persistent storage and a dedicated application database.
- [ ] Empty database receives V1 through Flyway; restarting does not reapply V1.
- [ ] JPA schema validation passes for the entity in `enquiry.model`.
- [ ] Database rejects invalid contact choice, missing selected contact, invalid status/type, blank name/message, and duplicate keys.
- [ ] Secrets stay outside Git; a placeholder `.env.example` documents configuration. The chosen runtime explicitly loads environment values.

Evidence: inspect `flyway_schema_history`, `enquiry` columns/constraints and application startup. Do not erase the existing development database just to prove migration; use a disposable database.

## Slice 2 — Repository and service: next

Purpose: create an enquiry without needing HTTP or frontend.

- [ ] Add an explicit constructor/factory or builder that preserves NEW status and does not expose backend-owned fields to visitors.
- [ ] Add `EnquiryRepository extends JpaRepository<Enquiry, UUID>`.
- [ ] Add a service command and transactional creation service.
- [ ] Service enforces selected-contact rules, sets a unique reference and actual configured privacy notice version, and keeps timestamps/backend status server-controlled.
- [ ] PostgreSQL integration test persists and reloads all fields, including enum strings and timestamps.
- [ ] Invalid input creates no row; transaction failure leaves no partial state.

Use real PostgreSQL semantics (e.g. a disposable Testcontainers instance), not H2 as proof of PostgreSQL compatibility. Keep this slice internal until validation/error handling and idempotency are ready.

## Slice 3 — REST endpoint and one complete backend flow

- [ ] Request DTO validates required values, lengths, email when supplied, enum values, submission key and selected contact.
- [ ] Response DTO returns reference and receipt status; no JPA entity or contact details are serialised.
- [ ] POST `/api/v1/enquiries` returns 201 after commit.
- [ ] Identical retries return the same receipt with 200 and one row; changed payload with same key gives 409.
- [ ] Concurrent duplicate test proves the unique constraint plus recovery path creates one enquiry.
- [ ] Malformed JSON and invalid input return 400 with safe errors; no stack traces/SQL exposed.
- [ ] Database failure does not return a receipt or claim success.
- [ ] Request limits, allowed browser origins and rate limits are configured; CORS is not treated as abuse prevention.

Manual check after implementation:

```bash
curl -i http://localhost:8080/api/v1/enquiries \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 28d90f59-46e8-427e-9c88-88ab69f301ad' \
  -d '{"customerName":"Test Customer","phone":"07123456789","preferredContact":"PHONE","enquiryType":"SERVICE","vehicleMake":"BMW","message":"Please contact me about servicing."}'
```

Verify the returned reference in DBeaver. Replay the request, change the message with the same key, and send invalid input. Record HTTP statuses and row counts. Test data is illustrative, not a real enquiry.

## Slice 4 — Durable email notification

- [ ] Add V2 for notification outbox; preserve applied V1.
- [ ] Enquiry and task commit atomically.
- [ ] Worker claims tasks safely, retries with bounded backoff, and recovers expired leases.
- [ ] Provider outage leaves the enquiry stored and notification retryable.
- [ ] Retry exhaustion is visible and supports controlled manual retry.
- [ ] Crash-after-send scenario is tested/documented; use provider idempotency if available. Do not promise exactly-once delivery.
- [ ] Use a sandbox inbox, sanitise errors, and minimise customer data in notifications.
 
- Customer submits enquiry
  ↓
  One database transaction saves:
  • the enquiry
  • a pending notification
  ↓
  Customer receives their reference
  ↓
  Background worker sends the email
  ↓
  Marks notification SENT, or schedules a retry

## Slice 5 — Frontend enquiry journey

- [ ] Confirm frontend choice and create responsive Home, Services/BMW, About/Work, Buy/Sell, Contact and Privacy pages.
- [ ] Use real garage details, approved photographs and genuine testimonials.
- [ ] Form fields map to the request DTO; optional values are handled consistently.
- [ ] One key is reused for retries of the same payload; changing the submitted payload starts a new submission.
- [ ] Success appears only after API acknowledgement; failures preserve form input and explain retry/call options.
- [ ] Keyboard navigation, labels, focus, loading feedback and mobile layout work.
- [ ] Browser end-to-end test submits to backend and verifies a row plus notification task.
- [ ] Client bundle contains no database/provider secrets.

## Slice 6 — Private garage workflow, later

- [ ] Add authenticated, authorised, paginated enquiry listing filtered by status and ordered by date.
- [ ] Define and test allowed status transitions and timestamps.
- [ ] Unauthenticated/unauthorised callers cannot read or modify customer details.
- [ ] Avoid sensitive logs; implement agreed retention/deletion and audit behaviour.

## Slice 7 — Staging and public launch

- [ ] Build/test passes using the Gradle wrapper and Java 21.
- [ ] Backend container, frontend deployment and private PostgreSQL are configured separately from local development.
- [ ] HTTPS, DNS, secrets, database pool limits, timeouts and readiness checks are verified.
- [ ] Successful and failed submissions are smoke-tested on staging.
- [ ] Backup restore, alerting and deployment rollback are rehearsed.
- [ ] Publish accurate privacy information and agreed retention policy before collecting public enquiries.
- [ ] Verify page metadata, canonical URLs, sitemap, robots rules and local business structured data.
- [ ] Configure Business Profile and Search Console; track enquiries/calls and indexing without promising ranking.

## Slice 8 — Scaling experiments and AI

Follow the separate improvement and AI plans. These are later milestones, not prerequisites for the first useful garage website.

## Change and completion log

| Date | Slice / requirement | Evidence | Remaining work |
|---|---|---|---|
| 2026-11-03 | Documentation baseline | Inspected migration/entity/build source | Runtime checks and slices above remain |
