# STAGE 1 — BUILD
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
COPY idea-service/pom.xml idea-service/

RUN mvn dependency:go-offline -pl idea-service -am -q

COPY idea-service/src idea-service/src

RUN mvn clean package -pl idea-service -am -DskipTests -q

# STAGE 2 — RUN
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/idea-service/target/*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]