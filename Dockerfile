# Stage 1: build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Baixa dependências primeiro (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compila o projeto
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]