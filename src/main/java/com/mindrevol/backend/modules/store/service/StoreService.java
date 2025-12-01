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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreItemRepository storeItemRepository;
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public List<StoreItem> getActiveItems() {
        return storeItemRepository.findByIsActiveTrue();
    }

    @Transactional
    public void buyItem(Long userId, String itemCode) {
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
        
        // 3. Áp dụng hiệu ứng (Giao hàng)
        if (item.getEffectType() == ItemEffectType.ADD_FREEZE_STREAK) {
            // Cộng thêm vé vào kho
            user.setFreezeStreakCount(user.getFreezeStreakCount() + item.getEffectValue());
        } 
        
        // 4. Lưu User và Lịch sử giao dịch
        userRepository.save(user);

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount(-item.getPrice()) // Số âm vì là trừ tiền
                .balanceAfter(user.getPoints())
                .reason("Mua vật phẩm: " + item.getName())
                .source(PointSource.SHOP_PURCHASE)
                .build();
        pointHistoryRepository.save(history);
    }
}