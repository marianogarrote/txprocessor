# Transaction Processor

**1\. Overview**  
This project implements a command-side CQRS service for processing transactions with optional parent relationships. The focus is on domain correctness, reactive execution, and explicit validation of illegal transaction graphs.  
The system validates:

* Self-parent transactions
* Circular transaction graphs

It is intentionally scoped to the command side: no projections or read models are included.

**2\. Hexagonal Architecture**  
API (WebFlux Controller)  
↓  
Application / Use Case (ProcessTransactionCommand)  
↓  
Domain (Transaction Aggregate)  
↓  
Ports (TransactionRepositoryPort)  
↓  
Adapters (R2DBC / InMemory)

**2.1. Domain Model**  
Transaction is a pure domain aggregate:
* Immutable
* Enforces invariants in the constructor
* Detects illegal graph structures eagerly

Enforced Invariants
* A transaction cannot reference itself as parent
* A transaction graph cannot contain cycles

Violations raise domain exceptions:

* SelfParentTransactionException
* CircularTransactionGraphException

**Reactive & Transactional Model**

* Built using Spring WebFlux
* No blocking calls (block, subscribe) are used inside the reactive pipeline.
* Transactional boundaries are defined at the command level using reactive transactions (when enabled by adapter \- in this case it is an in memory custom implementation for challenge purpose)

**3\. API Contract**  
**Command Endpoint**  
`PUT /api/v1/transactions/{transaction\_id}`  
Request  
`{  
"amount": 100.50,  
"type": "cars",  
"parent\_id": null  
}`  
Success Response  
`{ "status": "ok"}`

Error Cases
* Self parent reference, Circular graph detected, Validation error: 400 BAD REQUEST

**4\. Testing Strategy**  
**Domain Tests**

* Validate invariants at aggregate construction time
* No mocks

**Application Tests**

* Use-case level tests
* Mocked ports

**Reactive Tests**

* StepVerifier
* Explicit coverage of empty Monos and error paths

**5\. Docker**  
The Docker image is based on a JVM runtime image (e.g. eclipse-temurin) produced via multi-stage build.  
**Build**  
`./mvnw clean package`  
**Run**  
`docker build \-t txprocessor .`  
`docker run \-p 8080:8080 txprocessor`

**6\. Design Decisions**  
**6.1. Why domain validation instead of service-level**

* Strong consistency guarantees
* Impossible-to-ignore invariants
* Testable without infrastructure

**6.2. Possible Improvements**

* Add query-side projections
* Event publishing (Kafka) on successful commands
* Idempotency keys
* Rate limiting & circuit breakers
* OpenAPI documentation: it is already implemented (`TransactionApi`) but due to Spring Boot version (`4.0.1`) it is not compatible right now.

**7\. Tech Stack**

* Java 21
* Spring Boot WebFlux \+ Reactor
* Swagger/OpenAPI for Spring WebFlux (disabled due to lack of backward compatibility for Spring Boot version)
* JUnit 5 \+ Mockito \+ AssertJ
* Docker (Multi-stage Build): This project is fully containerized using a multi-stage Docker build in order to:
* Minimize final image size
* Avoid shipping build tools (Maven, source code) to production
* Improve security and startup performance
* Align with cloud-native deployment best practices

**8\. Out of Scope by Design**  
**REST Semantics vs Challenge Specification**  
The challenge specification proposes the following API interactions:  
`PUT /transactions/10 { "amount": 5000, "type": "cars" }`  
`GET /transactions/types/cars → \[10\]`  
`GET /transactions/sum/11	→ { "sum": 15000 }`

While these examples are implemented as requested, they present conceptual inconsistencies with RESTful API design principles.

**Why This Is Not Fully RESTful**  
1\. `PUT /transactions/{id}` Is Semantically Misused  
In REST semantics PUT implies idempotent replacement of a known resource  
In this challenge it is closer to a command invocation, not a resource update.  
Although `PUT /transactions/10` violates classic REST constraints but is common in CQRS command APIs.  
2\. Imperative endpoints  
Endpoints such as the following ones represent a derived state, not a primary resource.  
GET /transactions/sum/11  
GET /transactions/types/cars  
In REST terms, this is closer to:  
- `GET /transactions/11/sum`
- `GET /transactions?type=cars`

The current design exposes a calculation endpoint, not a resource representation.  
For the search by type case, it breaks extensibility for additional filters


**9\.  API Testing Collection (Bruno)**  
For convenience and reproducibility, a Bruno API collection has been included in the repository:  `docs/tx-processor.json`  
This collection contains a curated set of requests covering multiple interaction scenarios with the exposed endpoints, including:

* Valid transaction creation flows
* Parent–child transaction chains
* Circular and self-parent validation errors
* Type-based searches
* Transaction subgraph sum calculation

The goal of this collection is to allow reviewers to quickly validate API behavior without writing custom scripts or test clients, while also serving as an executable form of API documentation.

Bruno was chosen due to its:

* Lightweight, offline-first approach
* Git-friendly JSON format
* No vendor lock-in or runtime dependencies

The collection can be imported directly into Bruno and executed against a local instance.