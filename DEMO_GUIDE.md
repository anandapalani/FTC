# Enterprise Java Masterclass — Demo Execution Guide

> Trainer reference for live demonstrations. Each stage explains the concept, the advantage, and the hands-on steps. Follow stages in order — each one introduces the pain of the previous and resolves it with a pattern.

---

## Prerequisites

Verify these are installed before the session:

```bash
java -version        # must show Java 21+
mvn -version         # must show Maven 3.9+
docker --version     # must show Docker 24+
kubectl version      # must show kubectl 1.28+
```

> If `mvn` is not on PATH, use the full path: `/opt/homebrew/bin/mvn`

---

## Project Layout

```
Java Masterclass/
├── monolith/                          ← Stage 1
│   └── com/bells/monolith/
│       └── ProductApp.java
├── enterprise one/                    ← Stage 2
│   ├── pom.xml
│   └── src/
├── enterprise spring/                 ← Stage 3
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
├── cloud native/                      ← Stage 4 + Stage 5
│   ├── pom.xml
│   ├── Dockerfile
│   ├── k8s/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   └── src/
├── DEMO_GUIDE.md                      ← this file
├── SLIDES_CONTENT.md                  ← slide content reference
└── Enterprise_Java_Masterclass.pptx   ← presentation deck
```

---

## Stage 1 — The Legacy Monolith

### Concept

A monolith puts every concern — reading input, validating data, and storing results — inside a single method. There are no boundaries between layers. This is the starting point every developer recognises from their first assignment project.

### Why This Pattern Fails in Production

| Problem | Impact |
|---|---|
| `static List<String>` for storage | Global mutable state — any line can corrupt it |
| All logic in `main()` | No injection point — nothing can be replaced or mocked |
| `System.out.println` for errors | Cannot catch or handle specific failures programmatically |
| Zero seams | Cannot write a unit test without running the entire application |
| Everything coupled | Every change risks breaking unrelated behaviour |

### The Teaching Exercise

Before showing any solution, ask trainees to write a unit test for the duplicate-check logic. They will find there is no injection point — the `static List`, the Scanner, and the validation are all one inseparable block. **This frustration is the motivation for Stage 2.**

### Compile and Run

```bash
cd monolith
javac com/bells/monolith/ProductApp.java
java com.bells.monolith.ProductApp
```

### Expected Output

```
=== Product Registration System ===
Commands: add <name> | list | remove <name> | exit
>
```

### Demo Script

| You type | App responds | Teaching point |
|---|---|---|
| `add Widget` | `Added: Widget` | Works |
| `add Widget` | `Error: 'Widget' is already registered.` | Logic lives in `main()` — untestable |
| `add` | `Error: product name cannot be empty.` | Same `if` block, no separation |
| `list` | `1. Widget` | Direct ArrayList read — no layer |
| `remove Widget` | `Removed: Widget` | Direct mutation — no audit |
| `exit` | exits | |

### Key Question to Ask

> "Open `ProductApp.java`. Point at line 1. Where does the presentation layer end and the business logic begin? There is no answer — that is the problem."

---

## Stage 2 — Clean Layered Java (Manual DI)

### Concept

Separate the application into four classes, each with exactly one responsibility. Pass dependencies through constructors so every dependency is explicit, visible, and replaceable. This is the **Dependency Inversion Principle** (D in SOLID).

### Why Layering Matters — The Advantages

| Advantage | What It Buys You |
|---|---|
| **Single Responsibility** | Each class has one reason to change — a storage change never touches the controller |
| **Constructor Injection** | All dependencies are declared up front — no hidden coupling |
| **Interface Abstraction** | `ProductStore` hides *how* data is stored — swap implementations without touching the service |
| **Testable by design** | `@Mock ProductStore` replaces the real storage in milliseconds — no infrastructure needed |
| **Typed exceptions** | `BusinessRuleViolationException extends RuntimeException` — catch specific failures, not all failures |

### Architecture Walkthrough

Open each file and explain its role before running:

**1. `ProductStore.java` (interface)**
```
"This is the contract. The service knows what operations exist.
It has zero knowledge of whether data goes to a List, a file, or a database.
This is why we program to abstractions — the swap cost is one new class."
```

**2. `InMemoryProductStore.java`**
```
"This owns the ArrayList. It returns Collections.unmodifiableList()
so callers cannot mutate storage from the outside. Only this class can write to it."
```

