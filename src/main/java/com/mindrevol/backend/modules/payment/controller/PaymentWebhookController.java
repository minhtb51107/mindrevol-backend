package com.mindrevol.backend.modules.payment.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.gamification.entity.PointSource;
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
    private final PaymentTransactionRepository transactionRepository;

    @Value("${app.payment.sepay-token:MY_SECRET_TOKEN}")
    private String sepayApiToken;

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SePayWebhookRequest request) {

        log.info("WEBHOOK RECEIVED: {}", request);

        // 1. Check Security
        if (authHeader == null || !authHeader.contains(sepayApiToken)) {
            log.warn("Webhook Unauthorized");
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        // 2. Check Idempotency
        if (transactionRepository.existsByGatewayRefId(String.valueOf(request.getId()))) {
            log.info("Giao dịch {} đã được xử lý trước đó. Bỏ qua.", request.getId());
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        // 3. Chỉ xử lý tiền vào
        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        // 4. Parse UserID (String UUID hoặc Handle)
        String userId = extractUserIdFromContent(request.getContent());
        if (userId == null) {
            log.error("Không tìm thấy User identifier trong nội dung: {}", request.getContent());
            saveTransaction(null, request, "FAILED_NO_USER");
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        // 5. Xử lý cộng tiền
        // [FIX] findById nhận String
        userRepository.findById(userId).ifPresentOrElse(user -> {
            int pointsToAdd = (int) request.getTransferAmount();
            
            // a. Cộng điểm (Dùng signature mới của GamificationService: awardPoints(String userId, ...))
            // Lưu ý: Cần đảm bảo PointSource.PAYMENT tồn tại hoặc dùng source khác
            gamificationService.awardPoints(
                user.getId(), 
                pointsToAdd, 
                PointSource.CHECKIN, // Tạm dùng CHECKIN hoặc bạn thêm PAYMENT vào Enum PointSource
                "Nạp tiền SePay #" + request.getId(),
                String.valueOf(request.getId())
            );
            
            // b. Lưu transaction
            saveTransaction(user.getId(), request, "SUCCESS");

            // c. Báo user (NotificationService đã sửa sang String ID)
            notificationService.sendAndSaveNotification(
                    user.getId(), 
                    null, // System sender
                    NotificationType.SYSTEM,
                    "Tiền đã về ví! 💰",
                    "Đã cộng " + pointsToAdd + " điểm. Mã GD: " + request.getId(),
                    null, 
                    null
            );
            log.info("SUCCESS: User {} +{} points", user.getId(), pointsToAdd);

        }, () -> {
            log.error("User ID {} không tồn tại", userId);
            saveTransaction(userId, request, "FAILED_USER_NOT_FOUND");
        });

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // [UUID] userId là String
    private void saveTransaction(String userId, SePayWebhookRequest request, String status) {
        try {
            PaymentTransaction tx = PaymentTransaction.builder()
                    // Nếu userId null hoặc không tìm thấy thì set user = null
                    .user(userId != null ? userRepository.findById(userId).orElse(null) : null)
                    .amount(request.getTransferAmount())
                    .gateway("SEPAY")
                    .gatewayRefId(String.valueOf(request.getId()))
                    .content(request.getContent())
                    .status(status)
                    .build();
            transactionRepository.save(tx);
        } catch (Exception e) {
            log.error("Lỗi khi lưu transaction log", e);
        }
    }

    // [UUID] Trả về String
    private String extractUserIdFromContent(String content) {
        if (content == null) return null;
        
        // [FIX] Regex giờ chấp nhận chuỗi (để bắt UUID hoặc Handle)
        // Ví dụ: MINDREVOL user-handle-123 hoặc MINDREVOL <UUID>
        // \\S+ nghĩa là chuỗi ký tự không khoảng trắng
        Pattern pattern = Pattern.compile("MINDREVOL\\s*(\\S+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        
        // Nếu tìm thấy, trả về group 1 (chuỗi sau MINDREVOL)
        return matcher.find() ? matcher.group(1) : null;
    }
}