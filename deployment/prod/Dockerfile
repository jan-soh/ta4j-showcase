FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY TS4J-Showcase-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]