# Enterprise Product Registry

A multi-tier Java 21 console application demonstrating clean enterprise architecture with layered separation of concerns, SLF4J logging, JUnit 5 / Mockito testing, a Maven Shade fat JAR, and a hardened multi-stage Docker image.

---

## Architecture

```
com.bells
├── exception       BusinessRuleViolationException
├── repository      ProductRepository  (data layer)
├── service         ProductService     (business layer)
├── controller      ProductController  (presentation layer)
└── Main            Manual DI entry point
```

Each layer has a single responsibility and depends only on the layer directly below it. No framework is used — dependency injection is performed manually in `Main.java`.

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
enterprise one/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/bells/
    │   │   ├── Main.java
    │   │   ├── exception/
    │   │   │   └── BusinessRuleViolationException.java
    │   │   ├── repository/
    │   │   │   └── ProductRepository.java
    │   │   ├── service/
    │   │   │   └── ProductService.java
    │   │   └── controller/
    │   │       └── ProductController.java
    │   └── resources/
    │       └── logback.xml
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

### 2. Package a fat JAR

```bash
mvn package -DskipTests
```

The Shade plugin produces a single executable JAR at:

```
target/enterprise-product-registry-1.0.0.jar
```

### 3. Run the application

```bash
java -jar target/enterprise-product-registry-1.0.0.jar
```

---

## Console Commands

Once the application starts you will see the prompt `>`. The following commands are available:

| Command | Description |
|---------|-------------|
| `add <name>` | Register a new product by name |
| `list` | Display all registered products |
| `exit` | Shut down the application |

### Example session

```
=== Enterprise Product Registry ===
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
docker build -t product-registry .
```

The build uses two stages:

| Stage | Base image | Purpose |
|-------|-----------|---------|
| `builder` | `maven:3.9.8-eclipse-temurin-21-alpine` | Compiles source and packages the fat JAR |
| `runtime` | `eclipse-temurin:21-jre-alpine` | Runs the JAR as a non-root user |

The runtime stage creates a dedicated `appuser` in `appgroup` and runs under that identity — no root privileges in the final image.

### Run the container

The application reads from stdin, so the `-it` flags are required to attach an interactive terminal:

```bash
docker run -it product-registry
```

---

## Layer Design

### `exception` — `BusinessRuleViolationException`

Extends `RuntimeException`. Thrown by the service layer when a business rule is violated so the controller can catch it separately from unexpected system errors.

### `repository` — `ProductRepository`

Owns the in-memory `ArrayList<String>`. Exposes three methods:

- `save(String name)` — appends a product
- `exists(String name)` — duplicate check
- `findAll()` — returns an unmodifiable view

### `service` — `ProductService`

Accepts `ProductRepository` via constructor injection. Contains all business rules:

- Rejects null or blank names
- Rejects duplicates
- Trims whitespace before delegating to the repository
- Logs every operation via SLF4J at `INFO` / `WARN` level

### `controller` — `ProductController`

Handles the console loop. Calls the service inside a `try-catch (BusinessRuleViolationException)` block and prints a human-readable `[RULE VIOLATION]` prefix. Does not contain any business logic.

---

## Tests

`ProductServiceTest` uses JUnit 5 with the Mockito extension. The repository is mocked so tests are isolated to service behaviour only.

| Test | Verifies |
|------|---------|
| `registerProduct_successfulSave` | `save()` is called exactly once on the repository |
| `registerProduct_blankName_throws…` | Exception message is correct; repository never touched |
| `registerProduct_nullName_throws…` | Null is treated identically to blank |
| `registerProduct_duplicateName_throws…` | `save()` is never called when `exists()` returns `true` |
| `registerProduct_trimsWhitespaceBeforeSave` | `"  Widget  "` persists as `"Widget"` |

Run only the tests:

```bash
mvn test
```

---

## Logging

Logback is configured in `src/main/resources/logback.xml`. All output goes to stdout with the pattern:

```
HH:mm:ss.SSS [thread] LEVEL logger - message
```

To change the log level, edit the `<root level="INFO">` element in `logback.xml`.

---

## Build Reference

| Goal | Command |
|------|---------|
| Compile | `mvn compile` |
| Test | `mvn test` |
| Package fat JAR | `mvn package -DskipTests` |
| Package + test | `mvn package` |
| Clean | `mvn clean` |
| Docker build | `docker build -t product-registry .` |
| Docker run | `docker run -it product-registry` |
