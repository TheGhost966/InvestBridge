# STAGE 1 — BUILD
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
COPY dispatcher/pom.xml dispatcher/
COPY auth-service/pom.xml auth-service/
COPY idea-service/pom.xml idea-service/
COPY deal-service/pom.xml deal-service/
COPY ai-service/pom.xml ai-service/

RUN mvn dependency:go-offline -pl deal-service -am -q

COPY deal-service/src deal-service/src

RUN mvn clean package -pl deal-service -am -DskipTests -q

# STAGE 2 — RUN
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/deal-service/target/*.jar app.jar

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]