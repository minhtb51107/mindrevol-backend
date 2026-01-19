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

# Cài đặt FFmpeg (Giữ lại nhưng hạn chế dùng)
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# [QUAN TRỌNG] CẤU HÌNH TỐI ƯU RAM CHO 512MB SERVER
# -Xms128m -Xmx180m: Cấp tối thiểu 128MB, tối đa 180MB cho Heap.
# -XX:+UseSerialGC: Dùng bộ dọn rác đơn luồng (nhẹ nhất cho CPU/RAM thấp).
# -Xss256k: Giảm dung lượng stack mỗi luồng (mặc định 1MB -> 256KB) để tiết kiệm RAM khi có nhiều request.
# -XX:MaxMetaspaceSize=100m: Giới hạn bộ nhớ chứa class metadata.
# Giảm Heap xuống 130MB (đủ cho app chạy), Tăng Metaspace lên 200MB (để load đủ thư viện)
ENTRYPOINT ["java", "-Xms130m", "-Xmx130m", "-XX:+UseSerialGC", "-Xss256k", "-XX:MaxMetaspaceSize=200m", "-jar", "app.jar"]