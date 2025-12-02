package com.mindrevol.backend.modules.store.service;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.gamification.entity.PointHistory;
import com.mindrevol.backend.modules.gamification.entity.PointSource;
import com.mindrevol.backend.modules.gamification.repository.PointHistoryRepository;
import com.mindrevol.backend.modules.store.entity.ItemEffectType;
import com.mindrevol.backend.modules.store.entity.StoreItem;
import com.mindrevol.backend.modules.store.repository.StoreItemRepository;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Thêm Log
import org.springframework.orm.ObjectOptimisticLockingFailureException; // Thêm Exception
import org.springframework.retry.annotation.Backoff; // Thêm Retry
import org.springframework.retry.annotation.Retryable; // Thêm Retry
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // Log để debug
public class StoreService {

    private final StoreItemRepository storeItemRepository;
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public List<StoreItem> getActiveItems() {
        return storeItemRepository.findByIsActiveTrue();
    }

    @Transactional
    // --- THÊM ĐOẠN NÀY ---
    // Nếu gặp lỗi phiên bản (do race condition), tự động thử lại tối đa 3 lần, mỗi lần cách nhau 50ms
    @Retryable(
        value = { ObjectOptimisticLockingFailureException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 50)
    )
    // ---------------------
    public void buyItem(Long userId, String itemCode) {
        // Cần fetch lại User mới nhất từ DB trong mỗi lần retry để lấy point và version mới
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        StoreItem item = storeItemRepository.findByCode(itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        // 1. Kiểm tra tiền
        if (user.getPoints() < item.getPrice()) {
            throw new BadRequestException("Bạn không đủ điểm! Cần " + item.getPrice() + " nhưng chỉ có " + user.getPoints());
        }

        // 2. Trừ tiền
        user.setPoints(user.getPoints() - item.getPrice());
        
        // 3. Áp dụng hiệu ứng
        if (item.getEffectType() == ItemEffectType.ADD_FREEZE_STREAK) {
            user.setFreezeStreakCount(user.getFreezeStreakCount() + item.getEffectValue());
        } 
        
        // 4. Lưu User (Nếu version lệch, JPA sẽ ném exception tại đây và kích hoạt Retry)
        userRepository.save(user);

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount(-item.getPrice())
                .balanceAfter(user.getPoints())
                .reason("Mua vật phẩm: " + item.getName())
                .source(PointSource.SHOP_PURCHASE)
                .build();
        pointHistoryRepository.save(history);
        
        log.info("User {} bought item {} successfully. Points left: {}", userId, itemCode, user.getPoints());
    }
}