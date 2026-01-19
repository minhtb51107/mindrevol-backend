package com.mindrevol.backend.modules.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.backend.modules.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    // Lấy API Key từ biến môi trường (Lúc này sẽ là key của SendGrid)
    @Value("${app.email.api-key}") 
    private String apiKey;

    // Email này BẮT BUỘC phải trùng với email bạn đã "Verify Single Sender" trên SendGrid
    @Value("${app.email.sender-email}")
    private String senderEmail;

    @Value("${app.email.sender-name:MindRevol}")
    private String senderName;

    private final ObjectMapper objectMapper;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            log.info("🚀 Sending email via SendGrid API to: {}", to);

            // --- CẤU TRÚC JSON CHUẨN CỦA SENDGRID ---
            // Tài liệu: https://docs.sendgrid.com/api-reference/mail-send/mail-send
            Map<String, Object> body = Map.of(
                "personalizations", List.of(Map.of(
                    "to", List.of(Map.of("email", to)),
                    "subject", subject
                )),
                "from", Map.of(
                    "email", senderEmail,
                    "name", senderName
                ),
                "content", List.of(Map.of(
                    "type", "text/html",
                    "value", content
                ))
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                    .header("Authorization", "Bearer " + apiKey) // SendGrid dùng Bearer Token
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // SendGrid trả về 202 Accepted là thành công (Khác với Brevo là 201)
            if (response.statusCode() == 202 || response.statusCode() == 200) {
                log.info("✅ Email sent successfully via SendGrid!");
            } else {
                log.error("❌ Failed to send via SendGrid. Status: {}, Body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Failed to send email via SendGrid API");
            }

        } catch (Exception e) {
            log.error("❌ Exception sending email via API", e);
            throw new RuntimeException("Error sending email", e);
        }
    }
}