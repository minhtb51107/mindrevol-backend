package com.mindrevol.backend.modules.checkin.service.impl;

import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinReactionDetailResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CommentResponse;
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
    // Đã xóa JourneyTaskRepository
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CheckinMapper checkinMapper;
    
    private final CheckinCommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserBlockRepository userBlockRepository; 
    
    private final ReactionService reactionService; 

    // --- HELPER: Lấy danh sách ID chặn ---
    private Set<Long> getExcludedUserIds(Long userId) {
        Set<Long> blockedIds = userBlockRepository.findAllBlockedUserIdsInteraction(userId);
        blockedIds.add(-1L); 
        return blockedIds;
    }

    private CheckinResponse enrichResponse(CheckinResponse response) {
        // Lưu ý: response.getId() giờ là Long
        List<CheckinReactionDetailResponse> previews = reactionService.getPreviewReactions(response.getId());
        response.setLatestReactions(previews);
        return response;
    }

    @Override
    @CacheEvict(value = "journey_widget", key = "#p0.journeyId + '-' + #p1.id")
    @Transactional
    public CheckinResponse createCheckin(CheckinRequest request, User currentUser) {
        // [FIX] request.getJourneyId() phải là Long (Cần sửa CheckinRequest DTO nếu chưa sửa)
        Journey journey = journeyRepository.findById(request.getJourneyId())
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journey.getId(), currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên"));

        String tz = currentUser.getTimezone() != null ? currentUser.getTimezone() : "UTC";
        ZoneId userZone = ZoneId.of(tz);
        LocalDate todayLocal = LocalDate.now(userZone);

        // --- Kiểm tra ngày kết thúc ---
        if (journey.getEndDate() != null && todayLocal.isAfter(journey.getEndDate())) {
            throw new BadRequestException("Hành trình này đã kết thúc. Bạn không thể check-in thêm.");
        }

        // --- Logic Streak cơ bản: Mỗi ngày chỉ checkin 1 lần để tính streak ---
        // (Nếu muốn cho phép đăng nhiều ảnh 1 ngày thì bỏ đoạn này, nhưng chỉ tính streak lần đầu)
        if (participant.getLastCheckinAt() != null && 
            participant.getLastCheckinAt().toLocalDate().isEqual(todayLocal)) {
             // Tùy chọn: Có thể cho phép update checkin cũ hoặc block luôn
             // Ở đây giữ logic block để đơn giản hóa streak
             throw new BadRequestException("Hôm nay bạn đã check-in rồi! Hãy quay lại vào ngày mai.");
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
        // [FIX] Đã XÓA toàn bộ logic Task (JourneyTask)

        CheckinStatus finalStatus;
        if (request.getStatusRequest() == CheckinStatus.REST) {
            // [FIX] Logic REST mới: Không cần trừ vé, cứ nghỉ là nghỉ
            finalStatus = CheckinStatus.REST;
        } else {
            if (request.getStatusRequest() != null && request.getStatusRequest() != CheckinStatus.NORMAL) {
                 finalStatus = request.getStatusRequest();
            } else {
                 finalStatus = determineStatus(participant, todayLocal);
            }
        }

        // [FIX] Bỏ logic PENDING/Verification -> Luôn là finalStatus
        
        Checkin checkin = Checkin.builder()
                .user(currentUser)
                .journey(journey)
                // .task(task) -> Đã xóa trường task trong Entity Checkin (cần cập nhật Entity này)
                .imageUrl(imageUrl)
                .thumbnailUrl(imageUrl)
                .emotion(request.getEmotion())
                .status(finalStatus)
                .caption(request.getCaption())
                .visibility(request.getVisibility()) 
                .createdAt(LocalDateTime.now())
                .checkinDate(todayLocal)
                .build();

        checkin = checkinRepository.save(checkin);
        
        // Cập nhật stats
        updateParticipantStats(participant, finalStatus, todayLocal);

        // Bắn sự kiện
        eventPublisher.publishEvent(new CheckinSuccessEvent(
                checkin.getId(),
                currentUser.getId(),
                journey.getId(),
                checkin.getCreatedAt()
        ));

        return checkinMapper.toResponse(checkin);
    }

    private CheckinStatus determineStatus(JourneyParticipant participant, LocalDate todayLocal) {
        if (participant.getLastCheckinAt() == null) {
            return CheckinStatus.NORMAL;
        }
        LocalDate lastCheckin = participant.getLastCheckinAt().toLocalDate(); // Fix: convert LocalDateTime to LocalDate
        long daysGap = java.time.temporal.ChronoUnit.DAYS.between(lastCheckin, todayLocal);
        
        if (daysGap > 1) {
            return CheckinStatus.COMEBACK;
        }
        return CheckinStatus.NORMAL;
    }

    private void updateParticipantStats(JourneyParticipant participant, CheckinStatus status, LocalDate todayLocal) {
        if (status == CheckinStatus.REST) {
            // Nghỉ phép: Cập nhật ngày checkin nhưng KHÔNG tăng streak, KHÔNG reset streak
            participant.setLastCheckinAt(LocalDateTime.now()); // Hoặc todayLocal.atStartOfDay()
        } else {
            if (status == CheckinStatus.COMEBACK) {
                participant.setCurrentStreak(1); // Reset về 1
            } else {
                participant.setCurrentStreak(participant.getCurrentStreak() + 1); // Cộng dồn
            }
            participant.setLastCheckinAt(LocalDateTime.now());
            participant.setTotalCheckins(participant.getTotalCheckins() + 1); // Tăng tổng số lần
        }
        participantRepository.save(participant);
    }

    // --- GET FEED (Dùng Long ID) ---

    @Override
    @Transactional(readOnly = true)
    public Page<CheckinResponse> getJourneyFeed(Long journeyId, Pageable pageable, User currentUser) {
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
        Set<Long> excludedUserIds = getExcludedUserIds(currentUser.getId());

        // Cần đảm bảo Repository đã đổi UUID -> Long trong query
        return checkinRepository.findUnifiedFeed(currentUser.getId(), cursor, excludedUserIds, pageable)
                .stream()
                .map(checkinMapper::toResponse)
                .map(this::enrichResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinResponse> getJourneyFeedByCursor(Long journeyId, User currentUser, LocalDateTime cursor, int limit) {
        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền xem hành trình này");
        }
        if (cursor == null) cursor = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, limit);
        Set<Long> excludedUserIds = getExcludedUserIds(currentUser.getId());

        return checkinRepository.findJourneyFeedByCursor(journeyId, cursor, excludedUserIds, pageable)
                .stream()
                .map(checkinMapper::toResponse)
                .map(this::enrichResponse)
                .collect(Collectors.toList());
    }
    
    // --- COMMENT ---

    @Override
    @Transactional
    public CommentResponse postComment(Long checkinId, String content, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        // [FIX] Bỏ check InteractionType (đã xóa)
        
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
    public Page<CommentResponse> getComments(Long checkinId, Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Set<Long> excludedUserIds = getExcludedUserIds(currentUserId);
        
        return commentRepository.findByCheckinId(checkinId, excludedUserIds, pageable)
                .map(checkinMapper::toCommentResponse);
    }

    @Override
    @Transactional
    public CheckinResponse updateCheckin(Long checkinId, String caption, User currentUser) {
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
    public void deleteCheckin(Long checkinId, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        if (!checkin.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền xóa bài viết này");
        }

        Journey journey = checkin.getJourney();
        
        // [FIX] Bỏ logic trả lại vé REST

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

        // [NOTE] Cần đảm bảo checkinRepository.findValidCheckinDates dùng Long ID
        List<LocalDateTime> history = checkinRepository.findValidCheckinDates(
                participant.getJourney().getId(), 
                user.getId()
        );

        if (history.isEmpty()) {
            participant.setCurrentStreak(0);
            participant.setLastCheckinAt(null);
            participantRepository.save(participant);
            return;
        }

        // Logic tính toán streak giữ nguyên, chỉ đảm bảo các biến ngày tháng đúng kiểu
        LocalDateTime lastCheckinTime = history.get(0);
        participant.setLastCheckinAt(lastCheckinTime);

        int streak = 0;
        Set<LocalDate> uniqueDates = history.stream()
                .map(dt -> dt.atZone(ZoneId.of("UTC")).withZoneSameInstant(userZone).toLocalDate())
                .collect(Collectors.toSet());

        LocalDate cursorDate = today;
        
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