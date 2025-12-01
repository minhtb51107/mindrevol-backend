package com.mindrevol.backend.modules.notification.service;

public interface EmailService {

    /**
     * Gửi một email đơn giản.
     * @param to Địa chỉ email người nhận.
     * @param subject Chủ đề của email.
     * @param body Nội dung của email (hỗ trợ HTML).
     */
    void sendEmail(String to, String subject, String body);
}