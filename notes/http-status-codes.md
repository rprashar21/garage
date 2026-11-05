# HTTP status codes — teaching notes

An HTTP status code is the server's short answer to: “What happened to my request?”
The response body can explain more. The code describes the outcome; it does not replace validation or business logic.

## The five families

| Starts with | Meaning | Think of it as |
|---|---|---|
| 1xx | Information | Keep going |
| 2xx | Success | It worked |
| 3xx | Redirection / cache instruction | Use another location or your cached copy |
| 4xx | Request cannot be fulfilled as sent | Check the request or access |
| 5xx | Server-side failure | The server could not complete it |

A 4xx is not always a typing mistake by a person: a missing resource or expired login also belongs here.

## Important success codes

| Code | Simple explanation | Garage example |
|---|---|---|
| **200 OK** | Request succeeded. Usually returns a body. | Return the original receipt for an identical enquiry retry. |
| **201 Created** | Request succeeded and created a resource. | A new enquiry was committed to PostgreSQL. A Location header can identify the resource when a suitable URL exists. |
| **202 Accepted** | Request was accepted for processing, but work is not complete. | A future report-generation job has been queued. Provide a way to check its result. |
| **204 No Content** | Request succeeded and returns no response body. | A successful update or deletion where no body is needed. |

**201 versus 202:** our enquiry is created synchronously, so we return 201 once it is stored. Email can still run later. That alone does not make the enquiry response 202.

## Important request and access codes

| Code | Simple explanation | Example |
|---|---|---|
| **400 Bad Request** | The request is malformed or fails our validation rules. | Missing name, invalid email, unknown enum value. |
| **401 Unauthorized** | Valid authentication is missing. Despite the name, think “Who are you?” | No valid login for the private garage dashboard. Authentication challenges use the WWW-Authenticate header. |
| **403 Forbidden** | The server refuses access. Often identity is known but permission is missing. | A logged-in user lacks permission to manage enquiries. |
| **404 Not Found** | The requested resource or route was not found, or the server chooses not to reveal it. | A private enquiry lookup uses an ID that does not exist. |
| **405 Method Not Allowed** | The route exists, but this HTTP method is unsupported. | GET on a route supporting only POST. The Allow header lists supported methods. |
| **409 Conflict** | The request conflicts with existing state. | Same submission key with a different message. |
| **413 Content Too Large** | The request body exceeds the accepted size. | Sending an enormous message or upload. |
| **415 Unsupported Media Type** | The body format is unsupported. | Sending XML when the endpoint accepts JSON only. |
| **422 Unprocessable Content** | The content is understood, but its instructions cannot be processed. | Some APIs use this for semantic validation. Our current contract uses 400 for form validation; stay consistent. |
| **429 Too Many Requests** | Too many requests within a limit. | Repeated form/chat submissions. Retry-After can tell the client when to try again. |

**401 versus 403:** missing/invalid identity versus refused permission. A 403 does not always imply the caller is logged in.

## Important server error codes

| Code | Simple explanation | Example |
|---|---|---|
| **500 Internal Server Error** | An unexpected server failure occurred. | An unhandled bug. Do not expose stack traces to the customer. |
| **502 Bad Gateway** | A gateway/proxy received an invalid response from an upstream server. | Reverse proxy cannot obtain a valid response from the backend. |
| **503 Service Unavailable** | The service is currently unable to handle the request, often temporarily. | Database outage prevents accepting enquiries. |
| **504 Gateway Timeout** | A gateway/proxy waited too long for an upstream response. | Proxy times out waiting for the backend. |

Not every exception should become 500. Expected invalid input should have a clear 4xx response.

## Useful redirect and caching codes

| Code | Simple explanation |
|---|---|
| **301 Moved Permanently** | A resource has a permanent new URL; clients may change POST to GET when following it. |
| **302 Found** | Temporary redirect; clients may change POST to GET. |
| **303 See Other** | Follow another URL using GET (or HEAD). Useful after a browser form submission. |
| **304 Not Modified** | A conditional GET/HEAD can reuse the cached representation. No response body. |
| **307 Temporary Redirect** | Temporary redirect that preserves the HTTP method and body. |
| **308 Permanent Redirect** | Permanent redirect that preserves the method and body. |

Example: permanently redirect an old service-page URL to its replacement. 304 is a caching response, not a redirect to a new page.

## Our enquiry contract

```text
New valid enquiry                  → 201
Same key + same normalised payload  → 200, original receipt
Same key + different payload        → 409
Invalid input                      → 400
Request over rate limit            → 429
Database unavailable               → appropriate failure, generally 503
```

A timeout does not prove the database write failed. The write may have committed before the response was lost. Reuse the submission key when retrying an unchanged payload.

## How Spring returns a code

```java
// A fixed status for every successful invocation:
@ResponseStatus(HttpStatus.CREATED)

// Or choose based on what actually happened:
return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
return ResponseEntity.ok(originalReceipt);
return ResponseEntity.noContent().build();
```

Use ResponseEntity when initial creation and replay need different statuses. Do not return HTTP 200 with an error message for a failed operation.

## Practice

A visitor submits an enquiry. PostgreSQL commits it, but email is still pending. Which status describes the newly created enquiry: 201 or 202?
