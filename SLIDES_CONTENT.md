# Enterprise Java Masterclass — The 4-Stage Evolution Journey

> Trainer reference document. Each slide includes title, bullets, and speaker notes.

---

## Slide 1 — Title Slide

**Title:** "Enterprise-ifying" a Java Core Application
**Subtitle:** The 4-Stage Evolution Journey
**Tag line:** Legacy Monolith → Clean Layers → Spring Boot → Production DevOps
**Footer:** SCTP Software Engineering Masterclass | BELLS Team | Java 21 · Spring Boot 3 · Docker · GitHub Actions

### Speaker Notes
This masterclass follows one domain — a product registry — through four deliberate transformation stages. Each stage introduces a specific pain point from the previous one and resolves it with an enterprise pattern. By the end, trainees can articulate not just what they built, but why every architectural decision was made.

---

## Slide 2 — The Evolution Roadmap (Journey Overview)

**Title:** The 4-Stage Evolution Journey

```
[ Stage 1: The Legacy Monolith    ] ──(Pain: Tightly Coupled & Untestable)──▶
[ Stage 2: Clean Layered Java     ] ──(Pain: Manual Wiring Doesn't Scale)───▶
[ Stage 3: The Spring Boot Edge   ] ──(Pain: Works on My Machine & Manual QA)▶
[ Stage 4: Production DevOps      ] ──(Result: Continuous Delivery Ready)────▶
```

| Stage | Label | Core Problem Solved |
|---|---|---|
| 🔴 1 | The Legacy Monolith | Tight coupling — nothing is testable |
| 🟡 2 | Clean Layered Java | Separation of concerns + automated testing |
| 🟢 3 | The Spring Boot Edge | IoC container + multi-delivery architecture |
| 🔵 4 | Production DevOps | Git workflow + CI + reproducible containers |

### Speaker Notes
Every row is a job interview question waiting to happen. "Tell me about your architecture" maps directly to this table. The goal is not to memorise the stages — it is to understand the pain that drives each transition and the pattern that resolves it.

---

## Slide 3 — Stage 1: The Legacy Monolith

**Title:** 🔴 Stage 1 — The Legacy Monolith
**Sub-title:** The "Student Script" — everything smashed into a single file

### The Monolith Architecture Diagram
```
+------------------------------------------------------------+
|                    ProductApp (Monolith)                   |
|  - main(String[] args)                                     |
|                                                            |
|  [ Presentation ] ◀──▶  Reads Scanner Input & Prints UI   |
|  [ Core Logic   ] ◀──▶  Validates strings, checks dupes   |
|  [ Data Storage ] ◀──▶  Direct mutations on static List   |
+------------------------------------------------------------+
```

### Pain Points — Why It Breaks
- **Shared Mutable State:** Data lives in `static List<String>` owned by no-one, open to corruption by anyone
- **Tangled Responsibilities:** `main()` handles presentation, business logic, and storage simultaneously — three jobs, one method
- **Zero Seams:** No boundaries means no test injection points — you cannot test business rules without running the full application and typing inputs manually
- **No Exception Hierarchy:** Errors reported as bare `System.out.println` strings — no way to catch specific failures programmatically
- **The Definition of Tight Coupling:** Every change risks breaking everything else because every concern shares the same scope

### Speaker Notes
The exercise: ask trainees to write a unit test for the duplicate-check logic in `ProductApp.java`. They will immediately hit the wall — there is no injection point. The static method, the static list, and the Scanner are all one inseparable block. This frustration is the motivation for Stage 2. Do not skip it.

---

## Slide 4 — Stage 2: Clean Layered Java (Architecture)

**Title:** 🟡 Stage 2 — Clean Layered Java
**Sub-title:** Separation of Concerns — 4 classes, one clear job each

### The Layered Architecture Diagram
```
+-------------------------------------------------------------------------+
|                         Presentation Tier                               |
|                   com.bells.controller.ProductController                |
+-------------------------------------------------------------------------+
                                    |
                          (Dependency Injection)
                                    ▼
+-------------------------------------------------------------------------+
|                          Business Logic Tier                            |
|                      com.bells.service.ProductService                   |
+-------------------------------------------------------------------------+
                                    |
                         (Depends on Abstraction)
                                    ▼
+-------------------------------------------------------------------------+
|                        «interface» ProductStore                         |
+-------------------------------------------------------------------------+
                                    ▲
                         (Implements Contract)
+-------------------------------------------------------------------------+
|                         InMemoryProductStore                            |
+-------------------------------------------------------------------------+
```

