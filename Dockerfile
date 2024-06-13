FROM openjdk:21

WORKDIR /app

COPY src/main/resources/static /app/static
COPY target/ebbinghaus-memory-telegram-app-0.0.1.jar app.jar
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]