**3. `ProductService.java`**
```
"This is the brain. It trims whitespace, validates null/blank before touching
the store, logs every decision via SLF4J, and throws BusinessRuleViolationException.
Notice the constructor — the store is injected, not created here."
```

**4. `ProductController.java`**
```
"This handles I/O only. It catches BusinessRuleViolationException and prints
a clean message. System errors propagate uncaught — intentionally."
```

**5. `Main.java`**
```java
ProductStore store = new InMemoryProductStore();
ProductService service = new ProductService(store);
ProductController controller = new ProductController(service);
controller.menu();
```
```
"Three lines wire the entire application. This is manual dependency injection.
Every dependency is explicit. Ask: what would you change to use a database? 
Answer: write a new class that implements ProductStore. Main.java changes one line.
ProductService does not change at all."
```

### Run Tests First — Show the Seam

```bash
cd "enterprise one"
mvn test
```

Expected:
```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Walk through each test:**

- `registerProduct_successfulSave` — "save() is called exactly once. We verify the call happened, not just that no exception was thrown."
- `registerProduct_blankName` — "`verifyNoInteractions(store)` — the database was never touched. The guard fired first."
- `registerProduct_nullName` — "Null and blank are treated identically. One guard covers both."
- `registerProduct_duplicateName` — "`verify(store, never()).save()` — when exists() returns true, save() must never be called."
- `registerProduct_trimsWhitespace` — "`'  Widget  '` is saved as `'Widget'`. Data integrity, not cosmetics."

### Package and Run

```bash
mvn package -DskipTests
java -jar target/enterprise-product-registry-1.0.0.jar
```

### Demo Script

| You type | App responds |
|---|---|
| `add Widget` | `[OK] Registered: Widget` |
| `add Widget` | `[RULE VIOLATION] Product 'Widget' is already registered.` |
| `add   ` | `[RULE VIOLATION] Product name must not be blank.` |
| `list` | `[INFO] Products (1): 1. Widget` |
| `exit` | `Shutting down. Goodbye.` |

### Key Question to Ask

> "How many lines of `ProductService.java` do we need to change to make this app save to a PostgreSQL database? Zero. We write a new `PostgresProductStore` that implements `ProductStore`. The service never changes. That is what separation of concerns bought us."

---

## Stage 3 — Enterprise Spring (Spring Boot)

### Concept

Spring Boot is not a new architecture — it is an automation of the architecture built in Stage 2. The IoC container reads annotations and wires the entire object graph automatically. The developer declares intent with `@Service`, `@Repository`, `@Component` — Spring does the rest.

### Why Spring Boot — The Advantages

| Advantage | Plain Java (Stage 2) | Spring Boot (Stage 3) |
|---|---|---|
| **Dependency wiring** | Manual `new` in `Main.java` | `@SpringBootApplication` — container scans and wires |
| **Scales with codebase** | 4 classes = 3 lines; 40 classes = unmanageable | Annotations scale linearly — 400 classes, zero extra wiring |
| **Configuration** | Hard-coded in source | `application.properties` — overridable by env var at runtime |
| **Logging** | `logback.xml` manually configured | Auto-configured — one property controls log level |
| **Packaging** | Maven Shade plugin (explicit manifest) | `spring-boot-maven-plugin` — no config needed |
| **Lifecycle management** | None | Startup order, graceful shutdown, bean scopes — handled |
| **Testing** | JUnit + Mockito wired manually | `spring-boot-starter-test` bundles everything |
| **Extensibility** | Adding REST requires full Servlet wiring | Add `spring-boot-starter-web` — HTTP layer ready instantly |

### Architecture Walkthrough

**1. Show `Main.java` from Stage 2 vs `ProductApplication.java` from Stage 3**

```
Stage 2: new ProductStore() → new ProductService(store) → new ProductController(service)
Stage 3: @SpringBootApplication — Spring does all three lines automatically
```

**2. Show `@Service` on `ProductService`**
```
"The annotation tells Spring: manage this class as a singleton bean.
When ProductController needs a ProductService, Spring injects it automatically.
The constructor injection code is identical — Spring just calls it for us."
```

**3. Show `application.properties`**
```
"Every value here is overridable at runtime. No recompilation needed.
This is externalised configuration — essential for cloud deployments
where hostnames, ports, and secrets differ per environment."
```

### Option A — Run via Maven (fastest for demos)

```bash
cd "enterprise spring"
mvn spring-boot:run
```

### Option B — Package then Run

```bash
mvn package -DskipTests
java -jar target/enterprise-spring-product-registry-1.0.0.jar
```

### Override Config at Runtime (live demo)

```bash
# Change log level without recompiling
java -jar target/enterprise-spring-product-registry-1.0.0.jar \
     --logging.level.com.bells=DEBUG

