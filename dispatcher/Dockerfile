# ─────────────────────────────────────────────────────────────
# STAGE 1 — BUILD
# Uses a full Maven + JDK image to compile and package the app.
# This image is large (~600MB) but we throw it away after building.
# ─────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set working directory inside the container
WORKDIR /build

# Copy the ROOT pom.xml first (so Maven can resolve the parent)
COPY pom.xml .

# Copy the dispatcher module pom.xml
COPY dispatcher/pom.xml dispatcher/

# Download all dependencies first (cached layer — speeds up rebuilds)
# If pom.xml didn't change, Docker reuses this cached layer
RUN mvn dependency:go-offline -pl dispatcher -am -q

# Now copy the actual source code
COPY dispatcher/src dispatcher/src

# Build ONLY the dispatcher module, skip tests (tests run separately)
RUN mvn clean package -pl dispatcher -am -DskipTests -q

# ─────────────────────────────────────────────────────────────
# STAGE 2 — RUN
# Uses a tiny JRE-only image (~180MB). No Maven, no source code.
# Just the compiled .jar file.
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy ONLY the .jar from the builder stage — nothing else
COPY --from=builder /build/dispatcher/target/*.jar app.jar

# Document which port this service uses (informational)
EXPOSE 8080

# Health check — Docker will restart the container if this fails
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]