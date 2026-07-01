# Enterprise Product Registry — Spring Boot

A Spring Boot 3 port of the [enterprise one](../enterprise%20one/README.md) multi-tier product registration application. The business logic and test suite are identical; the infrastructure layer is replaced by Spring Boot's IoC container, auto-configuration, and build tooling.

---

## Why Spring Boot over Plain Java

| Concern | Plain Java (`enterprise one`) | Spring Boot (`enterprise spring`) |
|---------|------------------------------|-----------------------------------|
| **Dependency injection** | Manual wiring in `Main.java` | `@Service`, `@Repository`, `@Component` — container manages the graph |
| **Configuration** | Hard-coded values | Externalised in `application.properties`; overridable via env vars or CLI args at runtime |
| **Logging** | Logback XML wired manually | Auto-configured; controlled by a single `logging.level.*` property |
| **Build** | Maven Shade plugin (manual manifest) | `spring-boot-maven-plugin` — one `mvn package` produces a runnable fat JAR with no extra config |
| **Dependency management** | Each version declared individually | Spring Boot BOM pins all compatible versions; you declare intent, not versions |
| **Testing** | `spring-boot-starter-test` absent; JUnit + Mockito added manually | `spring-boot-starter-test` bundles JUnit 5, Mockito, AssertJ, Hamcrest in one dependency |
| **Lifecycle hooks** | `main()` drives everything | `CommandLineRunner` / `ApplicationRunner` integrate cleanly with the context lifecycle |
| **Extensibility** | Adding a REST layer requires manual Servlet wiring | Add `spring-boot-starter-web` to `pom.xml` — HTTP endpoints work immediately |
| **Health / Metrics** | Not available | Add `spring-boot-starter-actuator` for `/actuator/health`, `/actuator/metrics`, etc. |
| **Profile support** | None | `application-dev.properties`, `application-prod.properties` — switch with `--spring.profiles.active=prod` |

### Key advantages in detail

**Auto-configuration** — Spring Boot inspects the classpath and wires sensible defaults automatically. Adding a new starter (web, data-jpa, security) configures the entire stack without touching existing code.

**Externalised configuration** — every value in `application.properties` can be overridden by an environment variable or a command-line argument without recompilation. This is essential for containerised deployments where secrets and hostnames vary per environment.

**Production-ready out of the box** — the Actuator starter exposes liveness, readiness, and metrics endpoints that Kubernetes and load balancers rely on, with zero boilerplate.

**Ecosystem and community** — Spring Boot has the broadest Java ecosystem integration (databases, messaging, security, cloud). Moving from a console app to a full microservice requires only new starters, not a rewrite.

**Consistent testing support** — `@SpringBootTest` loads the full context for integration tests; `@ExtendWith(MockitoExtension.class)` stays for unit tests. Both patterns work side-by-side in the same project.

---

## Architecture

```
com.bells
├── exception       BusinessRuleViolationException
├── repository      ProductRepository   (@Repository — data layer)
├── service         ProductService      (@Service     — business layer)
├── runner          ProductRunner       (@Component, CommandLineRunner — presentation layer)
└── ProductApplication   @SpringBootApplication entry point
```

Spring Boot discovers and wires every `@Component`, `@Service`, and `@Repository` automatically via component scanning rooted at the `com.bells` package.

---

## Prerequisites

| Tool | Minimum version |
|------|----------------|
| Java JDK | 21 |
| Apache Maven | 3.9 |
| Docker | 24 (optional) |

---

## Project Structure

```
enterprise spring/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/bells/
    │   │   ├── ProductApplication.java
    │   │   ├── exception/
    │   │   │   └── BusinessRuleViolationException.java
    │   │   ├── repository/
    │   │   │   └── ProductRepository.java
    │   │   ├── service/
    │   │   │   └── ProductService.java
    │   │   └── runner/
    │   │       └── ProductRunner.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/bells/service/
            └── ProductServiceTest.java
```

