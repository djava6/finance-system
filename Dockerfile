# Stage 1: build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copia o wrapper e os arquivos de configuração do Gradle primeiro (cache layer)
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

# Baixa dependências
RUN ./gradlew dependencies --no-daemon -q

# Compila o projeto
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
USER spring

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Xms256m", "-Xmx512m", \
  "-XX:+UseContainerSupport", \
  "-XX:TieredStopAtLevel=1", \
  "-jar", "app.jar"]
