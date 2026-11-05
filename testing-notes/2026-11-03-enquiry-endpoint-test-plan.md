# Enquiry endpoint: test plan and Postman guide (2026-11-03)

Endpoint under test: `POST /api/v1/enquiries`

Status key: **Done** = automated test passing. **Planned** = to do next. **Manual** = check in Postman.

## Test layers

| Layer | Class | Needs DB | Status |
|---|---|---|---|
| Controller (`@WebMvcTest`, mocked service) | `EnquiryControllerTest` | No | 7 tests, passing |
| Service (Mockito) | `EnquiryServiceTest` | No | 5 tests, passing |
| Integration (`@SpringBootTest`, real Postgres) | not written yet | Yes | Planned |
| Manual (Postman) | n/a | Yes | Run after each change |

## Postman guide

### Start the app

```bash
docker start garage-db                 # Postgres on port 5433
export DB_URL=jdbc:postgresql://localhost:5433/garage
export DB_USERNAME=...
export DB_PASSWORD=...
./gradlew bootRun
```

App listens on `http://localhost:8080`.

### Build the request

1. New, HTTP. Method **POST**, URL `http://localhost:8080/api/v1/enquiries`.
2. Headers: `Content-Type: application/json`.
3. Body: raw, JSON.

```json
{
  "submissionKey": "{{$guid}}",
  "customerName": "Sam",
  "email": "sam@example.com",
  "phone": null,
  "preferredContact": "EMAIL",
  "enquiryType": "SERVICE",
  "vehicleMake": "BMW",
  "vehicleModel": "320d",
  "vehicleYear": 2018,
  "registration": "AB18 CDE",
  "message": "Need a service",
  "privacyNoticeVersion": "v1"
}
```

`{{$guid}}` gives a new random UUID on every send. To test idempotency, replace it
with a fixed UUID and send twice.

Expected on success: `201` and `{ "reference": "ENQ-XXXXXXXX", "status": "NEW", "createdAt": "..." }`.

### Check the data

```sql
select reference, customer_name, email, phone, preferred_contact, status, created_at
from enquiry order by created_at desc;
```

### Common problems

- Connection refused: app not running, check the terminal for a DB error.
- 404: URL must be plural `/api/v1/enquiries` and the method POST.
- 415: `Content-Type` is not `application/json`.
- 400 with an empty body: Spring default for validation errors. Field messages need a `@RestControllerAdvice` (open item).

## Scenarios

### A. Happy paths

| ID | Scenario | Expected | Status |
|---|---|---|---|
| A1 | All fields valid, EMAIL preferred | 201, reference `ENQ-` + 8 hex | Done (controller, service) |
| A2 | PHONE preferred with phone, email null | 201 | Planned |
| A3 | Both phone and email given | 201 | Planned |
| A4 | Minimal body (no vehicle fields, no registration) | 201, vehicle columns null | Planned |
| A5 | Full body including vehicle year, make, model, registration | 201, all columns saved | Manual |
| A6 | `enquiryType` each of SERVICE, REPAIR, BUY, SELL, OTHER | 201 for each | Planned |
| A7 | New enquiry has status NEW and `createdAt` set | response shows NEW | Done (service) |

### B. Idempotency and race condition

| ID | Scenario | Expected | Status |
|---|---|---|---|
| B1 | Same `submissionKey` sent twice, sequentially | Both 201, same reference, one row | Done (service, mocked), Manual |
| B2 | Same key, different body second time | Returns the original enquiry, second body ignored | Planned |
| B3 | Two identical requests at the same instant | Both 201, same reference, one row | Planned (integration) |
| B4 | `save` loses the race (duplicate key) | Winner's enquiry returned | Done (service, mocked) |
| B5 | Integrity error with no existing row (other constraint) | Exception rethrown | Done (service) |
| B6 | Different keys, same content | Two rows, two references | Planned |

### C. Validation: required fields (expect 400, service never called)

| ID | Scenario | Status |
|---|---|---|
| C1 | `submissionKey` missing | Done |
| C2 | `submissionKey` not a UUID (`"abc"`) | Planned |
| C3 | `customerName` missing, empty, or whitespace only | Planned |
| C4 | `message` empty or whitespace only | Done (blank) |
| C5 | `preferredContact` missing | Planned |
| C6 | `preferredContact` unknown value (`"SMS"`) | Planned |
| C7 | `enquiryType` missing | Planned |
| C8 | `enquiryType` unknown value (`"BOGUS"`) | Done |
| C9 | `privacyNoticeVersion` missing or blank | Planned |

