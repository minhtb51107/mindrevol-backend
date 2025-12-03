package com.mindrevol.backend.modules.user.listener;

import com.mindrevol.backend.modules.user.event.UserBlockedEvent;
import com.mindrevol.backend.modules.user.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserInteractionListener {

    private final FriendshipService friendshipService;

    @Async
    @EventListener
    @Transactional
    public void handleUserBlocked(UserBlockedEvent event) {
        log.info("Handling Block Event: {} blocked {}", event.getBlockerId(), event.getBlockedId());
        
        try {
            // Tự động hủy kết bạn nếu có
            friendshipService.removeFriendship(event.getBlockerId(), event.getBlockedId());
        } catch (Exception e) {
            // Không làm gì cả nếu họ chưa kết bạn
            log.debug("No friendship to remove or error occurred: {}", e.getMessage());
        }
    }
}