# --- Giai đoạn 1: Build (Dùng Gradle & JDK 21) ---
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .

# Cấp quyền thực thi
RUN chmod +x gradlew

# Build file .jar (Bỏ test để build nhanh hơn)
RUN ./gradlew clean bootJar -x test --no-daemon

# --- Giai đoạn 2: Run (Chạy ứng dụng với JRE 21) ---
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Cài đặt FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# [CẤU HÌNH FINAL]
# 1. Giữ Heap 130MB (để App chạy khỏe như lúc nãy).
# 2. Thêm -Djava.net.preferIPv4Stack=true (Để fix lỗi mạng gửi mail).
# 3. Các thông số SerialGC, CodeCache giữ nguyên để tiết kiệm RAM hệ thống.
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-Xms130m", "-Xmx130m", "-XX:MaxMetaspaceSize=140m", "-XX:ReservedCodeCacheSize=64m", "-XX:MaxDirectMemorySize=32m", "-XX:+UseSerialGC", "-Xss256k", "-jar", "app.jar"]