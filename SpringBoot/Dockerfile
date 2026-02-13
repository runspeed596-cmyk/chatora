# ===========================================
# Chatora Spring Boot API — Production Dockerfile
# ===========================================
# Multi-stage build for smaller, more secure image

# ── Stage 1: Build ──
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle files for dependency caching
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Download dependencies (cached layer)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application
RUN ./gradlew bootJar --no-daemon

# ── Stage 2: Runtime ──
FROM eclipse-temurin:21-jre-alpine

# Security: create non-root user
RUN addgroup -S Chatora && adduser -S Chatora -G Chatora

WORKDIR /app

# Copy only the built JAR from builder stage
COPY --from=builder /app/build/libs/SpringBoot-0.0.1-SNAPSHOT.jar app.jar

# Create uploads directory
RUN mkdir -p /app/uploads && chown -R Chatora:Chatora /app

# Switch to non-root user
USER Chatora

# Expose port
EXPOSE 8080

# JVM production flags for performance & security
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+UseStringDeduplication", \
    "-Xmx512m", \
    "-Xms256m", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/app/heapdump.hprof", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dserver.tomcat.remoteip.remote-ip-header=X-Forwarded-For", \
    "-Dserver.tomcat.remoteip.protocol-header=X-Forwarded-Proto", \
    "-jar", "app.jar", \
    "--spring.profiles.active=prod"]
