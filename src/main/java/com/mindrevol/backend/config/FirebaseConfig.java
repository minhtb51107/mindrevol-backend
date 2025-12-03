package com.mindrevol.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value; // <--- Import
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream; // <--- Import
import java.io.IOException;
import java.nio.charset.StandardCharsets; // <--- Import
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    // Inject biến môi trường chứa nội dung file JSON (đã mã hóa Base64 để tránh lỗi ký tự xuống dòng)
    @Value("${firebase.credentials.base64:}") 
    private String firebaseConfigBase64;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            GoogleCredentials credentials;

            // Ưu tiên 1: Đọc từ Biến môi trường (Production)
            if (firebaseConfigBase64 != null && !firebaseConfigBase64.isEmpty()) {
                byte[] decodedBytes = Base64.getDecoder().decode(firebaseConfigBase64);
                credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decodedBytes));
            } 
            // Ưu tiên 2: Fallback về file local (Development)
            else {
                ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
                if (resource.exists()) {
                    credentials = GoogleCredentials.fromStream(resource.getInputStream());
                } else {
                    // Nếu không có cả 2 -> Chạy mode giả lập hoặc throw lỗi (tùy chọn)
                    // Ở đây tôi throw lỗi để bạn biết mà config
                    throw new RuntimeException("Missing Firebase Credentials! Set FIREBASE_CREDENTIALS_BASE64 env var.");
                }
            }
            
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}