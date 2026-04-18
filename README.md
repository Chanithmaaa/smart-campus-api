# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W – Client-Server Architectures  
**University:** University of Westminster  
**Technology:** JAX-RS (Jersey 2.39.1) + Grizzly Embedded HTTP Server  
**Base URL:** `http://localhost:8080/api/v1`

---

## Table of Contents

1. [API Design Overview](#api-design-overview)
2. [Project Structure](#project-structure)
3. [Build & Run Instructions](#build--run-instructions)
4. [API Endpoints Reference](#api-endpoints-reference)
5. [Sample curl Commands](#sample-curl-commands)
6. [Conceptual Report – Question Answers](#conceptual-report--question-answers)

---

## API Design Overview

This API implements a RESTful backend for the University of Westminster's Smart Campus initiative. It manages two primary resources — Rooms and Sensors — along with a nested SensorReadings sub-resource that maintains a historical log of sensor measurements.

### Design Principles

- **Pure JAX-RS / Jersey** – no Spring Boot, no external database. All data is stored in thread-safe ConcurrentHashMap structures inside InMemoryDataStore.
- **Resource Hierarchy** – reflects the physical campus layout: Room → Sensor → SensorReading
- **HATEOAS** – every response includes _links so clients can navigate without consulting external documentation.
- **Versioned Entry Point** – all routes are prefixed with /api/v1 via @ApplicationPath.
- **Leak-Proof Error Handling** – custom ExceptionMapper implementations ensure no stack trace is returned to a client.
- **Observability** – a JAX-RS filter logs every request method/URI and response status code.

### Resource Model

```
/api/v1                                        <- Discovery / HATEOAS root
/api/v1/rooms                                  <- Room collection
/api/v1/rooms/{roomId}                         <- Individual room
/api/v1/sensors                                <- Sensor collection (supports ?type= filter)
/api/v1/sensors/{sensorId}                     <- Individual sensor
/api/v1/sensors/{sensorId}/readings            <- Reading history (sub-resource)
/api/v1/sensors/{sensorId}/readings/{rid}      <- Individual reading
```

---

## Project Structure

```
smart-campus-api/
├── pom.xml
├── README.md
└── src/main/java/com/westminster/smartcampus/
    ├── config/
    │   ├── ApiApplication.java
    │   └── Main.java
    ├── datastore/
    │   └── InMemoryDataStore.java
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── resource/
    │   ├── DiscoveryResource.java
    │   ├── SensorRoomResource.java
    │   ├── SensorResource.java
    │   └── SensorReadingResource.java
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── RoomNotEmptyMapper.java
    │   ├── LinkedResourceNotFoundException.java
    │   ├── LinkedResourceNotFoundMap.java
    │   ├── SensorUnavailableException.java
    │   ├── SensorUnavailableMapper.java
    │   └── GlobalExceptionMapper.java
    └── filter/
        └── LoggingFilter.java
```

---

## Build & Run Instructions

### Prerequisites

- Java 11 or higher
- Apache Maven 3.6+

### Step 1 – Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
```

### Step 2 – Build the project

```bash
mvn clean package
```

### Step 3 – Run the server

```bash
java -jar target/smart-campus-api-1.0.0.jar
```

### Step 4 – Verify it is running

```bash
curl http://localhost:8080/api/v1
```

---

## API Endpoints Reference

| Method | Path | Description | Code |
|--------|------|-------------|------|
| GET | /api/v1 | Discovery + HATEOAS links | 200 |
| GET | /api/v1/rooms | List all rooms | 200 |
| POST | /api/v1/rooms | Create a room | 201 |
| GET | /api/v1/rooms/{roomId} | Get room by ID | 200 |
| DELETE | /api/v1/rooms/{roomId} | Delete room | 200 |
| GET | /api/v1/sensors | List sensors (?type= filter) | 200 |
| POST | /api/v1/sensors | Register a sensor | 201 |
| GET | /api/v1/sensors/{sensorId} | Get sensor by ID | 200 |
| GET | /api/v1/sensors/{sensorId}/readings | Get reading history | 200 |
| POST | /api/v1/sensors/{sensorId}/readings | Add a reading | 201 |

---

## Sample curl Commands

### 1. Discover the API

```bash
curl -s http://localhost:8080/api/v1
```

### 2. Create a Room

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "LIB-301", "name": "Library Quiet Study", "capacity": 50}'
```

### 3. Register a Sensor with invalid roomId (triggers 422)

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "CO2-001", "type": "CO2", "status": "ACTIVE", "currentValue": 400.0, "roomId": "FAKE-999"}'
```

### 4. Register a Sensor with valid roomId

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "CO2-001", "type": "CO2", "status": "ACTIVE", "currentValue": 400.0, "roomId": "LIB-301"}'
```

### 5. Filter Sensors by Type

```bash
curl -s "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 6. Post a Reading

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 850.5}'
```

### 7. Get All Readings for a Sensor

```bash
curl -s http://localhost:8080/api/v1/sensors/CO2-001/readings
```

### 8. Try to Delete a Room with Sensors (triggers 409)

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 9. Delete an Empty Room

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "EMPTY-101", "name": "Empty Room", "capacity": 10}'

curl -s -X DELETE http://localhost:8080/api/v1/rooms/EMPTY-101
```

---

## Conceptual Report – Question Answers

> This section contains written answers to all conceptual questions in the coursework specification, organised by Part and Task exactly as they appear in the specification document.

---

## Part 1: Service Architecture & Setup

---

### Task 1.1 – Project & Application Configuration

**Question:** In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.

**Answer:**

By default, JAX-RS uses a **request-scoped lifecycle** for resource classes. This means the runtime — in this project, Jersey — creates a completely new instance of each resource class for every incoming HTTP request and discards it once the response has been sent. This behaviour is defined by the JAX-RS specification (JSR-370) and is intentional: it eliminates thread-safety problems that would arise if multiple concurrent requests shared the same mutable resource object and its instance fields.

However, this lifecycle decision creates a critical problem for in-memory data management. If data were stored as instance fields on the resource class (for example, `private HashMap<String, Room> rooms = new HashMap<>()`), those fields would be created fresh and then destroyed at the end of every single request. The system would lose all of its data between every API call, which is completely unacceptable for a functioning application.

To solve this, all shared mutable state in this implementation is stored in a dedicated class called `InMemoryDataStore`, using **static fields** backed by `ConcurrentHashMap` and `CopyOnWriteArrayList`. Because static fields belong to the class itself rather than to any individual instance, they persist across all requests for the entire lifetime of the JVM process — precisely what is required for an in-memory data store.

`ConcurrentHashMap` is used instead of a plain `HashMap` because, even though each resource instance is request-scoped and short-lived, the shared data store is accessed simultaneously by multiple threads (one thread is created per incoming request). A plain `HashMap` is not thread-safe: two threads calling `put()` at the same time can corrupt its internal hash table structure and silently lose data. `ConcurrentHashMap` uses internal segment-level locking to guarantee that each individual read and write operation is atomic and safe under concurrent access, preventing race conditions and data corruption without requiring explicit `synchronized` blocks throughout the application code.

---

### Task 1.2 – The Discovery Endpoint

**Question:** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?

**Answer:**

HATEOAS — Hypermedia As The Engine Of Application State — represents Level 3 of Richardson's Maturity Model, which is the highest achievable level of REST architectural maturity. The core principle is that API responses should not merely return data; they should also embed navigational links and available actions that tell the client where it can go and what it can do next. This transforms the API into a self-describing system at runtime, rather than one that requires the client to consult external or static documentation to know how to navigate it.

For example, when this API returns a newly registered sensor, the response includes a `_links` object containing the direct URL to the sensor itself, the URL to its readings history, and the URL to its parent room. The client never needs to construct or hardcode any of these paths — it simply follows the links provided by the server.

This approach provides several concrete benefits to client developers. First, it **reduces tight coupling** between client and server. Since clients navigate via embedded links rather than hardcoded URL patterns, if the server-side URL structure changes in a future version, clients that follow hypermedia links adapt automatically without requiring any code changes on the client side. Second, it dramatically improves **discoverability** — a developer working with the API for the first time can start at `GET /api/v1`, read the links in the response, and navigate progressively to every available resource, exactly like browsing a website with clickable links. Third, it eliminates the need to rely on **static documentation** which becomes outdated and inaccurate as the API evolves. Since links are embedded in live responses, they always reflect the current, accurate state of the API. Finally, HATEOAS enables smarter and more resilient clients that make navigation decisions dynamically at runtime based on what the server declares is currently available and accessible.

---

## Part 2: Room Management

---

### Task 2.1 – Room Resource Implementation

**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.

**Answer:**

Returning **only IDs** (for example, `["LIB-301", "ENG-202", "SCI-101"]`) produces an extremely lightweight initial response that consumes minimal bandwidth on the first request. This could be advantageous if the collection is enormous and the client genuinely only needs to check which rooms exist. However, it forces the client to make N additional HTTP requests — one separate request for each room ID — in order to retrieve any useful information about those rooms. This is the well-known **N+1 problem**: the cumulative network overhead and total latency from N sequential round-trips will typically far exceed the cost of a single, moderately larger initial response. This problem is especially severe on high-latency connections or mobile networks. The client-side code also becomes considerably more complex, needing to collect IDs, manage N concurrent or sequential requests, handle partial failures, and assemble the final result.

Returning **full room objects** delivers all the information the client needs in a single network round-trip, dramatically reducing total latency and greatly simplifying client-side logic. The trade-off is a larger response payload. However, in this system, room objects are compact — containing only an id, name, capacity integer, and a list of sensor ID strings — meaning the payload overhead is negligible while the benefit of eliminating additional requests is substantial.

A more advanced design pattern used in production APIs is to return **summary objects** in the list — a reduced subset of the most important fields (for example, only id and name) combined with a `self` hypermedia link so the client can fetch complete details on demand, only for the specific rooms it actually needs. For this implementation, complete room objects are returned in the list response because the payloads are small and eliminating the N+1 problem entirely is the priority for usability.

---

### Task 2.2 – Room Deletion & Safety Logic

**Question:** Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.

**Answer:**

Yes, the DELETE operation is fully idempotent in this implementation, correctly honouring the definition provided in RFC 9110 (HTTP Semantics). Idempotency means that sending the same request any number of times produces exactly the same **server state** as sending it once — the repeated requests have no additional side effects beyond the first.

In this implementation, the sequence of events across multiple identical DELETE requests is as follows. The **first DELETE** on a room that has no sensors assigned to it locates the room in `InMemoryDataStore.rooms`, removes it, and returns `200 OK` along with a JSON confirmation message. The **second DELETE** using the exact same room ID searches the data store, finds no entry because the room was already removed by the first call, and returns `404 Not Found`. Any **third, fourth, or subsequent** DELETE request on the same room ID also returns `404 Not Found`.

The critical point is that the **server state** is identical after the first call and after every subsequent call — in all cases, the specified room does not exist in the data store. The HTTP specification does not require that repeated idempotent requests return the same response code each time; it only requires that they produce the same side-effects on the server's persistent state. This idempotent property is extremely important for reliability in distributed systems and unreliable networks: if a client sends a DELETE request and then loses its network connection before receiving the response, it cannot know whether the request succeeded or not. Because DELETE is idempotent, the client can safely retry the request as many times as needed without any risk of accidentally deleting a different resource, corrupting data, or triggering unintended consequences.

---

## Part 3: Sensor Operations & Linking

---

### Task 3.1 – Sensor Resource & Integrity

**Question:** We explicitly use the `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?

**Answer:**

The `@Consumes(MediaType.APPLICATION_JSON)` annotation establishes a formal contract at the framework level: the JAX-RS runtime will only route an incoming request to that particular resource method if the request's `Content-Type` header value matches `application/json`. This validation check is performed during the **request dispatching phase** — before any resource method body is ever executed and before any attempt is made to parse or deserialise the incoming request body.

When a client sends a POST request with a mismatched `Content-Type` header such as `text/plain` or `application/xml`, the JAX-RS runtime evaluates all registered resource methods against the request's media type. Finding no method that declares it can consume that particular media type, the runtime immediately responds with **HTTP 415 Unsupported Media Type**. The resource method body is never executed, the request body is never read or parsed, and no application-level logic runs at all. The response is generated entirely at the framework level.

This built-in content negotiation mechanism serves several important purposes in API design. It protects the application from receiving data in an unexpected or potentially malicious format without writing any defensive code inside the resource method. It makes the API's accepted input formats explicit and machine-enforceable rather than just documenting them. It also enables the same URL endpoint to support multiple different input formats simultaneously by providing separate resource methods each annotated with a different `@Consumes` value — the framework automatically routes each incoming request to the correct method based on its `Content-Type` header, with no conditional logic required in the application code. This is a clean, declarative approach to input validation that is a core design feature of the JAX-RS framework.

---

### Task 3.2 – Filtered Retrieval & Search

**Question:** You implemented this filtering using `@QueryParam`. Contrast this with an alternative design where the type is part of the URL path (e.g., `/api/v1/sensors/type/CO2`). Why is the query parameter approach generally considered superior for filtering and searching collections?

**Answer:**

The distinction between these two approaches is rooted in what each part of a URL is semantically designed to communicate. In REST architecture, **path segments** are intended to identify a specific resource or a named collection. The **query string** is the correct mechanism for providing optional, supplementary parameters that modify how an existing collection is retrieved — such as filtering by a field value, sorting by a criterion, paginating results, or performing a keyword search.

The path-based design `/api/v1/sensors/type/CO2` is semantically incorrect because it falsely implies that `type` is a sub-resource of the sensors collection and that `CO2` is a further sub-resource within that. Neither is true — there is no discrete, identifiable resource called "type/CO2". Beyond this semantic issue, path-based filtering becomes increasingly awkward when multiple filter criteria are needed. Expressing "all CO2 sensors that are currently ACTIVE" would require a URL like `/sensors/type/CO2/status/ACTIVE`, which grows longer and more difficult to read with every additional filter. Adding optional filters — where some criteria may or may not be present — becomes especially complex and requires multiple endpoint definitions.

The query parameter approach `/api/v1/sensors?type=CO2` is semantically correct and reads clearly as "retrieve the sensors collection with a filter applied: type equals CO2". Multiple filters compose naturally and remain highly readable: `/sensors?type=CO2&status=ACTIVE`. Because query parameters are inherently optional, `GET /sensors` with no query parameters naturally returns the full unfiltered collection without requiring any additional code or endpoint. Query parameters are natively supported and understood by all HTTP client libraries, API documentation tools, browser developer tools, testing tools such as Postman, and HTTP caching infrastructure. The guiding rule in REST API design is clear: use path parameters to **identify** a specific resource (e.g., `/sensors/{sensorId}`), and use query parameters to **filter, sort, or paginate** the representation of a collection.

---

## Part 4: Deep Nesting with Sub-Resources

---

### Task 4.1 – The Sub-Resource Locator Pattern

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path in one massive controller class?

**Answer:**

The Sub-Resource Locator pattern is a JAX-RS design technique in which a resource method carries no HTTP verb annotation such as `@GET`, `@POST`, or `@DELETE`. Instead, it is annotated only with a `@Path` expression and returns an instance of another Java class. The JAX-RS runtime recognises this as a locator method, calls it to obtain the delegate object, and then dispatches the actual HTTP verb from the incoming request to the appropriate method on that returned object. In this project, `SensorResource` contains a method annotated with `@Path("/{sensorId}/readings")` that creates and returns a `SensorReadingResource` instance, which then handles the GET and POST operations on the readings sub-resource.

The architectural benefits of this pattern are substantial, particularly as APIs grow in size and complexity. First, it enforces **single responsibility**: `SensorResource` is concerned only with sensor-level operations such as registering and retrieving sensors, while `SensorReadingResource` is concerned exclusively with reading-level operations. Each class has a clear, bounded responsibility. Second, it prevents **uncontrolled class growth**: without this pattern, every nested path — `/sensors/{id}/readings`, `/sensors/{id}/readings/{rid}`, and any further nesting — would need to be defined directly inside `SensorResource`. In a real-world campus management API with many resource types, this would produce an enormous, unmaintainable controller class with hundreds of methods and unclear boundaries. Third, it enables **independent unit testing**: `SensorReadingResource` can be instantiated and tested in complete isolation simply by passing a `sensorId` to its constructor, with no need to simulate the full HTTP request routing pipeline. Fourth, the locator method itself serves as a natural **validation gateway**: it can verify that the parent sensor exists in the data store before the sub-resource is even instantiated, providing a centralised, non-duplicated place for this cross-cutting validation logic. Without this pattern, that validation would need to be repeated in every single reading method. In APIs with dozens of nested resource hierarchies, the Sub-Resource Locator pattern is an essential tool for keeping the codebase modular, readable, and maintainable.

---

### Task 4.2 – Historical Data Management

This task has no conceptual question. The implementation provides `GET /sensors/{sensorId}/readings` to retrieve the full reading history for a sensor, and `POST /sensors/{sensorId}/readings` to append a new reading. A successful POST triggers an automatic side-effect update to the `currentValue` field on the parent `Sensor` object in `InMemoryDataStore`, ensuring data consistency across the API.

---

## Part 5: Advanced Error Handling, Exception Mapping & Logging

---

### Task 5.1 – Resource Conflict (409 Conflict)

This task has no conceptual question. The implementation provides a custom `RoomNotEmptyException` class and a corresponding `RoomNotEmptyMapper` annotated with `@Provider`. The mapper intercepts this exception and returns an HTTP 409 Conflict response with a structured JSON body explaining that the room cannot be deleted while sensors remain assigned to it.

---

### Task 5.2 – Dependency Validation (422 Unprocessable Entity)

**Question:** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

**Answer:**

To understand why 422 is more appropriate in this scenario, it is essential to clearly understand what each status code is designed to communicate.

**HTTP 404 Not Found** is a routing-level status code. It means "the URL or endpoint path you requested does not exist on this server." It signals that the server was unable to locate any handler or resource at the given URI, and the request could not be routed at all.

**HTTP 422 Unprocessable Entity**, defined in RFC 4918 and reaffirmed in RFC 9110, means something categorically different: "the server understood the request, successfully routed it to the correct endpoint, the submitted JSON body was syntactically well-formed and parseable, but the semantic content of that payload cannot be processed as submitted."

When a client POSTs a new sensor with `"roomId": "GHOST-999"` in the request body, the request URI `/api/v1/sensors` is completely valid — the endpoint exists, it is registered, and the request is successfully routed to `SensorResource`. The JSON body deserialises without errors. The sole problem is that the value assigned to the `roomId` field inside the body references a room that does not exist anywhere in the system. This is a **referential integrity failure** or **semantic validation failure**. It has nothing whatsoever to do with whether the endpoint was found.

Returning HTTP 404 in this scenario would actively mislead the client developer into believing that the `/api/v1/sensors` endpoint itself does not exist on the server, which is factually incorrect. The developer would waste time checking their URL, potentially concluding their client is misconfigured, when in reality the endpoint is perfectly reachable. HTTP 422 communicates the actual situation accurately and precisely: "Your request was received, routed, and parsed successfully, but the data inside it references a resource that does not exist." This gives the developer clear, actionable information — they need to create the referenced room before attempting to register a sensor that belongs to it. Choosing 422 over 404 is therefore the semantically correct, developer-friendly, and specification-compliant choice for any scenario where a payload passes syntactic validation but fails business-rule or referential-integrity checks.

---

### Task 5.3 – State Constraint (403 Forbidden)

This task has no conceptual question. The implementation provides a custom `SensorUnavailableException` class and a corresponding `SensorUnavailableMapper` annotated with `@Provider`. The mapper intercepts this exception and returns an HTTP 403 Forbidden response with a structured JSON body, triggered when a POST reading is attempted on a sensor whose status field is set to `MAINTENANCE`.

---

### Task 5.4 – The Global Safety Net (500 Internal Server Error)

**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?

**Answer:**

Exposing raw Java stack traces to external API consumers constitutes **information disclosure**, which is listed among the OWASP Top 10 most critical web application security risks. A stack trace effectively provides an attacker with a detailed internal map of the application's architecture and implementation — without requiring any special privileges, credentials, or access. The specific categories of sensitive information that can be extracted from a Java stack trace include the following.

**Internal package and class names**: every frame in a stack trace includes the full qualified Java class name, such as `com.westminster.smartcampus.resource.SensorResource`. This immediately reveals the application's package structure, naming conventions, and which specific classes are responsible for handling critical operations such as authentication, data access, or business rule enforcement. This information provides a precise starting point for crafting targeted injection attacks or exploiting specific class vulnerabilities.

**Third-party library names and exact version numbers**: stack traces explicitly display third-party library class paths, for example `com.fasterxml.jackson.databind.exc.MismatchedInputException` or `org.glassfish.jersey.server.internal.routing.RoutingStage`. An attacker can immediately look up these library names and version identifiers in public CVE (Common Vulnerabilities and Exposures) databases to identify known security vulnerabilities present in the exact versions deployed and craft targeted exploits accordingly.

**Application execution flow and business logic**: the complete ordered call stack reveals exactly how the application processes requests internally — the sequence of methods called, the conditions under which specific code paths are reached, and where validation occurs. This provides an attacker with a blueprint for identifying which inputs will bypass validation logic, trigger specific exception conditions, or reach sensitive code branches.

**File system paths and server deployment information**: depending on the JVM version and application server, stack traces may include absolute file system paths revealing the server's directory structure, deployment location, and internal organisation. This information can assist in crafting path traversal attacks, local file inclusion exploits, or server-side request forgery attacks.

**Systematic error-based reconnaissance**: a methodical attacker can deliberately submit a series of carefully crafted malformed requests targeting different endpoints and triggering different types of errors. By collecting and analysing the resulting stack traces, the attacker can progressively build a comprehensive picture of the entire application's internal architecture without ever requiring access to source code, configuration files, or system credentials.

The `GlobalExceptionMapper` in this implementation mitigates all of these risks by implementing `ExceptionMapper<Throwable>` to intercept every possible unhandled exception at the top level. It logs the complete exception details and stack trace server-side only — accessible exclusively to authorised system administrators through server logs — and returns to the external caller only a minimal, generic HTTP 500 JSON response object containing no internal information whatsoever.

---

### Task 5.5 – API Request & Response Logging Filters

**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?

**Answer:**

Cross-cutting concerns are system-wide aspects of an application that affect many different components simultaneously but do not logically belong to any single component. Logging, authentication and authorisation checks, CORS header management, request rate limiting, and distributed tracing are all well-known examples. Implementing these concerns using JAX-RS filters rather than manually inserting log statements into individual resource methods provides important advantages across several dimensions.

**Single centralised point of control**: the `LoggingFilter` class, implementing both `ContainerRequestFilter` and `ContainerResponseFilter`, provides one single location that handles logging for every request and every response across the entire API. If the logging behaviour needs to change — for example, to add a unique request correlation ID, include the authenticated user's identity, record response times, or change the log level — the modification is made in exactly one file and immediately takes effect for every endpoint without touching any resource class. With manual `Logger.info()` statements scattered across dozens of resource methods, an equivalent change would require individually locating and editing every method, a process that is time-consuming, inconsistent, and extremely error-prone.

**Guaranteed and comprehensive execution**: filters registered with the JAX-RS runtime are guaranteed to execute for every matched incoming request, without any possibility of omission. When a new resource class or endpoint method is added to the API at any point in the future, it is automatically covered by all registered filters from the moment it is deployed — no additional action is required from the developer. Manual log statements placed inside resource methods are trivially easy to forget, especially when a developer is focused on implementing business logic under time pressure, during large refactoring efforts, or when onboarding new team members who may not know the convention.

**Clean separation of concerns**: resource methods should be focused entirely on expressing business logic in a clear, readable, and concise manner. Embedding infrastructure-level logging statements inside business logic methods mixes two fundamentally different responsibilities in the same code unit, increases the cognitive load for developers reading or maintaining the code, lengthens methods unnecessarily, and makes unit testing more difficult because tests must account for and mock the logging infrastructure alongside the business logic.

**Composability, ordering, and independent evolution**: JAX-RS supports registering multiple filters simultaneously and controlling their precise execution order using the `@Priority` annotation. This architecture makes it straightforward to add an authentication filter that runs before the logging filter, a request validation filter that runs after authentication but before routing, or a response compression filter that runs after content assembly — all without modifying any existing resource class or introducing coupling between concerns. Each filter evolves independently and can be added, removed, or replaced without affecting the others. This declarative, composable approach to infrastructure concerns is the established industry-standard pattern for building maintainable, testable, and observable production-grade web service APIs.
