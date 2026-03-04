# STAGE 1 — BUILD
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy root pom.xml
COPY pom.xml .

# Copy ALL module pom.xml files so Maven can resolve the multi-module structure
COPY dispatcher/pom.xml dispatcher/
COPY auth-service/pom.xml auth-service/
COPY idea-service/pom.xml idea-service/
COPY deal-service/pom.xml deal-service/
COPY ai-service/pom.xml ai-service/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl dispatcher -am -q

# Copy only dispatcher source code
COPY dispatcher/src dispatcher/src

# Build only dispatcher
RUN mvn clean package -pl dispatcher -am -DskipTests -q

# STAGE 2 — RUN
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/dispatcher/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]