### Architectural Breakdown
- **The Contract (`ProductStore` interface):** Program to an abstraction — the service knows *what* operations exist, but has zero knowledge of *how* or *where* data is stored
- **The Data Tier (`InMemoryProductStore`):** Encapsulates the `ArrayList`; returns `Collections.unmodifiableList()` so callers cannot mutate storage from outside
- **The Core Logic Tier (`ProductService`):** Trims whitespace, validates nulls/blanks, logs every decision via SLF4J, throws `BusinessRuleViolationException` for rule violations
- **The Presentation Tier (`ProductController`):** Handles exclusively I/O logic; catches domain exceptions gracefully without crashing the thread

### Speaker Notes
Point at the interface in the diagram. Ask: "What would happen if we wanted to swap from an in-memory list to a database?" Answer: write a new class that implements `ProductStore`. `ProductService` never changes. This is why we program to the interface — the swap cost is one new file, not a rewrite of the entire application.

---

## Slide 5 — Stage 2: The Testing Seam

**Title:** 🟡 Stage 2 — The Testing Seam
**Sub-title:** Constructor Injection creates the gap that lets mocks in

### Left Column — "Why Tests Are Now Possible"
- **Constructor Injection:** Dependencies passed explicitly via constructor — the test controls what gets injected
- `@Mock ProductStore` replaces the real implementation with a fast placeholder
- **Fast Feedback Loop:** Tests execute in milliseconds without any infrastructure running
- **Deterministic Assertions:** We can verify that a blank name triggers the validation guard before the repository is *ever* touched

### Right Column — "The 5-Test Suite"
- `successfulSave` — `save()` called exactly once after clean input
- `blankName` / `nullName` — `verifyNoInteractions(store)` proves the guard fires first
- `duplicateName` — `verify(store, never()).save()` when `exists()` returns `true`
- `trimsWhitespace` — `"  Widget  "` persists as `"Widget"` (data integrity)
- **Zero false positives** — mocking the interface means business logic is tested in complete isolation from storage

### Speaker Notes
`verifyNoInteractions(store)` is the most powerful line in the test suite. It does not just assert an exception was thrown — it proves the database was never touched. This is the kind of precision that makes a senior engineer trust a test suite. Without the interface and constructor injection, this line is impossible to write.

---

## Slide 6 — Stage 3: The Spring Boot Edge (IoC Container)

**Title:** 🟢 Stage 3 — The Spring Boot Edge
**Sub-title:** From manual object graphs to Inversion of Control

### Left Column — "The Pain of Manual Wiring"
Stage 2's `Main.java` required explicit instantiation:
```java
ProductStore store = new InMemoryProductStore();
ProductService service = new ProductService(store);
ProductController controller = new ProductController(service);
```
- Manageable at 4 classes — impossible at 40 or 400
- Adding a new dependency means editing `Main.java` every time
- No lifecycle management — no shutdown hooks, no scope control
- Swapping an implementation requires finding every `new` call manually

### Right Column — "The Spring Solution"
- **Inversion of Control (IoC):** `@Service` / `@Repository` / `@Component` instructs the Spring container to instantiate and wire the entire graph automatically
- **`@SpringBootApplication`:** Component scan discovers every annotated class — zero manual wiring
- **Constructor injection still applies:** Spring auto-wires via the constructor — the code looks identical, the container does the work
- **Lifecycle management:** Spring handles startup order, graceful shutdown, and scope — the application grows without the wiring growing

### Speaker Notes
The key distinction to make: Spring did not change the architecture — it automated it. `ProductService` still receives `ProductStore` via its constructor. The difference is that in Stage 2, the developer wrote `new ProductService(store)`. In Stage 3, Spring writes it. The design principle (constructor injection, interface abstraction) is the same; the labour is eliminated.

---

## Slide 7 — Stage 3: Zero Changes for a New Delivery Path

**Title:** 🟢 Stage 3 — Architecture Proof: Two Delivery Paths, One Service
**Sub-title:** Adding REST costs one pom.xml dependency and one new class — nothing else

### The Dual-Path Architecture Diagram
```
                 +----------------------------+
                 |    ProductController       | ──▶ (Console UI Path)
                 +----------------------------+
                               |
+--------------------+         ▼
| Web Client / HTTP  | ──▶ +----------------------------+
| Delivery Path      |     |  ProductApiController      | ──▶ (@RestController)
+--------------------+     +----------------------------+
                                       |
                                       ▼
                         +----------------------------+
                         |      ProductService        | ──▶ Untouched Core Logic
                         +----------------------------+
```