# Change port without touching application.properties
java -jar target/enterprise-spring-product-registry-1.0.0.jar \
     --server.port=9090
```

### Run Tests

```bash
mvn test
```

Expected:
```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Key Question to Ask

> "The same five unit tests pass on both Stage 2 and Stage 3 without modification. Spring did not change the architecture — it automated it. The design principle (constructor injection, interface abstraction) is the same. The labour is eliminated."

---

## Stage 4 — Cloud-Native Web App (Spring Boot + Thymeleaf + JPA + Liquibase)

### Concept

The cloud-native project is the full-stack extension of Stage 3. It adds:
- A **browser-based UI** via Thymeleaf templates
- **Spring Data JPA** for database persistence (replacing the in-memory List)
- **Liquibase** for versioned, auditable schema migrations
- **H2 database** in two modes: in-memory (teaching) and file-based (persistent)

### Stage 4A — Understanding the H2 Database Modes

This is a critical teaching moment. The app ships with **two database modes**, both documented in `application.yaml`. Switching between them teaches two distinct concepts.

#### Mode 1 — In-Memory (default, active on first run)

**What it does:** Data lives only while the JVM is running. Every restart wipes the database clean.

**Run the app (Mode 1 active):**

```bash
cd "cloud native"
java -jar target/cloud-native-product-registry-1.0.0.jar
```

Open `http://localhost:8081/web/products` — three seed products appear (Widget Pro, Gadget Lite, Doohickey Max).

**Demo sequence:**

1. Add a new product via the form — it appears in the table
2. Stop the app (`Ctrl+C`)
3. Restart — the product you added is gone, only the 3 seed rows remain
4. Ask: *"Why did our data disappear?"*
5. Point at the startup log:
   ```
   Running Changeset 001 — products table created
   Running Changeset 002 — unique constraint added
   Running Changeset 003 — 3 seed products inserted
   ```
   Liquibase re-ran all three changesets because the database was wiped on shutdown.

**Teaching point:**
> "In-memory databases are stateless by design. The app cannot tell the difference between a first boot and a hundredth boot — the database is always empty on startup. This is ideal for testing and for demonstrating the Liquibase migration lifecycle. It is not suitable for production."

**H2 Console (Mode 1):**
- URL: `http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:mem:productdb`
- Username: `sa` | Password: *(leave blank)*

Open the console and run:
```sql
SELECT * FROM PRODUCTS;
SELECT * FROM DATABASECHANGELOG;
```
Show trainees the `DATABASECHANGELOG` table — this is how Liquibase tracks what has run.

---

#### Mode 2 — File-Based (persistent across restarts)

**What to change in `application.yaml`:**

```yaml
# Comment out Mode 1 block:
# datasource:
#   url: jdbc:h2:mem:productdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE

# Uncomment Mode 2 block:
datasource:
  url: jdbc:h2:file:./data/productdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE
  driver-class-name: org.h2.Driver
  username: sa
  password: ""
```

**Rebuild and run:**

```bash
mvn package -DskipTests
java -jar target/cloud-native-product-registry-1.0.0.jar
```

A `./data/productdb.mv.db` file is created on disk.

**Demo sequence:**

1. Add a new product via the form
2. Stop the app (`Ctrl+C`)
3. Restart — the product you added is still there
4. Point at the startup log:
   ```
   Previously run: 3
   Filtered out:   0
   Total change sets: 3
   ```
   Liquibase found all three changesets already recorded in `DATABASECHANGELOG` and **skipped them all**.
5. Ask: *"Why didn't Liquibase re-run the seed data changeset?"*

**Teaching point:**
> "Liquibase stores a checksum of every changeset in the `DATABASECHANGELOG` table. On each startup it compares the changelog file against that table. If a changeset has already run, it is skipped. This is what makes Liquibase migrations idempotent — safe to run repeatedly without creating duplicates or errors."

**H2 Console (Mode 2):**
- JDBC URL: `jdbc:h2:file:./data/productdb`
- Username: `sa` | Password: *(leave blank)*

