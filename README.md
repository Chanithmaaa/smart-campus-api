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

You should see:

```
INFO: Smart Campus API running at http://localhost:8080/api/v1
INFO: Press ENTER to stop.
```

### Step 4 – Verify it is running

```bash
curl http://localhost:8080/api/v1
```

---

## API Endpoints Reference

| Method | Path | Description | Success Code |
|--------|------|-------------|--------------|
| GET | `/api/v1` | API discovery + HATEOAS links | 200 |
| GET | `/api/v1/rooms` | List all rooms | 200 |
| POST | `/api/v1/rooms` | Create a new room | 201 |
| GET | `/api/v1/rooms/{roomId}` | Get room by ID | 200 |
| DELETE | `/api/v1/rooms/{roomId}` | Delete room (blocked if sensors exist) | 200 |
| GET | `/api/v1/sensors` | List all sensors (optional `?type=` filter) | 200 |
| POST | `/api/v1/sensors` | Register a sensor (validates roomId) | 201 |
| GET | `/api/v1/sensors/{sensorId}` | Get sensor by ID | 200 |
| GET | `/api/v1/sensors/{sensorId}/readings` | Get all readings for a sensor | 200 |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add a new reading | 201 |
| GET | `/api/v1/sensors/{sensorId}/readings/{readingId}` | Get a specific reading | 200 |

### Error Responses

| Scenario | HTTP Status |
|----------|-------------|
| Room has sensors, cannot delete | 409 Conflict |
| Sensor roomId does not exist | 422 Unprocessable Entity |
| Sensor is in MAINTENANCE | 403 Forbidden |
| Resource not found | 404 Not Found |
| Wrong Content-Type sent | 415 Unsupported Media Type |
| Any unexpected server error | 500 Internal Server Error |

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
  -d '{"id": "LIB-301", "name": "Library Quiet Study", "capacity": 80}'
```

### 3. Register a Sensor with invalid roomId (triggers 422)

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "CO2-999", "type": "CO2", "status": "ACTIVE", "currentValue": 400.0, "roomId": "FAKE-999"}'
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
  -d '{"value": 455.7}'
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
curl -s -X DELETE http://localhost:8080/api/v1/rooms/EMPTY-101
```

---

## Conceptual Report – Question Answers

> This section contains the written answers to all conceptual questions posed in the coursework specification, organised by Part and Task exactly as they appear in the specification document.

---

## Part 1: Service Architecture & Setup

---

### Task 1.1 – Project & Application Configuration

**Question:** In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.

**Answer:**

By default, JAX-RS creates a **new resource instance per request** (request-scoped). This means instance fields are destroyed after every request and cannot hold shared data.

To solve this, all data is stored as **static fields** in `InMemoryDataStore` using `ConcurrentHashMap` and `CopyOnWriteArrayList`. Static fields persist for the entire JVM lifetime. `ConcurrentHashMap` is used instead of `HashMap` because multiple threads access the store simultaneously — one per request — and it guarantees thread-safe operations without `synchronized` blocks, preventing race conditions and data loss.

---

### Task 1.2 – The Discovery Endpoint

**Question:** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?

**Answer:**

HATEOAS (Hypermedia As The Engine Of Application State) means API responses embed navigation links alongside data, making the API self-describing at runtime.

This benefits client developers because clients follow embedded links rather than hardcoding URLs, so they automatically adapt if URLs change. Developers can explore the entire API by starting at `GET /api/v1` and following links — like browsing a website. Unlike static documentation, live responses are always accurate and never become outdated.

---

## Part 2: Room Management

---

### Task 2.1 – Room Resource Implementation

**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.

**Answer:**

Returning **only IDs** reduces initial payload size but forces N additional requests to fetch useful data — the N+1 problem — greatly increasing total latency and complicating client logic.

Returning **full objects** delivers everything in one round-trip, reducing latency and simplifying the client. Since room objects are small in this system, full objects are returned to eliminate the N+1 problem entirely.

---

### Task 2.2 – Room Deletion & Safety Logic

**Question:** Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.

**Answer:**

Yes, DELETE is idempotent per RFC 9110. The **first call** removes the room and returns `200 OK`. Every **subsequent call** finds no room and returns `404 Not Found`. The server state — room absent — is identical after every call, which satisfies the definition of idempotency. This means clients can safely retry a DELETE without risk of unintended side effects.

---

## Part 3: Sensor Operations & Linking

---

