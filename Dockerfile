# --- Giai đoạn 1: Build (Dùng Gradle & JDK 21) ---
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .

# Cấp quyền thực thi cho file gradlew (quan trọng để tránh lỗi Permission denied)
RUN chmod +x gradlew

# Build ra file .jar (Bỏ qua test cho nhanh)
# --no-daemon: Giúp build ổn định hơn trên môi trường Docker
RUN ./gradlew clean bootJar -x test --no-daemon

# --- Giai đoạn 2: Run (Chạy ứng dụng với JRE 21) ---
# Dùng eclipse-temurin bản 21 (Bản chuẩn, ổn định nhất hiện nay thay thế openjdk)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Cài đặt FFmpeg (Cần thiết cho tính năng video/ảnh của bạn)
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

# Copy file .jar từ giai đoạn build sang
# Gradle lưu file build ở /build/libs/ thay vì /target/
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]