### D. Validation: contact rule (mirrors DB CHECK)

| ID | Scenario | Expected | Status |
|---|---|---|---|
| D1 | PHONE preferred, phone null | 400 | Done |
| D2 | PHONE preferred, phone whitespace only | 400 | Planned |
| D3 | PHONE preferred, only email given | 400 | Planned |
| D4 | EMAIL preferred, email blank | 400 | Done |
| D5 | EMAIL preferred, email null, phone given | 400 | Planned |
| D6 | Invalid email format (`not-an-email`) | 400 | Done |

### E. Validation: lengths and ranges (boundary: max passes, max+1 fails)

| ID | Field | Pass | Fail | Status |
|---|---|---|---|---|
| E1 | `customerName` | 120 chars | 121 | Planned |
| E2 | `email` | 254 chars | 255 | Planned |
| E3 | `phone` | 32 chars | 33 | Planned |
| E4 | `vehicleMake` | 60 chars | 61 | Planned |
| E5 | `vehicleModel` | 80 chars | 81 | Planned |
| E6 | `registration` | 20 chars | 21 | Planned |
| E7 | `message` | 4000 chars | 4001 | Planned |
| E8 | `privacyNoticeVersion` | 30 chars | 31 | Planned |
| E9 | `vehicleYear` | 1900 and 2100 | 1899, 2101 | Planned |
| E10 | `vehicleYear` not a number (`"abc"`) or too large for a short (`99999`) | n/a | 400 | Planned |

### F. Request format

| ID | Scenario | Expected | Status |
|---|---|---|---|
| F1 | Malformed JSON | 400 | Planned |
| F2 | Empty body | 400 | Planned |
| F3 | Wrong content type (`text/plain`) | 415 | Planned |
| F4 | GET on `/api/v1/enquiries` | 405 | Planned |
| F5 | Old path `/api/v1/enquiry` | 404 | Planned |
| F6 | Unknown extra field in JSON | Decide: ignored or 400 (check Boot 4 default) | Planned |

### G. Data handling in the service

| ID | Scenario | Expected | Status |
|---|---|---|---|
| G1 | Name and message with leading/trailing spaces | Trimmed before saving | Done |
| G2 | Blank optional fields (phone, make, model, registration) | Saved as null | Done (phone, make), Planned (rest) |
| G3 | Unicode and emoji in name or message | Saved and returned intact | Planned (integration) |
| G4 | Reference format `ENQ-` + 8 uppercase hex | Matches regex | Done |
| G5 | Reference collision (same random value twice) | Unique constraint rejects, see open question below | Planned |

### H. Database level (integration test with real Postgres)

| ID | Scenario | Expected | Status |
|---|---|---|---|
| H1 | Flyway migration applies, Hibernate `validate` passes | Context starts | Done only with a live DB (`contextLoads`) |
| H2 | Insert two rows with the same `submission_key` | Unique violation | Planned |
| H3 | Insert two rows with the same `reference` | Unique violation | Planned |
| H4 | Insert violating `chk_enquiry_contact_details` | Check violation | Planned |
| H5 | Insert with blank name or message | Check violation | Planned |
| H6 | `createdAt` equals `updatedAt` on insert | Equal | Planned |
| H7 | Full HTTP to DB round trip with `MockMvc` or `TestRestTemplate` | 201 and row present | Planned |

## Open questions to settle while testing

1. **G5 / reference collision.** The service's `catch` assumes any
   `DataIntegrityViolationException` is a `submissionKey` clash. A `reference` clash
   would find no existing row and rethrow, giving a 500. Probability is very low, but
   decide whether to retry with a new reference.
2. **B2.** Confirm that returning the original enquiry when the body differs is the behaviour we want.
3. **F6.** Decide whether unknown JSON fields should be rejected.
4. **Error body.** Add a `@RestControllerAdvice` so 400 responses say which field failed.

## Suggested order for the next session

1. Fill the controller gaps (C2 to C9, D2 to D5, E, F1 to F4).
2. Add the integration test class for B3, B6, H2 to H7 against real Postgres.
3. Run the Postman pass for A5, B1 and one case from each of C, D and E.
4. Tick rows here only after the check passes.