| | Mode 1 — In-Memory | Mode 2 — File-Based |
|---|---|---|
| Data on restart | Wiped | Persists |
| Liquibase on restart | Runs all 3 changesets | Skips all (already applied) |
| `./data/` folder | Not created | `productdb.mv.db` written to disk |
| H2 Console JDBC URL | `jdbc:h2:mem:productdb` | `jdbc:h2:file:./data/productdb` |
| Best for teaching | Liquibase lifecycle, stateless design | Persistent workflow, idempotent migrations |

---

### Stage 4B — Walking Through the Architecture

Open each file and explain before running:

**1. `Product.java` (Entity)**
```
"@Entity maps this class to the products table in the database.
@Id @GeneratedValue means the database assigns the primary key.
@Column constraints match the Liquibase schema exactly — NOT NULL, UNIQUE, precision.
@PrePersist stamps created_at in Java so it works on any JDBC driver."
```

**2. `ProductRepository.java`**
```
"This interface extends JpaRepository<Product, Long>.
Spring Data JPA generates the entire implementation at startup —
findAll(), save(), deleteById(), and our two custom methods.
We wrote zero SQL. Zero DAO boilerplate."
```

**3. `db.changelog-master.yaml`**
```
"Three changesets, each independently revertible:
001 — creates the table with all columns
002 — adds the unique constraint on name
003 — inserts 3 seed rows for local dev

Each changeset has an id, author, and comment.
Liquibase tracks these in DATABASECHANGELOG.
This file IS the source of truth for your schema."
```

**4. `ProductService.java`**
```
"@Transactional on register() means the entire method runs in one DB transaction.
If the save() fails halfway, the transaction rolls back.
@Transactional(readOnly=true) on findAll() tells the DB connection pool
this is a read-only query — it can optimise accordingly."
```

**5. `ProductWebController.java`**
```
"@Controller (not @RestController) returns view names, not JSON.
@ModelAttribute binds the form fields to the Product object automatically.
BindingResult captures validation errors from @Valid.
RedirectAttributes.addFlashAttribute() stores the success/error message
across the POST → redirect → GET cycle."
```

**6. `products.html`**
```
"th:object="${newProduct}" binds the form to the Product bean.
th:field="*{name}" wires each input to a field on that object.
th:if="${#fields.hasErrors('name')}" shows validation errors inline.
th:each="product : ${products}" loops over the list from the controller."
```

---

## Stage 5 — Docker: Containerised Deployment

### Concept

Docker solves the "works on my machine" problem by packaging the application and its runtime environment together into a single, portable image. The **multi-stage Dockerfile** separates build-time tools (JDK, Maven) from the runtime image — the JDK is never present in what gets deployed.

### Why Docker — The Advantages

| Advantage | Without Docker | With Docker |
|---|---|---|
| **Portability** | Requires Java 21 on each host machine | Runs on any machine with Docker installed |
| **Reproducibility** | Host JRE version drift causes silent bugs | Exact JRE version pinned in the image |
| **Security** | App runs as root by default | Non-root `appuser` created in the build |
| **Image size** | Full JDK ~600 MB | JRE-only Alpine ~80–220 MB |
| **CI/CD** | Manual deployment steps | `docker build` + `docker push` + `docker run` |
| **Config** | Hard-coded or file-based | Environment variables override any config |

### Step-by-Step: Understanding the Dockerfile

Open `cloud native/Dockerfile` and walk through it line by line:

```dockerfile
# ── STAGE 1: BUILDER ──────────────────────────────────────────────────────────
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
```
> "This stage has a full JDK and Maven. It exists only to compile. It will never be deployed."

```dockerfile
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
```
> "We copy pom.xml first and download dependencies before copying source. Docker caches each layer. If only source files change, this layer is reused — the download step is skipped on rebuilds."

```dockerfile
COPY src ./src
RUN mvn package -DskipTests -q
```
> "Now we copy source and compile. The fat JAR lands in /build/target/."

```dockerfile
# ── STAGE 2: RUNTIME ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
```
> "A completely fresh Alpine Linux with only the JRE. Ask: does this image know anything about Stage 1? No. It has never seen Maven, pom.xml, or .java files."

```dockerfile
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
```
> "We create a non-root user. Without this, the process runs as root inside the container — a security risk in production cloud environments."

```dockerfile
COPY --from=builder /build/target/cloud-native-product-registry-1.0.0.jar app.jar
```
> "This is the stage boundary. Only the compiled JAR crosses from Stage 1 into Stage 2. Everything else — Maven, the JDK, source code, intermediate class files — is discarded."

