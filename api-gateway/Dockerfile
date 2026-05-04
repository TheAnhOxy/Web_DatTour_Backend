# Stage 1: Build với Maven (Sử dụng cache để build nhanh)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml trước để tải các thư viện (cache dependency)
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime (Sử dụng JRE nhẹ để chạy app)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tạo user 'spring' để chạy ứng dụng (Không dùng root để bảo mật)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy file jar từ stage build sang
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# Tối ưu hóa JVM cho môi trường Container
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]