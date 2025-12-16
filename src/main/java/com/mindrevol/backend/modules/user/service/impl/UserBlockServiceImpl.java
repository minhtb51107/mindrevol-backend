package com.mindrevol.backend.modules.user.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserBlock;
import com.mindrevol.backend.modules.user.event.UserBlockedEvent;
import com.mindrevol.backend.modules.user.mapper.UserMapper;
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
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void blockUser(Long userId, Long blockId) {
        if (userId.equals(blockId)) {
            throw new BadRequestException("Không thể tự chặn chính mình");
        }

        // SỬA: findByUserId... -> findByBlockerId...
        if (userBlockRepository.findByBlockerIdAndBlockedId(userId, blockId).isPresent()) {
            throw new BadRequestException("Bạn đã chặn người dùng này rồi");
        }

        User user = userRepository.findById(userId).orElseThrow();
        User blockedUser = userRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        UserBlock block = UserBlock.builder()
                .blocker(user)
                .blocked(blockedUser)
                .build();
        userBlockRepository.save(block);
        
        eventPublisher.publishEvent(new UserBlockedEvent(userId, blockId));
    }

    @Override
    @Transactional
    public void unblockUser(Long userId, Long blockId) {
        // SỬA: findByUserId... -> findByBlockerId...
        userBlockRepository.findByBlockerIdAndBlockedId(userId, blockId)
                .ifPresent(userBlockRepository::delete);
    }

    @Override
    public List<UserSummaryResponse> getBlockedUsers(Long userId) {
        return userBlockRepository.findAllByBlockerId(userId).stream()
                .map(block -> userMapper.toSummaryResponse(block.getBlocked()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isBlocked(Long userId, Long targetUserId) {
        // SỬA: findByUserId... -> findByBlockerId...
        boolean isBlockedByMe = userBlockRepository.findByBlockerIdAndBlockedId(userId, targetUserId).isPresent();
        boolean isBlockedByTarget = userBlockRepository.findByBlockerIdAndBlockedId(targetUserId, userId).isPresent();
        
        return isBlockedByMe || isBlockedByTarget;
    }
}