```dockerfile
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```
> "Run as non-root. Declare the port. Start the app. This is the entire runtime image."

### Hands-On: Build and Run

**Step 1 — Build the image**

```bash
cd "cloud native"
docker build -t cloud-native-registry:1.0.0 .
```

Watch the terminal — you will see both stages execute sequentially. The builder stage downloads dependencies and compiles; the runtime stage copies only the JAR.

**Step 2 — Inspect the image**

```bash
# See the final image size
docker images cloud-native-registry

# Prove the JDK was stripped
docker run --rm --entrypoint sh cloud-native-registry:1.0.0 \
  -c "which javac && echo 'JDK present' || echo 'JDK stripped — JRE only'"
```

Expected: `JDK stripped — JRE only`

**Step 3 — Run the container**

```bash
docker run -p 8081:8081 cloud-native-registry:1.0.0
```

Flag explanation:
- `-p 8081:8081` — maps host port 8081 to container port 8081
- Without `-p`, the container runs in isolation and nothing can reach it

Open `http://localhost:8081/web/products`

**Step 4 — Override configuration via environment variable**

```bash
docker run -p 8081:8081 \
  -e LOGGING_LEVEL_COM_BELLS=DEBUG \
  cloud-native-registry:1.0.0
```

Spring Boot maps environment variables automatically:
`LOGGING_LEVEL_COM_BELLS=DEBUG` → `logging.level.com.bells=DEBUG`

> "No file was changed. No rebuild happened. The same image runs with a different configuration. This is the power of externalised configuration."

**Step 5 — Run in the background (detached mode)**

```bash
# Start in background
docker run -d -p 8081:8081 --name product-registry cloud-native-registry:1.0.0

# Follow the logs
docker logs -f product-registry

# Stop the container
docker stop product-registry

# Remove the container
docker rm product-registry
```

### Layer Caching Demo (teaches CI efficiency)

```bash
# First build — downloads everything
docker build -t cloud-native-registry:1.0.0 .

# Change one Java file, then rebuild
# Watch: dependency layer is CACHED, only compile step reruns
docker build -t cloud-native-registry:1.0.0 .
```

> "The `COPY pom.xml` + `mvn dependency:go-offline` layer is cached because pom.xml did not change. Only the `COPY src` + `mvn package` layer reruns. In CI, this saves minutes on every build."

### Key Questions to Ask

> - "Where is the JDK in the final image?" — It isn't there.
> - "What happens if a security scanner finds a vulnerability in Maven?" — It is irrelevant — Maven is not in the runtime image.
> - "How do you pass a database password to a containerised app?" — `-e DB_PASSWORD=secret`. Never bake secrets into the image.

---

## Stage 6 — Kubernetes: Production Orchestration

### Concept

Kubernetes takes a Docker image and runs it at scale — multiple copies, automatic restarts on failure, traffic routing, health monitoring. Where Docker answers "how do I ship the app?", Kubernetes answers "how do I keep the app running in production?"

### Why Kubernetes — The Advantages

| Advantage | Docker alone | Kubernetes |
|---|---|---|
| **High availability** | Single container — one crash = downtime | `replicas: 2+` — pods restart automatically |
| **Health monitoring** | None | Liveness + readiness probes restart unhealthy pods |
| **Traffic routing** | Manual port mapping | Service object load-balances across all healthy pods |
| **Scaling** | Manual `docker run` again | `kubectl scale --replicas=N` — instant |
| **Rolling updates** | Stop + restart = downtime | Rolling deployment — zero downtime |
| **Resource limits** | Unlimited — can starve other apps | CPU and memory requests + limits per pod |
| **Config management** | `-e` env flags on each `docker run` | `env:` block in deployment.yaml — version-controlled |

### Step-by-Step: Understanding the Kubernetes Files

#### `k8s/deployment.yaml`

Open the file and explain each section:

```yaml
spec:
  replicas: 2
```
> "Two copies of the container run simultaneously. If one crashes, the other keeps serving traffic while Kubernetes restarts the failed pod. No human intervention needed."

```yaml
  selector:
    matchLabels:
      app: product-registry
```
> "The selector tells Kubernetes which pods belong to this deployment. Labels are how K8s connects Deployments, Services, and pods."

```yaml
      containers:
        - name: product-registry
          image: cloud-native-registry:1.0.0
          imagePullPolicy: IfNotPresent
```
> "`IfNotPresent` means Kubernetes uses the locally loaded image. In production this would be `Always` with a remote registry like Docker Hub or AWS ECR."

