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

import java.util.HashSet;
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
    public void handleCacheInvalidation(CheckinSuccessEvent event) {
        // Sự kiện: Có ai đó vừa đăng bài -> Cần làm mới feed cho những người liên quan
        log.info("Processing Feed Invalidation for Checkin: {}", event.getCheckinId());

        Checkin checkin = checkinRepository.findById(event.getCheckinId()).orElse(null);
        if (checkin == null) return;

        String authorId = checkin.getUser().getId();
        CheckinVisibility visibility = checkin.getVisibility();
        String journeyId = checkin.getJourney().getId();

        // Tập hợp danh sách các user cần xóa cache (dùng Set để tránh trùng lặp)
        Set<String> usersToInvalidate = new HashSet<>();

        // 1. Luôn xóa cache của tác giả (để họ thấy bài của chính mình)
        usersToInvalidate.add(authorId);

        if (visibility == CheckinVisibility.PRIVATE) {
            doInvalidate(usersToInvalidate);
            return;
        }

        // 2. Lấy thành viên trong hành trình (vì bài đăng thuộc hành trình)
        List<JourneyParticipant> participants = participantRepository.findAllByJourneyId(journeyId);
        
        // 3. Lấy danh sách bạn bè (nếu visibility là FRIENDS_ONLY)
        Set<String> friendIds = new HashSet<>();
        if (visibility == CheckinVisibility.FRIENDS_ONLY) {
            friendIds = friendshipRepository.findAllAcceptedFriendsList(authorId).stream()
                    .map(f -> f.getFriend(authorId).getId())
                    .collect(Collectors.toSet());
        }

        // 4. Lọc danh sách
        for (JourneyParticipant p : participants) {
            String memberId = p.getUser().getId();
            if (memberId.equals(authorId)) continue;

            if (visibility == CheckinVisibility.PUBLIC) {
                usersToInvalidate.add(memberId);
            } else if (visibility == CheckinVisibility.FRIENDS_ONLY) {
                if (friendIds.contains(memberId)) {
                    usersToInvalidate.add(memberId);
                }
            }
        }

        // 5. Thực hiện xóa cache
        doInvalidate(usersToInvalidate);
    }

    private void doInvalidate(Set<String> userIds) {
        log.info("Invalidating feed cache for {} users.", userIds.size());
        for (String userId : userIds) {
            feedService.evictFeedCache(userId);
        }
    }
}