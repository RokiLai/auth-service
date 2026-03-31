FROM eclipse-temurin:17-jre-jammy

ARG JAR_FILE=auth-center-bootstrap/target/auth-center-bootstrap-1.0-SNAPSHOT.jar

WORKDIR /app

COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
