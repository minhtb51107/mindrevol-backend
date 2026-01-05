package com.mindrevol.backend.modules.payment.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import com.mindrevol.backend.modules.payment.dto.SePayWebhookRequest;
import com.mindrevol.backend.modules.payment.entity.PaymentTransaction;
import com.mindrevol.backend.modules.payment.repository.PaymentTransactionRepository;
import com.mindrevol.backend.modules.user.entity.AccountType;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PaymentTransactionRepository transactionRepository;

    @Value("${app.payment.sepay-token:MY_SECRET_TOKEN}")
    private String sepayApiToken;

    private static final int PRICE_1_MONTH = 20000;  
    private static final int PRICE_1_YEAR  = 200000; 

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SePayWebhookRequest request) {

        log.info("🔔 WEBHOOK RECEIVED: {}", request);

        // 1. [BẢO MẬT] Kiểm tra Token từ SePay
        // SePay gửi format: "Apikey {token}"
        if (authHeader == null || !authHeader.contains(sepayApiToken)) {
            log.warn("Webhook Unauthorized access attempt. Header: {}", authHeader);
            return ResponseEntity.ok(ApiResponse.success(null)); // Vẫn trả 200 để SePay không retry spam
        }

        // 2. Idempotency: Tránh xử lý trùng
        if (transactionRepository.existsByGatewayRefId(String.valueOf(request.getId()))) {
            log.info("Transaction {} already processed.", request.getId());
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        // 3. Phân tích User ID
        String userId = extractUserIdFromContent(request.getContent());
        if (userId == null) {
            log.error("Cannot find UserID in content: {}", request.getContent());
            saveTransaction(null, request, "FAILED_NO_USER_ID");
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        // 4. Xử lý Nâng Cấp
        userRepository.findById(userId).ifPresentOrElse(user -> {
            double amount = request.getTransferAmount();
            int monthsToAdd = calculateMonths((int) amount);

            if (monthsToAdd > 0) {
                upgradeUserSubscription(user, monthsToAdd);
                saveTransaction(user.getId(), request, "SUCCESS_UPGRADE_" + monthsToAdd + "M");
                
                notificationService.sendAndSaveNotification(
                    user.getId(), null, NotificationType.SYSTEM,
                    "Nâng cấp thành công! 🌟",
                    "Bạn đã được nâng cấp lên gói GOLD (" + monthsToAdd + " tháng).",
                    null, null
                );
            } else {
                saveTransaction(user.getId(), request, "FAILED_INVALID_AMOUNT");
            }

        }, () -> {
            log.error("User ID {} not found.", userId);
            saveTransaction(userId, request, "FAILED_USER_NOT_FOUND");
        });

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private int calculateMonths(int amount) {
        if (amount >= PRICE_1_YEAR) {
            int years = amount / PRICE_1_YEAR;
            int remaining = amount % PRICE_1_YEAR;
            return (years * 12) + (remaining / PRICE_1_MONTH);
        }
        if (amount >= PRICE_1_MONTH) return amount / PRICE_1_MONTH; 
        return 0;
    }

    private void upgradeUserSubscription(User user, int months) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiry;
        if (user.isPremium()) {
            newExpiry = user.getSubscriptionExpiryDate().plusMonths(months);
        } else {
            newExpiry = now.plusMonths(months);
        }
        user.setAccountType(AccountType.GOLD);
        user.setSubscriptionExpiryDate(newExpiry);
        userRepository.save(user);
    }

    private void saveTransaction(String userId, SePayWebhookRequest request, String status) {
        try {
            User user = null;
            if (userId != null) user = userRepository.findById(userId).orElse(null);
            
            PaymentTransaction tx = PaymentTransaction.builder()
                    .user(user)
                    .amount(request.getTransferAmount())
                    .gateway("SEPAY")
                    .gatewayRefId(String.valueOf(request.getId()))
                    .content(request.getContent())
                    .status(status)
                    .build();
            transactionRepository.save(tx);
        } catch (Exception e) {
            log.error("Failed to save transaction log", e);
        }
    }

    private String extractUserIdFromContent(String content) {
        if (content == null) return null;
        Pattern pattern = Pattern.compile("MINDREVOL\\s*(\\S+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
}