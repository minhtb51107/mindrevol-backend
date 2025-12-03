package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserBlock;
import com.mindrevol.backend.modules.user.event.UserBlockedEvent; // <--- IMPORT MỚI
import com.mindrevol.backend.modules.user.repository.UserBlockRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
// import com.mindrevol.backend.modules.user.service.FriendshipService; // <--- XÓA
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher; // <--- IMPORT MỚI
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    // private final FriendshipService friendshipService; // <--- XÓA
    private final ApplicationEventPublisher eventPublisher; // <--- INJECT MỚI

    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BadRequestException("Bạn không thể tự chặn chính mình");
        }

        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new BadRequestException("Bạn đã chặn người dùng này rồi");
        }

        User blocker = userRepository.findById(blockerId).orElseThrow();
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        // 1. Tạo record chặn
        UserBlock block = UserBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();
        userBlockRepository.save(block);

        // 2. Bắn sự kiện "Đã chặn" để các module khác (Friendship) tự xử lý
        eventPublisher.publishEvent(new UserBlockedEvent(blockerId, blockedId));
    }

    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        UserBlock block = userBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new BadRequestException("Bạn chưa chặn người dùng này"));
        
        userBlockRepository.delete(block);
    }
}