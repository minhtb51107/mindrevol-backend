package com.mindrevol.backend.modules.checkin.service.impl;

import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinReactionDetailResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CommentResponse;
import com.mindrevol.backend.modules.checkin.entity.ActivityType;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinComment;
import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.event.CommentPostedEvent;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinCommentRepository;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.checkin.service.CheckinService;
import com.mindrevol.backend.modules.checkin.service.ReactionService;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.storage.service.FileStorageService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserBlockRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CheckinMapper checkinMapper;
    
    private final CheckinCommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserBlockRepository userBlockRepository; 
    
    private final ReactionService reactionService; 

    private Set<String> getExcludedUserIds(String userId) {
        Set<String> blockedIds = userBlockRepository.findAllBlockedUserIdsInteraction(userId);
        if (blockedIds == null) blockedIds = new HashSet<>();
        blockedIds.add("-1"); 
        return blockedIds;
    }

    private CheckinResponse enrichResponse(CheckinResponse response) {
        List<CheckinReactionDetailResponse> previews = reactionService.getPreviewReactions(response.getId());
        response.setLatestReactions(previews);
        return response;
    }

    @Override
    @CacheEvict(value = "journey_widget", key = "#request.journeyId + '-' + #currentUser.id")
    @Transactional
    public CheckinResponse createCheckin(CheckinRequest request, User currentUser) {
        Journey journey = journeyRepository.findById(request.getJourneyId())
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journey.getId(), currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên"));

        String tz = currentUser.getTimezone() != null ? currentUser.getTimezone() : "UTC";
        ZoneId userZone = ZoneId.of(tz);
        LocalDate todayLocal = LocalDate.now(userZone);

     // [FIX] Cho phép check-in nốt hôm nay (isAfter trả về true nếu LỚN HƠN hẳn)
     // Hoặc an toàn hơn: Cho phép trễ 1 ngày để tránh lệch múi giờ
     if (journey.getEndDate() != null && todayLocal.isAfter(journey.getEndDate().plusDays(1))) {
         // Chỉ chặn khi đã trễ quá 1 ngày so với ngày kết thúc
         throw new BadRequestException("Hành trình này đã kết thúc (Hạn chót: " + journey.getEndDate() + ").");
     }

        if (participant.getLastCheckinAt() != null && 
            participant.getLastCheckinAt().toLocalDate().isEqual(todayLocal)) {
             throw new BadRequestException("Hôm nay bạn đã check-in rồi!");
        }

        String imageUrl = "";
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            imageUrl = fileStorageService.uploadFile(request.getFile());
        }

        return saveCheckinTransaction(currentUser, journey, participant, request, imageUrl, todayLocal);
    }
    
    @Transactional
    protected CheckinResponse saveCheckinTransaction(User currentUser, Journey journey, 
                                                   JourneyParticipant participant, 
                                                   CheckinRequest request, 
                                                   String imageUrl,
                                                   LocalDate todayLocal) {
        // [CẬP NHẬT] Logic Status mới: Mặc định là NORMAL (Active)
        // Loại bỏ logic tự động tính toán LATE/COMEBACK phức tạp.
        CheckinStatus finalStatus = CheckinStatus.NORMAL;
        
        // Chỉ dùng REST nếu user chủ động chọn chế độ nghỉ ngơi
        if (request.getStatusRequest() == CheckinStatus.REST) {
            finalStatus = CheckinStatus.REST;
        }

        // Logic xử lý tên hoạt động
        String finalActivityName = request.getActivityName();
        if (finalActivityName != null && finalActivityName.trim().isEmpty()) {
            finalActivityName = null;
        }

        Checkin checkin = Checkin.builder()
                .user(currentUser)
                .journey(journey)
                .imageUrl(imageUrl)
                .thumbnailUrl(imageUrl) // Sẽ được xử lý resize async sau
                .caption(request.getCaption())
                
                // [MỚI] Mapping Context Data
                .emotion(request.getEmotion())
                .activityType(request.getActivityType() != null ? request.getActivityType() : ActivityType.DEFAULT)
                .activityName(finalActivityName)
                .locationName(request.getLocationName())
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                
                .status(finalStatus)
                .visibility(request.getVisibility()) 
                .createdAt(LocalDateTime.now())
                .checkinDate(todayLocal)
                .build();

        checkin = checkinRepository.save(checkin);
        
        // Vẫn cập nhật streak để hiển thị huy hiệu (Platform vẫn cần gamification ngầm)
        updateParticipantStats(participant, finalStatus, todayLocal);

        eventPublisher.publishEvent(new CheckinSuccessEvent(
                checkin.getId(),
                currentUser.getId(),
                journey.getId(),
                checkin.getCreatedAt()
        ));

        return checkinMapper.toResponse(checkin);
    }

    // [CẬP NHẬT] Hàm tính toán Stats độc lập, không phụ thuộc vào Status enum
    private void updateParticipantStats(JourneyParticipant participant, CheckinStatus status, LocalDate todayLocal) {
        boolean isFirstCheckinToday = false;
        
        if (participant.getLastCheckinAt() == null) {
            isFirstCheckinToday = true;
        } else {
            LocalDate lastDate = participant.getLastCheckinAt().toLocalDate();
            if (!lastDate.equals(todayLocal)) {
                isFirstCheckinToday = true;
            }
        }

        if (isFirstCheckinToday) {
            participant.setTotalActiveDays(participant.getTotalActiveDays() + 1);
        }

        if (status == CheckinStatus.REST) {
            // Nếu nghỉ thì chỉ cập nhật thời gian, giữ nguyên streak
            participant.setLastCheckinAt(LocalDateTime.now());
        } else {
            // Logic tính Streak dựa trên khoảng cách ngày
            if (participant.getLastCheckinAt() == null) {
                participant.setCurrentStreak(1);
            } else {
                LocalDate lastDate = participant.getLastCheckinAt().toLocalDate();
                long daysGap = ChronoUnit.DAYS.between(lastDate, todayLocal);
                
                if (daysGap > 1) {
                    // Mất chuỗi -> Reset về 1
                    participant.setCurrentStreak(1);
                } else if (daysGap == 1) {
                    // Liên tục -> Tăng chuỗi
                    participant.setCurrentStreak(participant.getCurrentStreak() + 1);
                }
                // Nếu daysGap == 0 (cùng ngày) -> Không làm gì (vì đã chặn checkin trùng ngày ở trên, nhưng giữ logic safe)
            }
            
            participant.setLastCheckinAt(LocalDateTime.now());
            participant.setTotalCheckins(participant.getTotalCheckins() + 1);
        }
        
        participantRepository.save(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CheckinResponse> getJourneyFeed(String journeyId, Pageable pageable, User currentUser) {
        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, currentUser.getId())) {
            throw new BadRequestException("Không có quyền xem");
        }
        return checkinRepository.findByJourneyIdOrderByCreatedAtDesc(journeyId, pageable)
                .map(checkinMapper::toResponse)
                .map(this::enrichResponse); 
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinResponse> getUnifiedFeed(User currentUser, LocalDateTime cursor, int limit) {
        if (cursor == null) cursor = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, limit);
        Set<String> excludedUserIds = getExcludedUserIds(currentUser.getId());

        return checkinRepository.findUnifiedFeed(currentUser.getId(), cursor, excludedUserIds, pageable)
                .stream()
                .map(checkinMapper::toResponse)
                .map(this::enrichResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinResponse> getJourneyFeedByCursor(String journeyId, User currentUser, LocalDateTime cursor, int limit) {
        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền xem hành trình này");
        }
        if (cursor == null) cursor = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, limit);
        Set<String> excludedUserIds = getExcludedUserIds(currentUser.getId());

        return checkinRepository.findJourneyFeedByCursor(journeyId, cursor, excludedUserIds, pageable)
                .stream()
                .map(checkinMapper::toResponse)
                .map(this::enrichResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public CommentResponse postComment(String checkinId, String content, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));
        
        if (userBlockRepository.existsByBlockerIdAndBlockedId(checkin.getUser().getId(), currentUser.getId())) {
             throw new BadRequestException("Bạn không thể bình luận bài viết này.");
        }

        CheckinComment comment = CheckinComment.builder()
                .checkin(checkin)
                .user(currentUser)
                .content(content)
                .build();
        
        comment = commentRepository.save(comment);
        eventPublisher.publishEvent(new CommentPostedEvent(checkin, currentUser, content));
        return checkinMapper.toCommentResponse(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(String checkinId, Pageable pageable) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        Set<String> excludedUserIds = getExcludedUserIds(currentUserId);
        
        return commentRepository.findByCheckinId(checkinId, excludedUserIds, pageable)
                .map(checkinMapper::toCommentResponse);
    }

    @Override
    @Transactional
    public CheckinResponse updateCheckin(String checkinId, String caption, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        if (!checkin.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền sửa bài viết này");
        }

        checkin.setCaption(caption);
        checkin = checkinRepository.save(checkin);
        return checkinMapper.toResponse(checkin);
    }

    @Override
    @Transactional
    @CacheEvict(value = "journey_widget", allEntries = true)
    public void deleteCheckin(String checkinId, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        if (!checkin.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền xóa bài viết này");
        }

        Journey journey = checkin.getJourney();
        checkinRepository.delete(checkin);
        checkinRepository.flush(); 

        JourneyParticipant participant = participantRepository
                .findByJourneyIdAndUserId(journey.getId(), currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        recalculateParticipantStats(participant, currentUser);
    }

    private void recalculateParticipantStats(JourneyParticipant participant, User user) {
        String tz = user.getTimezone() != null ? user.getTimezone() : "UTC";
        ZoneId userZone = ZoneId.of(tz);
        LocalDate today = LocalDate.now(userZone);

        List<LocalDateTime> history = checkinRepository.findValidCheckinDates(
                participant.getJourney().getId(), 
                user.getId()
        );

        if (history.isEmpty()) {
            participant.setCurrentStreak(0);
            participant.setTotalCheckins(0);
            participant.setTotalActiveDays(0); 
            participant.setLastCheckinAt(null);
            participantRepository.save(participant);
            return;
        }

        participant.setTotalCheckins(history.size());

        Set<LocalDate> uniqueDates = history.stream()
                .map(dt -> dt.atZone(ZoneId.of("UTC")).withZoneSameInstant(userZone).toLocalDate())
                .collect(Collectors.toSet());
        participant.setTotalActiveDays(uniqueDates.size());

        LocalDateTime lastCheckinTime = history.get(0);
        participant.setLastCheckinAt(lastCheckinTime);

        int streak = 0;
        LocalDate cursorDate = today;
        
        // Logic tính streak này đã đúng (kiểm tra liên tục ngược dòng thời gian)
        // Nếu hôm nay chưa check-in thì kiểm tra hôm qua
        if (!uniqueDates.contains(cursorDate)) {
            if (uniqueDates.contains(cursorDate.minusDays(1))) {
                cursorDate = cursorDate.minusDays(1); 
            } else {
                participant.setCurrentStreak(0);
                participantRepository.save(participant);
                return;
            }
        }

        while (uniqueDates.contains(cursorDate)) {
            streak++;
            cursorDate = cursorDate.minusDays(1);
        }

        participant.setCurrentStreak(streak);
        participantRepository.save(participant);
    }
}