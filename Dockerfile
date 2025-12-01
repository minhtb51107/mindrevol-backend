# Stage 1: Build
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
# Skip test ở bước này vì CI đã chạy test rồi, giúp build nhanh hơn
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Run
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]