package com.mindrevol.backend.modules.journey.recap.service.impl;

import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.recap.dto.*;
import com.mindrevol.backend.modules.journey.recap.service.JourneyRecapService;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JourneyRecapServiceImpl implements JourneyRecapService {

    private final JourneyParticipantRepository participantRepository;
    private final CheckinRepository checkinRepository;

    @Override
    @Transactional(readOnly = true)
    public JourneyRecapResponse generateRecap(com.mindrevol.backend.modules.user.entity.User user, UUID journeyId) {
        
        // 1. Lấy thông tin cơ bản
        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journeyId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        Journey journey = participant.getJourney();

        // 2. Lấy toàn bộ check-in (để phân tích)
        List<Checkin> allCheckins = checkinRepository.findByJourneyIdAndUserId(journeyId, user.getId());
        
        // Sắp xếp theo thời gian tăng dần
        allCheckins.sort(Comparator.comparing(Checkin::getCreatedAt));

        List<RecapSlide> slides = new ArrayList<>();

        // === SCENE 1: INTRO ===
        slides.add(RecapSlide.builder()
                .type(RecapSlideType.INTRO)
                .title("Hành trình của bạn")
                .subtitle(journey.getName())
                .date(participant.getJoinedAt().toLocalDate())
                .build());

        if (allCheckins.isEmpty()) {
            // Nếu chưa check-in gì cả thì trả về Intro + Outro thôi
            slides.add(RecapSlide.builder()
                    .type(RecapSlideType.OUTRO)
                    .title("Chưa có dữ liệu")
                    .subtitle("Hãy bắt đầu check-in ngay hôm nay!")
                    .build());
            return buildResponse(journey, slides);
        }

        // === SCENE 2: FIRST CHECKIN ===
        Checkin firstCheckin = allCheckins.get(0);
        slides.add(RecapSlide.builder()
                .type(RecapSlideType.FIRST_CHECKIN)
                .title("Ngày đầu tiên")
                .subtitle(firstCheckin.getCaption() != null ? firstCheckin.getCaption() : "Khởi đầu đầy năng lượng!")
                .imageUrl(firstCheckin.getImageUrl())
                .date(firstCheckin.getCreatedAt().toLocalDate())
                .build());

        // === SCENE 3: MOST LIKED (Khoảnh khắc tỏa sáng) ===
        // Giả sử Checkin entity chưa có field likeCount, ta có thể tạm bỏ qua hoặc dùng logic mock
        // Ở đây tôi lấy check-in có caption dài nhất làm "tâm huyết nhất" (hoặc bạn có thể query count reaction)
        // TODO: Nâng cấp query findCheckinWithMostReactions()
        Checkin randomHighlight = allCheckins.get(allCheckins.size() / 2); // Lấy cái ở giữa làm điểm nhấn
        slides.add(RecapSlide.builder()
                .type(RecapSlideType.MOST_LIKED)
                .title("Khoảnh khắc đáng nhớ")
                .subtitle("Bạn đã làm rất tốt!")
                .imageUrl(randomHighlight.getImageUrl())
                .date(randomHighlight.getCreatedAt().toLocalDate())
                .build());

        // === SCENE 4: COMEBACK (Nếu có) ===
        allCheckins.stream()
                .filter(c -> c.getStatus() == CheckinStatus.COMEBACK)
                .findFirst()
                .ifPresent(comeback -> slides.add(RecapSlide.builder()
                        .type(RecapSlideType.COMEBACK)
                        .title("Cú lội ngược dòng")
                        .subtitle("Dù có khó khăn, bạn vẫn quay trở lại!")
                        .imageUrl(comeback.getImageUrl())
                        .date(comeback.getCreatedAt().toLocalDate())
                        .build()));

        // === SCENE 5: STATS ===
        slides.add(RecapSlide.builder()
                .type(RecapSlideType.STATS)
                .title("Thành quả của bạn")
                .subtitle("Bạn đã kiên trì đến cùng")
                .streakCount(participant.getCurrentStreak()) // Chuỗi hiện tại
                .reactionCount(allCheckins.size()) // Tổng số ngày check-in
                .build());

        // === SCENE 6: OUTRO ===
        slides.add(RecapSlide.builder()
                .type(RecapSlideType.OUTRO)
                .title("Keep going!")
                .subtitle("Tiếp tục giữ vững phong độ nhé!")
                .build());

        return buildResponse(journey, slides);
    }

    private JourneyRecapResponse buildResponse(Journey journey, List<RecapSlide> slides) {
        return JourneyRecapResponse.builder()
                .journeyId(journey.getId())
                .journeyName(journey.getName())
                .slides(slides)
                .build();
    }
}