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

    @Async("taskExecutor")
    @EventListener
    @Transactional(readOnly = true)
    public void handleFanOut(CheckinSuccessEvent event) {
        log.info("Starting Feed Fan-out for Checkin: {}", event.getCheckinId());

        // event.getCheckinId() là String -> OK
        Checkin checkin = checkinRepository.findById(event.getCheckinId()).orElse(null);
        if (checkin == null) return;

        String authorId = checkin.getUser().getId();
        CheckinVisibility visibility = checkin.getVisibility();

        // 1. Luôn push vào feed của chính tác giả
        feedService.pushToFeed(authorId, checkin.getId());

        if (visibility == CheckinVisibility.PRIVATE) {
            return; 
        }

        // 2. Lấy danh sách thành viên (Check lại JourneyParticipantRepository xem đã đổi UUID chưa)
        // checkin.getJourney().getId() trả về String -> OK
        List<JourneyParticipant> participants = participantRepository.findAllByJourneyId(checkin.getJourney().getId());

        // 3. Lấy danh sách bạn bè
        Set<String> friendIds = null;
        if (visibility == CheckinVisibility.FRIENDS_ONLY) {
            // friendshipRepository cần nhận String authorId
            friendIds = friendshipRepository.findAllAcceptedFriendsList(authorId).stream()
                    .map(f -> f.getFriend(authorId).getId()) 
                    .collect(Collectors.toSet());
        }

        // 4. Duyệt và Push
        for (JourneyParticipant p : participants) {
            String memberId = p.getUser().getId();
            
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
                // feedService cần nhận (String userId, String checkinId)
                feedService.pushToFeed(memberId, checkin.getId());
            }
        }
        
        log.info("Fan-out completed for Checkin {}", checkin.getId());
    }
}