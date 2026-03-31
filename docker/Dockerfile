FROM openjdk:17-jdk-slim
COPY ../auth-center-bootstrap/target/auth-center-bootstrap-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
