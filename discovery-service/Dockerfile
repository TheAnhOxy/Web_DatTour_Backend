# Stage build: Vẫn dùng maven nhưng xóa cache ngay sau khi build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage runtime: Dùng JRE bản Alpine (Chỉ ~100MB thay vì ~300MB)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Xóa bớt các file rác không cần thiết trong container
ENTRYPOINT ["java", "-jar", "app.jar"]
