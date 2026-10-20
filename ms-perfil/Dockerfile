# ── Etapa 1: compilación ──
# JDK 21 (LTS). El pom declara java.version 20 y Spring Boot compila con
# --release ${java.version}, así que el bytecode generado sigue siendo Java 20.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Primero solo el pom: las dependencias quedan en una capa que se reutiliza
# mientras el pom no cambie.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# ── Etapa 2: ejecución ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios para no correr la aplicación como root.
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
