# Enquiry flow with the notification outbox

Branch: `feature/outbox-1`. Spec: `architecture diagram/notification-spec.md`.

## The flow, step by step

```
Customer -> POST /api/v1/enquiries
  -> EnquiryController.submitEnquiry      (validates the body, returns 201)
  -> EnquiryService.submitEnquiry         (NOT transactional)
       1. findBySubmissionKey             (already saved? return the original)
       2. create(request)                 (build an Enquiry from the request)
       3. EnquiryCreator.create(enquiry)  (@Transactional, new in this change)
            a. save the enquiry
            b. save a NotificationTask (PENDING)
            -> COMMIT both, or ROLLBACK both
       4. on DataIntegrityViolationException: look the key up again, return the winner
  -> EnquiryResponse (reference, status, createdAt)
```

The customer gets the response after both rows commit. No email is sent in the request.

## What changed and why

| Piece | What it is | Why |
|---|---|---|
| `V2__create_notification_outbox.sql` | Table `notification_outbox` | Stores the "email the mechanic" reminder in PostgreSQL, so it survives restarts |
| `NotificationTask` | Entity for that table | Holds status, attempt count, next attempt time, lease and claim token |
| `NotificationChannel`, `NotificationStatus` | Enums | `EMAIL`; `PENDING`, `PROCESSING`, `SENT`, `FAILED` |
| `NotificationTaskRepository` | Empty `JpaRepository` | `save()` is enough for now |
| `EnquiryCreator` | New bean, one `@Transactional` method | Saves enquiry and task in one transaction |
| `EnquiryService` | Calls `EnquiryCreator` instead of `enquiryRepository.save` | Keeps duplicate-key recovery outside the transaction |

## Why a separate bean

- Before: `save()` ran in its own transaction, so the enquiry committed alone.
- If the task insert were a second `save()`, the enquiry could commit and the task fail. The mechanic would never hear about that enquiry.
- `@Transactional` works through a Spring proxy. A call from one method to another in the same class skips the proxy, so the annotation does nothing. A separate bean makes the call go through the proxy.
- `EnquiryService` must stay outside the transaction. A failed insert aborts a PostgreSQL transaction, so recovery (the second `findBySubmissionKey`) can only run after it ends.

## The outbox pattern in one line

Write the reminder in the same transaction as the data. A worker reads reminders later and sends the email. This avoids doing a slow, unreliable email call inside a database transaction.

## Not done yet

- No worker, so tasks stay `PENDING` forever.
- No email sender.
- 409 for a reused key with different data (NOT-C04).
- Gate A tests (real PostgreSQL rollback and race tests). Until they exist, the rollback is untested.
- The mocked `EnquiryServiceTest` passes, but it cannot prove the transaction works.

## Next

1. Run the app against Docker Postgres and check one enquiry creates one `PENDING` task.
2. Worker with a fake sender (claim, retry, backoff, lease recovery).
3. Real email provider.