---

## Running Locally

### 1. Compile and run tests

```bash
mvn test
```

Expected output:
```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 2. Package the executable fat JAR

```bash
mvn package -DskipTests
```

Output JAR:
```
target/enterprise-spring-product-registry-1.0.0.jar
```

### 3. Run the application

```bash
java -jar target/enterprise-spring-product-registry-1.0.0.jar
```

Override a property at runtime without recompilation:

```bash
java -jar target/enterprise-spring-product-registry-1.0.0.jar \
     --logging.level.com.bells=DEBUG
```

---

## Console Commands

| Command | Description |
|---------|-------------|
| `add <name>` | Register a new product by name |
| `list` | Display all registered products |
| `exit` | Shut down the application |

### Example session

```
=== Spring Boot Product Registry ===
Commands: add <name> | list | exit
> add Widget
[OK] Registered: Widget
> add Widget
[RULE VIOLATION] Product 'Widget' is already registered.
> add
[RULE VIOLATION] Product name must not be blank.
> list
[INFO] Products (1):
  1. Widget
> exit
Shutting down. Goodbye.
```

---

## Running with Docker

### Build the image

```bash
docker build -t spring-product-registry .
```

| Stage | Base image | Purpose |
|-------|-----------|---------|
| `builder` | `maven:3.9.8-eclipse-temurin-21-alpine` | Downloads dependencies and packages the fat JAR |
| `runtime` | `eclipse-temurin:21-jre-alpine` | Runs the JAR as a non-root user (`appuser`) |

### Run the container

```bash
docker run -it spring-product-registry
```

Override application properties via environment variables:

```bash
docker run -it \
  -e LOGGING_LEVEL_COM_BELLS=DEBUG \
  spring-product-registry
```

---

## Configuration Reference

All properties in `application.properties` can be overridden by environment variable (replace `.` with `_` and uppercase) or `--key=value` CLI argument.

| Property | Default | Description |
|----------|---------|-------------|
| `spring.application.name` | `enterprise-product-registry` | Application name shown in logs |
| `spring.main.banner-mode` | `off` | Suppresses the Spring Boot ASCII banner |
| `logging.pattern.console` | timestamp + thread + level + logger + message | Log line format |
| `logging.level.com.bells` | `INFO` | Root log level for application classes |

---

## Tests

`ProductServiceTest` uses JUnit 5 with the Mockito extension. Spring Boot is **not** loaded in unit tests — `ProductRepository` is mocked, keeping tests fast and isolated.

| Test | Verifies |
|------|---------|
| `registerProduct_successfulSave` | `save()` called once; `exists()` checked first |
| `registerProduct_blankName_throws…` | Exception message correct; repository never touched |
| `registerProduct_nullName_throws…` | Null treated identically to blank |
| `registerProduct_duplicateName_throws…` | `save()` never called when `exists()` returns `true` |
| `registerProduct_trimsWhitespaceBeforeSave` | `"  Widget  "` persists as `"Widget"` |

---

## Extending to a REST API

Because Spring Boot manages wiring, converting this console app to a REST service requires no changes to the service or repository layers:

1. Add `spring-boot-starter-web` to `pom.xml`.
2. Create a `@RestController` that injects `ProductService` and exposes `POST /products` and `GET /products`.
3. Add a `@ControllerAdvice` to map `BusinessRuleViolationException` to HTTP 422.
4. Delete `ProductRunner` (or keep it for a dual-mode app).

The same five unit tests continue to pass unchanged.

---

## Build Reference

| Goal | Command |
|------|---------|
| Compile | `mvn compile` |
| Test | `mvn test` |
| Package fat JAR | `mvn package -DskipTests` |
| Package + test | `mvn package` |
| Clean | `mvn clean` |
| Docker build | `docker build -t spring-product-registry .` |
| Docker run | `docker run -it spring-product-registry` |
