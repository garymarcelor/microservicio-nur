# =============================================================================
# ms-nutrition-planner (BC2) - imagen de la aplicacion
#
# Multi-stage: compila dentro de Docker, no hace falta tener el jar ni Maven
# en la maquina. La capa de dependencias solo se invalida si cambia el pom.
#
#   docker build -t <usuario-dockerhub>/ms-nutrition-planner:1.0.0 .
# =============================================================================

FROM maven:3.9-eclipse-temurin-21-alpine AS dependency-cache
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B -e -q


FROM dependency-cache AS builder
COPY src src
# Los tests se corren aparte (mvn verify); aqui solo se empaqueta
RUN mvn package -B -e -q -DskipTests -Djacoco.skip=true && cp target/*.jar app.jar


FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuario sin privilegios
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /build/app.jar app.jar
USER appuser

EXPOSE 7000

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
