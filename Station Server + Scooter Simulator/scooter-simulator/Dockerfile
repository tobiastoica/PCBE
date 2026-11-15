FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG JAR_FILE=target/scooter-simulator-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENV SERVER_PORT=8088
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
