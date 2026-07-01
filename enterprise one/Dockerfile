# ── Stage 1: Builder ──────────────────────────────────────────────────────────
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache dependency layer separately from source
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user for least-privilege execution
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/target/enterprise-product-registry-1.0.0.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
