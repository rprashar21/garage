# Java records and the race condition (2026-11-03)

## What is a record?

A short way to write a class that only holds data. Java generates the fields,
constructor, getters, `equals`, `hashCode` and `toString`.

```java
public record EnquiryResponse(String reference, EnquiryStatus status, Instant createdAt) {}
```

- Fields are `final`, so data cannot change after creation.
- Getters have no `get` prefix: `response.reference()`.
- You can still add your own methods (see the `@AssertTrue` check in `EnquiryRequest`).
- Good for DTOs, which only carry data. Entities stay normal classes because JPA needs to change them.

## Race condition

A bug that depends on timing. Two things happen at nearly the same moment and the
result depends on which gets ahead.

### How it happens here

A customer double-clicks Submit, or the network retries. Both requests carry the
same `submissionKey`. The service does two steps: check if the key exists, then save.
The gap between the steps is the problem.

```
Time   Request A                      Request B
 1     check key... not found
 2                                    check key... not found
 3     save enquiry (success)
 4                                    save enquiry (FAILS: duplicate key)
```

Both see "not found" because B checks before A has saved. Neither request is wrong
on its own. The timing causes the clash.

### What we did

1. **Database is the real guard.** `submission_key` has a `UNIQUE` constraint.
   Postgres rejects the second insert, so a duplicate row can never exist.
2. **Service handles the failure.** `save` sits in a `try/catch`. When B hits the
   duplicate error, it looks up the row A saved and returns it. The customer sees
   the same reference both times and no error.

The check at the start of the method is only a shortcut for the common retry. The
`catch` covers the rare true collision.

### Why no `@Transactional` on the service method

With it, the insert happens at commit, after the method returns, so the `catch`
would never see the error. Without it, `save()` commits in its own transaction
inside the `try`.

## Takeaway

Check-then-act is unsafe under concurrency. Let the database enforce the rule
(unique constraint), then handle the failure in code.
