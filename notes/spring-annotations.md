# Spring and related annotations — teaching notes

An annotation is metadata attached to a class, method, field or parameter. A framework or compiler reads it to decide what to do. It is not magic: always ask “Who reads this, and when?”

Not every annotation in our project belongs to Spring. JPA handles database mapping, Jakarta Validation checks input, and Lombok generates Java code.

## 1. Starting the application

| Annotation | Simple meaning | Who reads it? |
|---|---|---|
| `@SpringBootApplication` | Starts Boot configuration and component scanning from this package downward. | Spring Boot |
| `@Configuration` | This class defines application configuration. | Spring |
| `@Bean` | Register this method's returned object in Spring's object container. | Spring |
| `@ConfigurationProperties` | Bind a group of settings to a typed object. | Spring Boot |
| `@Value` | Inject a configuration value or expression. Never hardcode secrets. | Spring |

A **bean** is an object managed by Spring. Dependency injection means Spring supplies an object's dependencies, usually through its constructor.

## 2. Layers and dependency injection

| Annotation | Simple meaning | Garage example |
|---|---|---|
| `@Component` | A general Spring-managed class. | A helper that needs injection. |
| `@Service` | A component containing business operations. | EnquiryService. |
| `@Repository` | A persistence component; also supports exception translation when configured. | A custom database adapter. Spring Data repository interfaces are discovered without adding this annotation. |
| `@Autowired` | Ask Spring to inject a dependency. | Usually unnecessary on a class's only constructor. |
| `@Qualifier` | Choose which bean when several match the same type. | Choose one email sender. |

Do not add every annotation everywhere. Choose one meaningful role for a class.

## 3. HTTP controller annotations

| Annotation | Simple meaning | Example |
|---|---|---|
| `@RestController` | Handles HTTP requests and writes return values into response bodies, usually JSON. | EnquiryController. |
| `@RequestMapping` | Sets a route prefix or routing conditions. | `/api/v1/enquiries`. |
| `@PostMapping` | Handle POST requests. | Submit a new enquiry. |
| `@GetMapping` | Handle GET requests. | Read public service information. |
| `@PutMapping` | Handle PUT, normally replacing a resource representation. | A full resource update when the contract supports it. |
| `@PatchMapping` | Handle PATCH for a partial change. | Change enquiry status in a private endpoint. |
| `@DeleteMapping` | Handle DELETE requests. | An authorised deletion flow. |
| `@RequestBody` | Convert the request body into a Java object. | JSON into EnquiryRequest. |
| `@PathVariable` | Read a value from the route. | The ID in `/enquiries/{id}`. |
| `@RequestParam` | Read a query/form parameter. | `?status=NEW&page=0`. |
| `@RequestHeader` | Read an HTTP header. | Idempotency-Key. |
| `@ResponseStatus` | Set a fixed response status. | 201 for creation; use ResponseEntity for variable statuses. |
| `@RestControllerAdvice` | Centralise controller exception handling and related advice. | Consistent safe error responses. |
| `@ExceptionHandler` | Handle a particular exception type. | Map a submission conflict to 409. |

These mappings route requests; they do not automatically implement authentication, business rules or database access.

## 4. Input validation — Jakarta Validation

| Annotation | Simple meaning |
|---|---|
| `@Valid` | Trigger validation of this object at a supported boundary and cascade into nested objects. |
| `@NotNull` | Must not be null; an empty string still passes. |
| `@NotBlank` | Text must contain something other than whitespace; null also fails. |
| `@NotEmpty` | Must not be null or empty; whitespace text still passes. |
| `@Size(max = 120)` | Limit text/collection length; null generally passes unless separately forbidden. |
| `@Email` | Check email syntax; does not prove the address exists or belongs to someone. Pair with required-value rules when needed. |
| `@Min` / `@Max` | Numeric lower/upper bounds. |
| `@AssertTrue` | The annotated Boolean property/method must evaluate to true when present. Useful for our selected-contact check. |

Spring's `@Validated` supports validation groups and method-validation integration. Do not assume adding constraints to a method makes every direct Java call validated: method validation needs the appropriate Spring configuration/proxy.

```java
public EnquiryResponse submit(@Valid @RequestBody EnquiryRequest request) {
    return service.submitEnquiry(request);
}
```