```yaml
          resources:
            requests:
              cpu: "250m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
```
> "Requests are what the pod is guaranteed. Limits are the ceiling. `250m` = 0.25 CPU cores. Without limits, one misbehaving pod can starve every other app on the node."

```yaml
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 15
```
> "Every 15 seconds Kubernetes calls `/actuator/health/liveness`. If it fails 3 times in a row, the pod is killed and a new one starts. This is automatic self-healing."

```yaml
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            initialDelaySeconds: 20
            periodSeconds: 10
```
> "Readiness controls traffic routing. Until `/actuator/health/readiness` returns 200, the Service will not send any requests to this pod. During startup, the pod accepts no traffic — it only starts receiving it when it is truly ready."

```yaml
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
```
> "The cluster enforces non-root execution at the infrastructure level. Even if the Dockerfile forgets `USER appuser`, the cluster policy blocks it."

#### `k8s/service.yaml`

```yaml
spec:
  type: NodePort
  ports:
    - port: 80
      targetPort: 8081
      nodePort: 30080
```
> "The Service acts as a stable entry point. It load-balances across all healthy pods matching the selector. `NodePort` exposes it on port 30080 of your local machine. In a real cloud cluster this would be `LoadBalancer` to get an external IP."

### Hands-On: Deploy to Kubernetes

**Prerequisites — verify the cluster is ready:**

```bash
kubectl cluster-info
kubectl get nodes
```

Expected:
```
NAME             STATUS   ROLES           AGE
docker-desktop   Ready    control-plane   ...
```

**Step 1 — Build and load the image**

```bash
cd "cloud native"
docker build -t cloud-native-registry:1.0.0 .
```

If using **minikube**:
```bash
minikube image load cloud-native-registry:1.0.0
```

If using **Docker Desktop Kubernetes**: the image is already available — no extra step.

**Step 2 — Deploy the application**

```bash
kubectl apply -f k8s/deployment.yaml
```

Watch Kubernetes create the pods:

```bash
kubectl get pods -w
```

Expected progression:
```
NAME                               READY   STATUS              RESTARTS
product-registry-xxxxxxx-xxxxx    0/1     ContainerCreating   0
product-registry-xxxxxxx-xxxxx    0/1     ContainerCreating   0
product-registry-xxxxxxx-xxxxx    1/1     Running             0
product-registry-xxxxxxx-xxxxx    1/1     Running             0
```

> "Notice `0/1` during startup — the readiness probe has not returned healthy yet. Kubernetes is not sending traffic to these pods. Once it shows `1/1 Running`, the pod is both alive and ready."

**Step 3 — Expose the application**

```bash
kubectl apply -f k8s/service.yaml
```

```bash
kubectl get svc product-registry-svc
```

Expected:
```
NAME                   TYPE       CLUSTER-IP    PORT(S)        AGE
product-registry-svc   NodePort   10.x.x.x      80:30080/TCP   5s
```

Open `http://localhost:30080/web/products`

**Step 4 — Inspect the running pods**

```bash
# Summary of all pods
kubectl get pods

# Detailed view of one pod (shows probe status, events, restarts)
kubectl describe pod <pod-name>

# Live logs from all pods
kubectl logs -l app=product-registry --follow

# Logs from one specific pod
kubectl logs <pod-name> --follow
```

**Step 5 — Demonstrate self-healing (live demo)**

```bash
# Get pod names
kubectl get pods

# Delete one pod manually — simulates a crash
kubectl delete pod <pod-name>

# Watch Kubernetes immediately start a replacement
kubectl get pods -w
```

> "We deleted a pod. Within seconds Kubernetes detected the desired replica count was 2 but only 1 was running, and it started a replacement automatically. No alert. No human intervention. This is self-healing."

**Step 6 — Scale the deployment**

```bash
# Scale up to 4 replicas
kubectl scale deployment product-registry --replicas=4

# Watch pods come up
kubectl get pods -w

# Scale back to 2
kubectl scale deployment product-registry --replicas=2

# Watch pods terminate gracefully
kubectl get pods -w
```

> "Enterprise traffic spikes don't wait for a human to manually start more containers. Kubernetes can scale horizontally in seconds."

**Step 7 — Override configuration via environment variables**

```bash
# Edit the deployment to add an env var (opens in $EDITOR)
kubectl set env deployment/product-registry LOGGING_LEVEL_COM_BELLS=DEBUG

# Kubernetes performs a rolling restart — zero downtime
kubectl rollout status deployment/product-registry
```