### What Changes / What Does Not
| | Console Path | REST Path |
|---|---|---|
| **Entry class** | `ProductController` + `CommandLineRunner` | `ProductApiController` with `@RestController` |
| **Exception handling** | `catch (BusinessRuleViolationException e)` | `@ControllerAdvice` → HTTP 422 |
| **ProductService** | Untouched | Untouched |
| **All 5 unit tests** | Pass | Still pass |

### Speaker Notes
This is the payoff slide. Run the live demo: add `spring-boot-starter-web`, add `ProductApiController`, start the app, hit `POST /products` with curl. Ask: "How many lines of `ProductService` did we change?" Zero. The architecture built in Stage 2 absorbs a completely new delivery mechanism without modification.

---

## Slide 8 — Stage 4: Professional Git Workflow

**Title:** 🔵 Stage 4 — Stop Committing to Main
**Sub-title:** Direct commits bypass every quality gate — enterprise teams use a 3-step enforcement model

### Step 1 — Feature Branches: Isolation of Intent
- `feature/add-product-validation` · `fix/duplicate-check-edge-case`
- Each branch is reviewable as a single structural diff
- `main` is production — direct commits skip code review and CI entirely

### Step 2 — Semantic Commits: Machine-Readable History
- `feat: add BusinessRuleViolationException hierarchy`
- `test: verify null name triggers guard before repository contact`
- `refactor: extract duplicate-check logic from controller to service`
- `chore: configure Maven Shade manifest for fat JAR`
- Format: `type(scope): imperative present tense` — grep-able, generates changelogs automatically

### Step 3 — Pull Requests: Collaborative Quality Gate
- Opens a peer code review conversation before code reaches `main`
- Merge **only** when CI pipeline passes **and** reviewer approves
- PR history is the team's permanent, searchable audit trail

### Speaker Notes
Ask: "What does a recruiter see when they open your GitHub profile?" They see commit messages. A history of "fix stuff", "wip", "update" signals a solo hobbyist. A history of `feat:`, `test:`, `refactor:` signals someone who has shipped in a team. This convention costs nothing and returns immediate credibility.

---

## Slide 9 — Stage 4: GitHub Actions CI

**Title:** 🔵 Stage 4 — Continuous Integration with GitHub Actions
**Sub-title:** Every Push and Pull Request triggers an automated cloud quality gate

### Left Column — "The Pipeline"
```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: mvn test
      - run: mvn package -DskipTests
```

### Right Column — "Why This Matters"
- **The Automator:** `mvn test` and `mvn package` run automatically on every push
- **Enforceable Quality Gates:** GitHub shows a green tick or red cross on every PR — correctness is automated, not a convention
- **Environment Parity:** `ubuntu-latest` runner mirrors the cloud production environment, not a developer laptop
- **The Connection to Stage 3:** "Merge only when CI passes and reviewer approves" from the Git workflow is now mechanically enforced — not just agreed upon

### Speaker Notes
Go back to Slide 8. "Merge only when CI passes" — how does the reviewer know CI passed? They see the green tick on the PR. CI is what makes the Git workflow enforceable. Without it, code review is a convention that can be bypassed. With it, it is a gate. This single YAML file is the difference between a hobby project and a professionally-maintained repository.

---

## Slide 10 — Stage 4: Multi-Stage Dockerfile

**Title:** 🔵 Stage 4 — Reproducible Packaging: The Multi-Stage Dockerfile
**Sub-title:** Replace host JDK dependency with a portable, secure container contract

### The Build Pipeline Diagram
```
[ STAGE 1: BUILDER ]
FROM maven:3.9.8-eclipse-temurin-21-alpine
→ Full JDK + Maven available
→ Compiles source and bundles into a fat JAR
                |
                ▼  (Only compiled .jar crosses the stage boundary)
                |
[ STAGE 2: RUNTIME ]
FROM eclipse-temurin:21-jre-alpine
→ JDK stripped — only the minimal JRE footprint (~80 MB image)
→ Non-root 'appuser' provisioned — secure container execution
→ No compiler, no build tools, no source code in the final image
```

### Left Column — "The Problem"
- Standard `java -jar` depends on the host machine's specific JRE version
- Dev laptop, CI server, and production cloud diverge silently over time
- Missing env vars, different file paths, manual classpath configuration
- "Works on my machine" is not a deployment strategy

