FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
VOLUME /data
ARG JAR_FILE=target/charging-station-service-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENV SERVER_PORT=8084
ENV SQLITE_DB_PATH=/data/station.db
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