> "The `set env` command triggers a rolling deployment — pods are replaced one at a time, so traffic never drops to zero. The same externalised configuration principle from Spring Boot works seamlessly in Kubernetes."

**Step 8 — Roll back a deployment**

```bash
# View deployment history
kubectl rollout history deployment/product-registry

# Roll back to the previous version
kubectl rollout undo deployment/product-registry
```

**Step 9 — Tear down when done**

```bash
kubectl delete -f k8s/deployment.yaml
kubectl delete -f k8s/service.yaml
```

Or delete everything at once:

```bash
kubectl delete -f k8s/
```

### Key Questions to Ask

> - "What happens when a pod crashes at 3am?" — Kubernetes detects it via the liveness probe and starts a replacement. No pager. No human.
> - "How do you deploy a new version with zero downtime?" — `kubectl set image deployment/product-registry ... && kubectl rollout status`. Kubernetes replaces pods one at a time.
> - "Why do we need both liveness AND readiness probes?" — Liveness restarts broken pods. Readiness stops traffic to pods that are starting up or temporarily overloaded. They serve different purposes.

---

## Deployment Method Comparison

| | Java JAR | Docker | Kubernetes |
|---|---|---|---|
| **Requires** | Java 21 on host | Docker Desktop | Docker + kubectl + cluster |
| **Port** | `8081` | `8081` (mapped via -p) | `30080` (NodePort) |
| **Config override** | `--key=value` flags | `-e ENV_VAR=value` | `env:` in deployment.yaml |
| **Scaling** | Manual (run another process) | Manual (`docker run` again) | `kubectl scale --replicas=N` |
| **Health monitoring** | None | None | Liveness + readiness probes |
| **Self-healing** | None | None | Automatic pod restart |
| **Rolling updates** | Restart = downtime | Stop + restart = downtime | Zero-downtime rolling deployment |
| **Best for** | Local development | CI/CD, staging | Production cloud workloads |

---

## Spring Boot Advantages — Complete Reference

### 1. Auto-Configuration
Spring Boot inspects the classpath and wires sensible defaults automatically. Add `spring-boot-starter-web` and you get an embedded Tomcat, Jackson JSON serialisation, and MVC dispatcher — no XML, no `web.xml`.

### 2. Externalised Configuration
Every `application.properties` / `application.yaml` value is overridable by:
- Environment variable: `SERVER_PORT=9090`
- Command-line arg: `--server.port=9090`
- Profile-specific file: `application-prod.yaml`

This is essential for containers and cloud — the same image runs in dev, staging, and production with different config.

### 3. Spring Boot BOM (Bill of Materials)
Declaring `spring-boot-starter-parent` as parent POM pins all compatible dependency versions. You declare intent (`spring-boot-starter-web`), not versions. No more "version X of library A is incompatible with version Y of library B".

### 4. Embedded Server
Tomcat is embedded in the fat JAR. No separate server installation. No WAR deployment. `java -jar app.jar` is the entire deployment process. The server version is pinned in the BOM — no server drift between environments.

### 5. Spring Data JPA
`JpaRepository<Product, Long>` generates `findAll()`, `save()`, `deleteById()`, and pagination — no SQL, no DAO boilerplate. Custom queries are derived from method names: `findByNameIgnoreCase` generates the correct SQL automatically.

### 6. Production-Ready via Actuator
Add `spring-boot-starter-actuator`:
- `/actuator/health` — liveness and readiness endpoints for Kubernetes probes
- `/actuator/metrics` — JVM, HTTP, and custom metrics for monitoring dashboards
- `/actuator/info` — build version, git commit — useful for deployment audits

### 7. Testing Support
`spring-boot-starter-test` bundles JUnit 5, Mockito, AssertJ, Hamcrest, MockMvc, and Spring Test. Two test patterns coexist:
- `@ExtendWith(MockitoExtension.class)` — fast unit tests, no Spring context
- `@SpringBootTest` — full context integration tests for HTTP contract verification

### 8. Profile Support
`@Profile("prod")` beans activate only when `--spring.profiles.active=prod` is set. Separate `application-dev.yaml` and `application-prod.yaml` files hold environment-specific config. Switch environments by changing one flag.

---

## Side-by-Side Comparison Demo

Open three terminal tabs and run all three simultaneously:

