package com.mindrevol.backend.modules.checkin.listener;

import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinVisibility;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.feed.service.FeedService;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.user.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedEventListener {

    private final CheckinRepository checkinRepository;
    private final JourneyParticipantRepository participantRepository;
    private final FriendshipRepository friendshipRepository;
    private final FeedService feedService;

    @Async("taskExecutor") // Chạy bất đồng bộ để không làm chậm API trả về
    @EventListener
    @Transactional(readOnly = true)
    public void handleFanOut(CheckinSuccessEvent event) {
        log.info("Starting Feed Fan-out for Checkin: {}", event.getCheckinId());

        Checkin checkin = checkinRepository.findById(event.getCheckinId()).orElse(null);
        if (checkin == null) return;

        Long authorId = checkin.getUser().getId();
        CheckinVisibility visibility = checkin.getVisibility();

        // 1. Luôn push vào feed của chính tác giả
        feedService.pushToFeed(authorId, checkin.getId());

        if (visibility == CheckinVisibility.PRIVATE) {
            return; // Không push cho ai khác
        }

        // 2. Lấy danh sách tất cả thành viên trong Journey
        List<JourneyParticipant> participants = participantRepository.findAllByJourneyId(checkin.getJourney().getId());

        // 3. Lấy danh sách bạn bè của tác giả (nếu cần filter FRIENDS_ONLY)
        Set<Long> friendIds = null;
        if (visibility == CheckinVisibility.FRIENDS_ONLY) {
            friendIds = friendshipRepository.findAllAcceptedFriendsList(authorId).stream()
                    .map(f -> f.getFriend(authorId).getId()) // Helper method trong Entity Friendship
                    .collect(Collectors.toSet());
        }

        // 4. Duyệt và Push
        for (JourneyParticipant p : participants) {
            Long memberId = p.getUser().getId();
            
            // Bỏ qua tác giả (đã push rồi)
            if (memberId.equals(authorId)) continue;

            boolean shouldPush = false;

            if (visibility == CheckinVisibility.PUBLIC) {
                shouldPush = true;
            } else if (visibility == CheckinVisibility.FRIENDS_ONLY) {
                if (friendIds != null && friendIds.contains(memberId)) {
                    shouldPush = true;
                }
            }

            if (shouldPush) {
                feedService.pushToFeed(memberId, checkin.getId());
            }
        }
        
        log.info("Fan-out completed for Checkin {}", checkin.getId());
    }
}