### Task 3.1 – Sensor Resource & Integrity

**Question:** We explicitly use the @Consumes (MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?

**Answer:**

JAX-RS checks the `Content-Type` header during request dispatching, before the method body executes. If it does not match `application/json` (e.g., `text/plain` is sent), JAX-RS immediately returns **HTTP 415 Unsupported Media Type** and the resource method is never invoked. This enforces the API contract automatically at the framework level without any defensive code inside the method.

---

### Task 3.2 – Filtered Retrieval & Search

**Question:** You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/v1/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?

**Answer:**

Path-based filtering (`/sensors/type/CO2`) is semantically wrong — it implies `type` is a sub-resource, which it is not. It also becomes unwieldy with multiple filters.

`@QueryParam` (`/sensors?type=CO2`) is semantically correct — it means "give me the sensors collection, filtered by type." Multiple filters compose naturally (`?type=CO2&status=ACTIVE`), parameters are optional by default, and query strings are supported by all HTTP tools. The rule is: path segments **identify** resources; query strings **filter** collections.

---

## Part 4: Deep Nesting with Sub-Resources

---

### Task 4.1 – The Sub-Resource Locator Pattern

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive controller class?

**Answer:**

The Sub-Resource Locator pattern lets a resource method return a delegate class instead of handling the request directly. Here, `SensorResource` returns a `SensorReadingResource` for the `/readings` path, and JAX-RS dispatches the actual HTTP method to it.

Benefits: **single responsibility** — each class handles one concern; **no class bloat** — avoids one massive controller with hundreds of methods; **independent testability** — `SensorReadingResource` can be unit-tested by constructing it directly; **centralised validation** — the locator verifies the sensor exists before the sub-resource is created.

---

### Task 4.2 – Historical Data Management

This task has no conceptual question. The implementation provides `GET /sensors/{sensorId}/readings` to retrieve reading history and `POST /sensors/{sensorId}/readings` to add a new reading. A successful POST also updates the `currentValue` field on the parent `Sensor` object to ensure data consistency across the API.

---

## Part 5: Advanced Error Handling, Exception Mapping & Logging

---

### Task 5.1 – Resource Conflict (409 Conflict)

This task has no conceptual question. `RoomNotEmptyException` is thrown when DELETE is attempted on a room that still has sensors. `RoomNotEmptyMapper` (`@Provider`) maps it to HTTP 409 Conflict with a structured JSON error body.

---

### Task 5.2 – Dependency Validation (422 Unprocessable Entity)

**Question:** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

**Answer:**

**HTTP 404** means the endpoint URL was not found — a routing error. **HTTP 422** means the endpoint was found, the JSON was valid, but the data inside it is semantically incorrect.

When a sensor is posted with a non-existent `roomId`, the URL `/api/v1/sensors` is perfectly reachable — returning 404 would wrongly tell the client the endpoint does not exist. HTTP 422 correctly communicates: "your request was understood and parsed, but the referenced resource does not exist." This gives developers accurate, actionable feedback to fix their data rather than their URL.

---

### Task 5.3 – State Constraint (403 Forbidden)

This task has no conceptual question. `SensorUnavailableException` is thrown when a POST reading is attempted on a `MAINTENANCE` sensor. `SensorUnavailableMapper` (`@Provider`) maps it to HTTP 403 Forbidden with a structured JSON body.

---

### Task 5.4 – The Global Safety Net (500)

**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?

**Answer:**

Exposing stack traces is an **information disclosure** vulnerability (OWASP Top 10). An attacker can extract:

- **Class and package names** — reveals application structure for targeted attacks.
- **Library names and versions** — allows searching CVE databases for known exploits.
- **Execution flow** — shows how data is processed, helping craft inputs that bypass validation.
- **File system paths** — can assist path traversal attacks.

`GlobalExceptionMapper` prevents this by catching all `Throwable` exceptions, logging details server-side only, and returning a generic HTTP 500 JSON with no internal information exposed.

---

### Task 5.5 – API Request & Response Logging Filters

**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?

**Answer:**

JAX-RS filters provide key advantages over manual logging:

- **Single point of control** — change log format once and it applies to every endpoint automatically.
- **Guaranteed execution** — new endpoints are covered immediately; manual statements are easy to forget.
- **Separation of concerns** — resource methods stay focused on business logic, not infrastructure.
- **Composability** — multiple filters chain and order via `@Priority` without touching resource classes.