Here MVC reads JSON, validates the DTO, then calls the controller method if validation passes. Directly calling the service in a unit test does not go through this HTTP validation step.

## 5. Database mapping — Jakarta Persistence / JPA

| Annotation | Simple meaning | Garage example |
|---|---|---|
| `@Entity` | This class is persisted by JPA. | Enquiry. |
| `@Table` | Name the mapped table. | `enquiry`. |
| `@Id` | Identify the primary-key field. | UUID id. |
| `@GeneratedValue` | Ask the persistence provider to generate an ID. | UUID generation. |
| `@Column` | Describe a column mapping. | Length and column name. |
| `@Enumerated(EnumType.STRING)` | Store the enum name as text. | NEW rather than numeric ordinal 0. |
| `@PrePersist` | Run a callback before inserting a new entity. | Set createdAt and updatedAt. |
| `@PreUpdate` | Run a callback before an entity update. | Refresh updatedAt. |
| `@Version` | Track an entity version for optimistic locking. | Later: reject conflicting concurrent admin updates. |

`@Column(nullable = false)` does not replace input validation. With our `ddl-auto=validate` setting, changing the Java annotation does not alter PostgreSQL: Flyway migrations own schema changes. JPA callbacks do not run for direct SQL or bulk update queries.

## 6. Transactions and background work — Spring

| Annotation | Simple meaning | Important limit |
|---|---|---|
| `@Transactional` | Execute an operation within a transaction boundary. | By default runtime exceptions/errors cause rollback; checked exceptions need configured rules. |
| `@Async` | Execute through a configured asynchronous executor when enabled. | A background thread is not a durable queue; work can disappear on a crash. |
| `@Scheduled` | Invoke a task on a configured schedule when scheduling is enabled. | Each app replica can run it, so distributed work needs safe coordination. |

In the usual Spring proxy mode, calls must pass through the proxy for transactional/async behaviour. A method calling another annotated method on the same object does not normally activate a new interception.

Our service currently relies on repository save transactions so it can recover outside a failed insert transaction. Do not blindly add an outer transaction: re-evaluate boundaries when adding enquiry-plus-outbox atomic writes.

## 7. Lombok — generated code, not Spring

| Annotation | What it generates |
|---|---|
| `@Getter` | Getter methods. |
| `@Setter` | Setter methods; use selectively when changes need business rules. |
| `@RequiredArgsConstructor` | Constructor for uninitialised final fields and required non-null fields. Useful for dependency injection. |
| `@NoArgsConstructor` | Empty constructor. JPA needs a public/protected no-argument constructor. |
| `@AllArgsConstructor` | Constructor for fields. |
| `@Builder` | Builder methods to construct an object with named values. |
| `@Builder.Default` | Preserve a field default with a class-level builder. Our constructor-level builder already preserves status because status is not a constructor argument. |
| `@Data` | Getters, setters, equality, hash code and string representation. Avoid applying blindly to JPA entities or sensitive DTOs. |

Lombok generates code during compilation. Spring can then use the generated constructor; Lombok itself does not inject dependencies.

## 8. Testing annotations

| Annotation | Simple meaning |
|---|---|
| JUnit `@Test` | This method is a test. |
| `@SpringBootTest` | Load a Spring Boot application context; does not necessarily start a real HTTP server. |
| `@WebMvcTest` | Test the MVC layer with selected web components, normally mocking the service. |
| `@MockitoBean` | Replace or add a Spring bean with a Mockito mock in a test context. |
| Mockito `@Mock` | Create a mock for a unit test; not a real database repository. |
| Mockito `@InjectMocks` | Construct/inject the class under test using available mocks. |
| JUnit `@ExtendWith(MockitoExtension.class)` | Enable Mockito integration for the test. |

A controller test with a mocked service proves HTTP/validation behaviour, not a successful PostgreSQL insert. A service test with a mocked repository proves decisions, not SQL constraints. Use a PostgreSQL integration test for persistence.

## Follow one request

```text
@RestController + @PostMapping → choose the handler
@RequestBody                  → JSON becomes a DTO
@Valid                        → check DTO constraints
@Service                      → apply business rules
Repository / transaction      → persist through JPA
@Entity + @Column             → map Java fields to columns
@PrePersist                   → set insertion timestamps
ResponseEntity                → choose status and response body
```

## Practice

Which annotation validates the incoming request object, and which one maps a Java class to a database table?
