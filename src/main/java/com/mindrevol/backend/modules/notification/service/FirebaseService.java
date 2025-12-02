package com.mindrevol.backend.modules.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseService {

    private final FirebaseMessaging firebaseMessaging;

    public void sendNotification(String fcmToken, String title, String body, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            return;
        }

        try {
            // Xây dựng message
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            // Thêm data payload (để Client xử lý điều hướng khi bấm vào)
            if (data != null) {
                messageBuilder.putAllData(data);
            }

            // Gửi
            String response = firebaseMessaging.send(messageBuilder.build());
            log.info("Sent FCM message: {}", response);

        } catch (Exception e) {
            log.error("Failed to send FCM message to token: {}", fcmToken, e);
            // Không throw exception để tránh làm lỗi luồng chính
        }
    }
}