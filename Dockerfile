# ============================================================
# Stage 1: BUILD — Maven + JDK builds the fat JAR
# This stage is large but gets thrown away after building
# ============================================================

# lightweight Linux image with Java 21 and development tools installed
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory inside the build container
WORKDIR /build

# Copy Maven wrapper and pom.xml first
# This layer is cached — dependencies only re-download if pom.xml changes
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (cached layer unless pom.xml changes)
RUN ./mvnw dependency:go-offline -q

# Now copy source code (changes more often — separate layer)
COPY src ./src

# Build the fat JAR, skip tests (tests run in CI before this step)
RUN ./mvnw package -DskipTests -q

# ============================================================
# Stage 2: RUNTIME — minimal JRE only
# Only the JAR comes from Stage 1 — nothing else
# ============================================================

#lightweight Java runtime environment where the final application will run
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: don't run as root inside the container
RUN addgroup -S gateway && adduser -S gateway -G gateway

WORKDIR /app

# Copy ONLY the fat JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Create the config directory and set ownership
RUN mkdir -p /config && chown gateway:gateway /config

# Switch to non-root user
USER gateway

# Expose the KeyPilot port
EXPOSE 4000

# Health check — Docker and orchestrators use this to know when ready
HEALTHCHECK \
  --interval=10s \
  --timeout=5s \
  --start-period=30s \
  --retries=3 \
  CMD wget -q --spider http://localhost:4000/health || exit 1

# Environment variables with sensible defaults
ENV SERVER_PORT=4000
ENV KEYPILOT_STORAGE_PATH=/config/keys.json
ENV KEYPILOT_DEFAULT_PROVIDER=openai
ENV KEYPILOT_RETRY_MAX_ATTEMPTS=3

# Entrypoint — runs the JAR with environment-driven config
ENTRYPOINT ["java", "-jar", "app.jar"]