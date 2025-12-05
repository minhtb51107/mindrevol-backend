package com.mindrevol.backend.modules.payment.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import com.mindrevol.backend.modules.payment.dto.SePayWebhookRequest;
import com.mindrevol.backend.modules.payment.entity.PaymentTransaction;
import com.mindrevol.backend.modules.payment.repository.PaymentTransactionRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final UserRepository userRepository;
    private final GamificationService gamificationService;
    private final NotificationService notificationService;
    private final PaymentTransactionRepository transactionRepository; // [MỚI]

    @Value("${app.payment.sepay-token:MY_SECRET_TOKEN}")
    private String sepayApiToken;

    @PostMapping("/webhook")
    @Transactional // [MỚI] Đảm bảo giao dịch Atomic (hoặc thành công hết, hoặc rollback hết)
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SePayWebhookRequest request) {

        log.info("WEBHOOK RECEIVED: {}", request);

        // 1. Check Security
        if (authHeader == null || !authHeader.contains(sepayApiToken)) {
            log.warn("Webhook Unauthorized");
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        // 2. Check Trùng lặp (Idempotency Check) - QUAN TRỌNG NHẤT
        // SePay ID là duy nhất. Nếu đã xử lý ID này rồi thì bỏ qua ngay.
        if (transactionRepository.existsByGatewayRefId(String.valueOf(request.getId()))) {
            log.info("Giao dịch {} đã được xử lý trước đó. Bỏ qua.", request.getId());
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        // 3. Chỉ xử lý tiền vào
        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        // 4. Parse UserID
        Long userId = extractUserIdFromContent(request.getContent());
        if (userId == null) {
            log.error("Không tìm thấy UserID trong nội dung: {}", request.getContent());
            // Vẫn lưu transaction nhưng trạng thái FAILED_NO_USER để tra soát
            saveTransaction(null, request, "FAILED_NO_USER");
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        // 5. Xử lý cộng tiền
        userRepository.findById(userId).ifPresentOrElse(user -> {
            int pointsToAdd = (int) request.getTransferAmount();
            
            // a. Cộng điểm
            gamificationService.awardPoints(user, pointsToAdd, "Nạp tiền SePay #" + request.getId());
            
            // b. Lưu lịch sử giao dịch (Biên lai)
            saveTransaction(user.getId(), request, "SUCCESS");

            // c. Báo user
            notificationService.sendAndSaveNotification(
                    user.getId(), null, NotificationType.SYSTEM,
                    "Tiền đã về ví! 💰",
                    "Đã cộng " + pointsToAdd + " điểm. Mã GD: " + request.getId(),
                    null, null
            );
            log.info("SUCCESS: User {} +{} points", userId, pointsToAdd);

        }, () -> {
            log.error("UserID {} không tồn tại", userId);
            saveTransaction(userId, request, "FAILED_USER_NOT_FOUND");
        });

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private void saveTransaction(Long userId, SePayWebhookRequest request, String status) {
        try {
            PaymentTransaction tx = PaymentTransaction.builder()
                    .user(userId != null ? userRepository.getReferenceById(userId) : null)
                    .amount(request.getTransferAmount())
                    .gateway("SEPAY")
                    .gatewayRefId(String.valueOf(request.getId())) // Khóa chống trùng
                    .content(request.getContent())
                    .status(status)
                    .build();
            transactionRepository.save(tx);
        } catch (Exception e) {
            log.error("Lỗi khi lưu transaction log", e);
            // Không throw exception để tránh SePay retry liên tục nếu lỗi DB log
        }
    }

    private Long extractUserIdFromContent(String content) {
        if (content == null) return null;
        Pattern pattern = Pattern.compile("MINDREVOL\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }
}