# =============================================================================
# Multi-stage Dockerfile for the Calculator REST API
#   Stage 1 (build):   Maven + full JDK 17  -> compiles and packages target/calculator.jar
#   Stage 2 (runtime): slim JRE 17 on Alpine -> contains ONLY the JAR, runs as non-root
# The final image never contains Maven, the JDK, source code or the ~/.m2 cache.
# =============================================================================

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Layer caching: copy only pom.xml first and download dependencies.
# This layer is reused on every build until pom.xml changes, so code-only
# changes do not re-download the internet.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy the sources (changes often) and build the executable JAR.
# Tests are run by the Jenkins "Test" stage, so they are skipped here.
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL org.opencontainers.image.title="calculator-api" \
      org.opencontainers.image.description="Calculator REST API (Spring Boot) - CSE 816 SPE mini-project" \
      org.opencontainers.image.source="https://github.com/Nishu1508/calculator-devops"

# Security: create and use an unprivileged system user instead of root.
RUN addgroup -S spring && adduser -S -G spring -H -s /sbin/nologin spring

WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/target/calculator.jar app.jar
USER spring:spring

# JVM sizes its heap relative to the container memory limit.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    SERVER_PORT=8080

EXPOSE 8080

# Container-level health check (busybox wget is available in Alpine).
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# Exec form: java is PID 1 and receives SIGTERM directly -> graceful shutdown.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
