
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app


ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} app.jar

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]

EXPOSE 8084