FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
USER spring

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Xms256m", "-Xmx512m", \
  "-XX:+UseContainerSupport", \
  "-XX:TieredStopAtLevel=1", \
  "-jar", "app.jar"]
