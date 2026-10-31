# ===================================================================
# Dockerfile for BankEase — NextGen Net Banking Application
# Multi-stage build ensuring minimal production image footprint
# ===================================================================

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and dependencies specification
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Pre-fetch dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build application jar
COPY src src
RUN ./mvnw clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for banking application security
RUN addgroup -S bankgroup && adduser -S bankuser -G bankgroup
USER bankuser

COPY --from=builder /app/target/bankease-backend-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8081
EXPOSE 8081

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
