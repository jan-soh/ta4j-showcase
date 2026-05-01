FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN mkdir -p logs && chmod 777 logs
RUN mkdir -p logs/archive && chmod 777 logs/archive
COPY TA4J-Showcase-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
