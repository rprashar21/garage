# Enquiry submit endpoint (2026-11-03)

Goal: `POST /api/v1/enquiries` saves a customer enquiry and returns its reference.

## Flow

```
Client -> EnquiryController -> EnquiryService -> EnquiryRepository -> Postgres
          (validate, HTTP)     (map, rules)       (JPA)
```

Request and response use DTOs. The `Enquiry` entity never leaves the service.

## Mistakes found in the first draft, and why they matter

1. **Non-final injected fields.** `@RequiredArgsConstructor` builds a constructor
   from `final` fields only. Without `final`, Spring injects nothing and the field
   stays `null`, giving a `NullPointerException` on first call.
2. **Controller method was `private` with no mapping.** Spring only exposes
   public methods with `@PostMapping`. It also needs `@RequestBody` to read JSON
   and `@Valid` to run validation.
3. **Typo in the DTO name** (`EquiryDTO`) and a missing import. The compiler catches this.
4. **Path mismatch.** The design docs say `/api/v1/enquiries` (plural, REST convention).
5. **Entity had no creation path.** It has a protected no-arg constructor and only
   getters, so nothing outside could build one. Fix: a private `@Builder` constructor.

## Concepts

- **DTO (record):** a plain data carrier for the API. Records are immutable and
  short. Keeps the API shape independent of the database shape.
- **Bean Validation:** annotations like `@NotBlank`, `@Size`, `@Email` on the record.
  `@Valid` on the controller parameter triggers them. Failure returns 400.
- **Cross-field rule with `@AssertTrue`:** the DB `CHECK` says the preferred contact
  method needs a matching phone or email. The same rule in the DTO gives a clean 400
  instead of a database error. The DB constraint stays as the last line of defence.
- **Idempotency key (`submissionKey`):** the client generates a UUID per form. If the
  request is retried (double click, network retry), the service finds the existing row
  and returns it instead of creating a duplicate.
- **Race condition:** two identical requests can both pass the "does it exist?" check.
  The unique constraint rejects the second insert. The service catches
  `DataIntegrityViolationException` and returns the winner's row.
- **Why the service method is not `@Transactional`:** with a transaction, the insert
  happens at commit, after the method returns, so the `catch` would never see the
  failure. Without it, `save()` commits in its own transaction inside the `try`.
- **`@Builder` on a private constructor:** callers get readable named-field creation
  and the entity keeps its protected no-arg constructor for JPA.
- **`@PrePersist`:** sets `createdAt` and `updatedAt` automatically. `status` defaults to `NEW`.

## Files touched

- `enquiry/dto/EnquiryRequest.java` (new)
- `enquiry/dto/EnquiryResponse.java` (new)
- `enquiry/model/Enquiry.java` (builder constructor)
- `enquiry/repository/EnquiryRepository.java` (`findBySubmissionKey`)
- `enquiry/service/EnquiryService.java`
- `enquiry/controller/EnquiryController.java`

## Open items

- Notification to the mechanic after save (undecided).
- Consistent error response format (`@RestControllerAdvice`).
- Tests: service unit test, `MockMvc` controller test, duplicate `submissionKey` case.
