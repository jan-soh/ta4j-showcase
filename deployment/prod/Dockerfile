FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN mkdir -p /opt/ts4j_showcase/logs && chmod 777 /opt/ts4j_showcase/logs
COPY TS4J-Showcase-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
