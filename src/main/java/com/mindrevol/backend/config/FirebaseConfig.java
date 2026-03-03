package com.mindrevol.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    // Inject biến môi trường chứa nội dung file JSON
    @Value("${firebase.credentials.base64:}") 
    private String firebaseConfigBase64;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            GoogleCredentials credentials = null;

            // Ưu tiên 1: Đọc từ Biến môi trường (Dành cho Server/Production)
            if (firebaseConfigBase64 != null && !firebaseConfigBase64.trim().isEmpty() && !firebaseConfigBase64.contains("${FIREBASE_CREDENTIALS_BASE64}")) {
                try {
                    byte[] decodedBytes = Base64.getDecoder().decode(firebaseConfigBase64);
                    credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decodedBytes));
                    logger.info("Successfully loaded Firebase credentials from Base64 environment variable.");
                } catch (Exception e) {
                    // Nếu chuỗi Base64 bị lỗi, in ra cảnh báo và để credentials = null để chuyển sang Ưu tiên 2
                    logger.warn("Failed to load Firebase credentials from Base64 env var. It might be malformed. Falling back to local file. Error: {}", e.getMessage());
                }
            } 
            
            // Ưu tiên 2: Fallback về file local (Dành cho Development)
            if (credentials == null) {
                ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
                if (resource.exists()) {
                    credentials = GoogleCredentials.fromStream(resource.getInputStream());
                    logger.info("Successfully loaded Firebase credentials from local file 'firebase-service-account.json'.");
                } else {
                    // Nếu không có cả 2 -> Throw lỗi
                    throw new RuntimeException("Missing Firebase Credentials! No valid Base64 env var and 'firebase-service-account.json' not found.");
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