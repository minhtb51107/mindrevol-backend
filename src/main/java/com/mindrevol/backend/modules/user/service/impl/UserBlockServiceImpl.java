package com.mindrevol.backend.modules.user.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserBlock;
import com.mindrevol.backend.modules.user.event.UserBlockedEvent;
import com.mindrevol.backend.modules.user.mapper.UserMapper;
import com.mindrevol.backend.modules.user.repository.FriendshipRepository;
import com.mindrevol.backend.modules.user.repository.UserBlockRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserBlockServiceImpl implements UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void blockUser(Long currentUserId, Long blockedId) {
        if (currentUserId.equals(blockedId)) {
            throw new BadRequestException("Không thể tự chặn chính mình");
        }

        // 1. Tạo block record nếu chưa có
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, blockedId)) {
            User blocker = userRepository.findById(currentUserId).orElseThrow();
            User blocked = userRepository.findById(blockedId)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

            UserBlock block = UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked)
                    .createdAt(LocalDateTime.now())
                    .build();
            userBlockRepository.save(block);
            
            // Gửi event để các module khác xử lý (ví dụ: feed, chat)
            eventPublisher.publishEvent(new UserBlockedEvent(currentUserId, blockedId));
        }
        
        // 2. Tự động Hủy kết bạn (Unfriend) 2 chiều
        friendshipRepository.deleteByRequesterIdAndAddresseeId(currentUserId, blockedId);
        friendshipRepository.deleteByRequesterIdAndAddresseeId(blockedId, currentUserId);
    }

    @Override
    @Transactional
    public void unblockUser(Long currentUserId, Long blockedId) {
        userBlockRepository.findByBlockerIdAndBlockedId(currentUserId, blockedId)
                .ifPresent(userBlockRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getBlockList(Long currentUserId) {
        return userBlockRepository.findAllByBlockerId(currentUserId).stream()
                .map(block -> userMapper.toSummaryResponse(block.getBlocked()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(Long userId, Long targetUserId) {
        boolean isBlockedByMe = userBlockRepository.existsByBlockerIdAndBlockedId(userId, targetUserId);
        boolean isBlockedByTarget = userBlockRepository.existsByBlockerIdAndBlockedId(targetUserId, userId);
        return isBlockedByMe || isBlockedByTarget;
    }
}