### Right Column — "The Contract"
- Stage boundary: `COPY --from=builder` — the builder's JDK never enters the runtime image
- Enterprise security teams can scan the runtime layer and find a clean, minimal surface
- `docker run -it product-registry` behaves identically on every machine that has Docker
- Override config at runtime: `docker run -e LOGGING_LEVEL_COM_BELLS=DEBUG ...`

### Speaker Notes
Ask: "Where is the JDK in the final image?" It isn't there. The multi-stage build discards everything the builder used — Maven, the JDK, downloaded dependencies, intermediate `.class` files. Only the compiled JAR crosses the boundary. That is why this image is ~80 MB instead of 400+ MB and why a security scanner finds almost nothing to flag.

---

## Slide 11 — The Skill Comparison Matrix

**Title:** The 4-Stage Skill Comparison Matrix
**Sub-title:** Every row represents a specific, high-value skill you can defend in an engineering interview

| Architectural Concern | Stage 1: Legacy Monolith | Stage 2: Clean Layered Java | Stage 3: Spring Boot | Stage 4: Production DevOps |
|---|---|---|---|---|
| **Dependency Management** | None — static global state | Manual constructor injection | Automatic IoC container | Automatic IoC container |
| **Testing Strategy** | Untestable via automation | JUnit 5 + Mockito (milliseconds) | Fast isolated unit tests | Unit tests + MockMvc integration tests |
| **Delivery Interfaces** | Hardcoded inside console loop | Clean console controller layer | Console **or** HTTP REST API | Decoupled REST + global exception handlers |
| **Deployment Model** | Host JDK installation required | Local `target/` artifact packaging | Containerised multi-stage JRE image | GitHub Actions CI + secure cloud runtime |

### Speaker Notes
This is the single best artifact to screenshot for a LinkedIn post or portfolio README. It proves the trainee did not just build one thing — they built the same domain four times with increasing rigour and can articulate the trade-off of each step. That systematic progression is what separates a portfolio from a collection of homework assignments.

---

## Slide 12 — Interview Defense Strategy

**Title:** Interview Defense Strategy
**Sub-title:** Translate your work into the language that engineering teams recognise

### Left Column — "Student Phase Answers"
- "I split the code into layers to make it cleaner."
- "I added tests so I know it works."
- "I used Spring because it makes things easier."
- "I used Docker so it runs everywhere."
- "I used GitHub Actions for CI."

### Right Column — "Industry-Ready Answers"
- "Each layer has one reason to change — the repository has no knowledge of business rules, so a schema change never touches the service."
- "`ProductStore` is an interface mocked at the boundary; tests validate service invariants, not infrastructure state — `verifyNoInteractions(store)` proves the guard fires before the database is touched."
- "Spring eliminates manual object graphs via IoC. Adding `spring-boot-starter-web` introduces a full REST layer with zero changes to `ProductService` or any existing test."
- "The multi-stage build ensures the builder JDK never enters the runtime image; the non-root JRE container satisfies most cloud security baselines out of the box."
- "Every PR triggers `mvn test` on GitHub Actions — quality is enforced mechanically, not agreed upon."

**Footer:** The right-column answers demonstrate systems thinking. That is what engineering-led companies hire for.

### Speaker Notes
Have every trainee write both columns for their own project before their first interview. The left-column answers are not wrong — they are shallow. The right-column answers prove the candidate understood *why* each decision was made, which is the single question that separates junior candidates from hireable engineers.

---

## Trainer Notes — Content Improvement Process

1. **Anchor claims to source files.** "The service validates before calling the repository" lands harder as "line 21–24 of `ProductService.java` fires the null/blank guard before `store.exists()` is ever called."
2. **Run the pain live.** Before showing Stage 2, show `ProductApp.java` running and then ask trainees to write a unit test for it. The impossibility is the lesson.
3. **The REST demo is the payoff.** Show `ProductService` unchanged, add `starter-web`, add `ProductApiController`, hit `POST /products` with curl. Ask how many lines of `ProductService` changed. Zero.
4. **Use the matrix as a self-assessment.** Have trainees fill in Slide 11 for their own project. Empty cells are explicit, actionable gaps before job applications.
5. **Slide 7 is the portfolio screenshot.** The dual-path architecture diagram with "Untouched Core Logic" is the strongest single image a trainee can add to a LinkedIn post or README.