**Tab 1 — Monolith**
```bash
cd monolith
java com.bells.monolith.ProductApp
```

**Tab 2 — Enterprise One**
```bash
cd "enterprise one"
java -jar target/enterprise-product-registry-1.0.0.jar
```

**Tab 3 — Enterprise Spring**
```bash
cd "enterprise spring"
mvn spring-boot:run
```

> "All three apps do the same thing. The domain never changed. What changed is how the code is structured, tested, configured, and deployed. Every difference is explainable and defensible in an interview."

---

## Quick Reference Card

| Project | Method | Command | URL / Notes |
|---|---|---|---|
| **Monolith** | Compile | `javac com/bells/monolith/ProductApp.java` | From `monolith/` |
| **Monolith** | Run | `java com.bells.monolith.ProductApp` | Console app |
| **Enterprise One** | Test | `mvn test` | From `enterprise one/` |
| **Enterprise One** | Run JAR | `java -jar target/enterprise-product-registry-1.0.0.jar` | Console app |
| **Enterprise Spring** | Run (quick) | `mvn spring-boot:run` | From `enterprise spring/` |
| **Enterprise Spring** | Run JAR | `java -jar target/enterprise-spring-product-registry-1.0.0.jar` | Console app |
| **Cloud Native** | Package | `mvn package -DskipTests` | From `cloud native/` |
| **Cloud Native** | Run JAR | `java -jar target/cloud-native-product-registry-1.0.0.jar` | `localhost:8081/web/products` |
| **Cloud Native** | H2 Console | *(app must be running)* | `localhost:8081/h2-console` |
| **Cloud Native** | Docker build | `docker build -t cloud-native-registry:1.0.0 .` | From `cloud native/` |
| **Cloud Native** | Docker run | `docker run -p 8081:8081 cloud-native-registry:1.0.0` | `localhost:8081/web/products` |
| **Cloud Native** | Docker bg | `docker run -d -p 8081:8081 --name product-registry cloud-native-registry:1.0.0` | Detached |
| **Cloud Native** | Docker logs | `docker logs -f product-registry` | Follow logs |
| **Cloud Native** | Docker stop | `docker stop product-registry && docker rm product-registry` | Clean up |
| **Cloud Native** | K8s deploy | `kubectl apply -f k8s/deployment.yaml` | From `cloud native/` |
| **Cloud Native** | K8s service | `kubectl apply -f k8s/service.yaml` | `localhost:30080/web/products` |
| **Cloud Native** | K8s scale | `kubectl scale deployment product-registry --replicas=4` | |
| **Cloud Native** | K8s self-heal | `kubectl delete pod <pod-name>` | Watch replacement start |
| **Cloud Native** | K8s logs | `kubectl logs -l app=product-registry --follow` | All pods |
| **Cloud Native** | K8s teardown | `kubectl delete -f k8s/` | Removes all resources |

---

## Troubleshooting

| Issue | Fix |
|---|---|
| `mvn: command not found` | Use `/opt/homebrew/bin/mvn` instead of `mvn` |
| `Port already in use` | `lsof -ti :<port>` to find the PID, then `kill <PID>` |
| `Error: Could not find or load main class` | Ensure you are in the correct project directory |
| `docker: command not found` | Docker Desktop must be running before any Docker commands |
| `Cannot GET /web/products` | Check the port — JAR uses `8081`, Docker uses `8081`, K8s uses `30080` |
| `ImagePullBackOff` in kubectl | Run `minikube image load cloud-native-registry:1.0.0` or use Docker Desktop K8s |
| `CrashLoopBackOff` in kubectl | Run `kubectl logs <pod-name>` to see the startup error |
| `0/1 pods Ready` in kubectl | Readiness probe still warming up — wait 30 seconds and check again |
| Fat JAR not found | Run `mvn package -DskipTests` first — `target/` is not in source control |
| H2 console login fails (Mode 1) | JDBC URL must be exactly `jdbc:h2:mem:productdb`, username `sa`, password blank |
| H2 console login fails (Mode 2) | JDBC URL must be `jdbc:h2:file:./data/productdb`, run app from project root |
| Data lost after restart | App is running in Mode 1 (in-memory) — switch to Mode 2 in `application.yaml` |
| Liquibase re-seeds on restart | Expected in Mode 1 — in-memory DB is wiped, Liquibase re-runs all changesets |
| Liquibase error: checksum mismatch | A changeset was modified after it ran — never edit a changeset, always add a new one |
