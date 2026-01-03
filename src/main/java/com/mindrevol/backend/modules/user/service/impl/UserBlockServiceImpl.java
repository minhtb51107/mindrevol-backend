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
    public void blockUser(String currentUserId, String blockedId) { // [UUID]
        if (currentUserId.equals(blockedId)) {
            throw new BadRequestException("Không thể tự chặn chính mình");
        }

        if (!userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, blockedId)) {
            User blocker = userRepository.findById(currentUserId).orElseThrow();
            User blocked = userRepository.findById(blockedId)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

            // [FIX] Xóa .createdAt(), để BaseEntity tự động xử lý
            UserBlock block = UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked)
                    .build();
            
            userBlockRepository.save(block);
            
            eventPublisher.publishEvent(new UserBlockedEvent(currentUserId, blockedId));
        }
        
        // Hủy kết bạn 2 chiều
        friendshipRepository.deleteByRequesterIdAndAddresseeId(currentUserId, blockedId);
        friendshipRepository.deleteByRequesterIdAndAddresseeId(blockedId, currentUserId);
    }

    @Override
    @Transactional
    public void unblockUser(String currentUserId, String blockedId) { // [UUID]
        userBlockRepository.findByBlockerIdAndBlockedId(currentUserId, blockedId)
                .ifPresent(userBlockRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getBlockList(String currentUserId) { // [UUID]
        return userBlockRepository.findAllByBlockerId(currentUserId).stream()
                .map(block -> userMapper.toSummaryResponse(block.getBlocked()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(String userId, String targetUserId) { // [UUID]
        boolean isBlockedByMe = userBlockRepository.existsByBlockerIdAndBlockedId(userId, targetUserId);
        boolean isBlockedByTarget = userBlockRepository.existsByBlockerIdAndBlockedId(targetUserId, userId);
        return isBlockedByMe || isBlockedByTarget;
    }
}