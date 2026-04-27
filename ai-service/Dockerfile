# STAGE 1 — BUILD
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
COPY dispatcher/pom.xml dispatcher/
COPY auth-service/pom.xml auth-service/
COPY idea-service/pom.xml idea-service/
COPY deal-service/pom.xml deal-service/
COPY ai-service/pom.xml ai-service/
# Parent pom lists common/ and desktop-client/ as modules — both must exist for validation.
COPY common/pom.xml common/
COPY desktop-client/pom.xml desktop-client/

RUN mvn dependency:go-offline -pl ai-service -am -q

COPY ai-service/src ai-service/src

RUN mvn clean package -pl ai-service -am -DskipTests -q

# STAGE 2 — RUN
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/ai-service/target/*.jar app.jar

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]