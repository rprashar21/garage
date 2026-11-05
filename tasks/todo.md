# Enquiry email notification (outbox) plan

Spec: `architecture diagram/notification-spec.md`. Split into slices; each slice ends with tests green and a check-in.

## Slice 1: Atomic creation (Gate A)
- [ ] Add Testcontainers (PostgreSQL) test deps to build.gradle
- [ ] Write failing tests: enquiry + PENDING task commit together; forced outbox failure rolls back enquiry; replay returns same ref; races converge on one row
- [ ] `V2__create_notification_outbox.sql` (incl. `claim_token`, constraints, `(status, next_attempt_at)` index)
- [ ] `NotificationTask` entity, status/channel enums, repository
- [ ] New bean `EnquiryCreator` with `@Transactional` method: insert enquiry + task
- [ ] `EnquiryService` calls it; keeps duplicate-key recovery outside the transaction
- [ ] NOT-C04: same key + different data returns 409 (new behaviour; needs exception + handler)
- [ ] Run existing enquiry tests as regression

## Slice 2: Worker with fake sender (Gate B)
- [ ] `EmailSender` interface + fake; injectable `Clock`
- [ ] Config properties: poll 5s, batch 10, max attempts 6, lease 60s, timeout 15s, recipient/sender
- [ ] Claim query (`FOR UPDATE SKIP LOCKED`, attempts+1, claim token, lease) in short transaction
- [ ] Record outcome with conditional update (id + PROCESSING + token)
- [ ] Backoff 30s/1m/2m/4m/8m; sixth failure -> FAILED
- [ ] Expired lease recovery; sixth abandoned attempt -> FAILED
- [ ] `@Scheduled` poller, `@EnableScheduling`
- [ ] Tests for each Gate B item

## Slice 3: Real email (Gate C)
- [ ] Pick provider (open decision) and sandbox inbox
- [ ] Implement `EmailSender`, plain-text body, creds via env
- [ ] Verify Gate C checks manually
- [ ] Update roadmap with commands and outcomes

## Review
(filled in at the end)
