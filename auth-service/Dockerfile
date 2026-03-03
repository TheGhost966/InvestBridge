# STAGE 1 — BUILD
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
COPY auth-service/pom.xml auth-service/

RUN mvn dependency:go-offline -pl auth-service -am -q

COPY auth-service/src auth-service/src

RUN mvn clean package -pl auth-service -am -DskipTests -q

# STAGE 2 — RUN
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/auth-service/target/*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]