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
import com.mindrevol.backend.modules.journey.entity.InteractionType;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.entity.JourneyTask;
import com.mindrevol.backend.modules.journey.entity.JourneyType;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyTaskRepository;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final JourneyTaskRepository journeyTaskRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CheckinMapper checkinMapper;
    
    private final CheckinCommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserBlockRepository userBlockRepository; 
    
    private final ReactionService reactionService; 

    // --- HELPER: Lấy danh sách ID chặn (để lọc query) ---
    private Set<Long> getExcludedUserIds(Long userId) {
        // Lấy danh sách: người mình chặn + người chặn mình
        Set<Long> blockedIds = userBlockRepository.findAllBlockedUserIdsInteraction(userId);
        // [QUAN TRỌNG] Luôn thêm -1L vào set để tránh lỗi SQL "NOT IN (empty)"
        blockedIds.add(-1L); 
        return blockedIds;
    }

    private CheckinResponse enrichResponse(CheckinResponse response) {
        List<CheckinReactionDetailResponse> previews = reactionService.getPreviewReactions(response.getId());
        response.setLatestReactions(previews);
        return response;
    }

    @Override
    @CacheEvict(value = "journey_widget", key = "#p0.journeyId + '-' + #p1.id")
    @Transactional
    public CheckinResponse createCheckin(CheckinRequest request, User currentUser) {
        Journey journey = journeyRepository.findById(request.getJourneyId())
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journey.getId(), currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên"));

        String tz = currentUser.getTimezone() != null ? currentUser.getTimezone() : "UTC";
        ZoneId userZone;
        try {
            userZone = ZoneId.of(tz);
        } catch (Exception e) {
            userZone = ZoneId.of("UTC");
        }
        LocalDate todayLocal = LocalDate.now(userZone);

        // --- Kiểm tra ngày kết thúc của hành trình ---
        if (journey.getEndDate() != null && todayLocal.isAfter(journey.getEndDate())) {
            throw new BadRequestException("Hành trình này đã kết thúc vào ngày " + journey.getEndDate() + ". Bạn không thể check-in thêm.");
        }

        if (journey.isHasStreak()) {
            if (participant.getLastCheckinAt() != null && participant.getLastCheckinAt().isEqual(todayLocal)) {
                throw new BadRequestException("Hôm nay bạn đã check-in rồi! Hãy quay lại vào ngày mai.");
            }
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
        JourneyTask task = null;

        if (journey.getType() == JourneyType.ROADMAP) {
            if (request.getTaskId() != null) {
                task = journeyTaskRepository.findById(request.getTaskId())
                        .orElseThrow(() -> new ResourceNotFoundException("Nhiệm vụ không tồn tại"));

                if (!task.getJourney().getId().equals(journey.getId())) {
                    throw new BadRequestException("Nhiệm vụ này không thuộc hành trình hiện tại");
                }

                if (checkinRepository.existsByUserIdAndTaskId(currentUser.getId(), task.getId())) {
                    throw new BadRequestException("Bạn đã hoàn thành nhiệm vụ này rồi!");
                }

                // --- Kiểm tra thứ tự nhiệm vụ (Không cho làm tắt) ---
                Optional<JourneyTask> prevTaskOpt = journeyTaskRepository
                    .findFirstByJourneyIdAndDayNoLessThanOrderByDayNoDesc(journey.getId(), task.getDayNo());
                
                if (prevTaskOpt.isPresent()) {
                    JourneyTask prevTask = prevTaskOpt.get();
                    boolean isPrevDone = checkinRepository.existsByUserIdAndTaskId(currentUser.getId(), prevTask.getId());
                    if (!isPrevDone) {
                        throw new BadRequestException("Vui lòng hoàn thành nhiệm vụ ngày " + prevTask.getDayNo() + " trước!");
                    }
                }
            }
        }

        CheckinStatus finalStatus;
        if (request.getStatusRequest() == CheckinStatus.REST) {
            if (journey.isRequiresFreezeTicket()) {
                if (currentUser.getFreezeStreakCount() <= 0) {
                    throw new BadRequestException("Bạn đã hết vé Nghỉ Phép! Hãy tích điểm để đổi thêm.");
                }
                currentUser.setFreezeStreakCount(currentUser.getFreezeStreakCount() - 1);
                userRepository.save(currentUser);
            }
            finalStatus = CheckinStatus.REST;
        } else {
            // Logic ưu tiên Status từ Client gửi lên (tránh lỗi COMPLETE/COMEBACK sai)
            if (request.getStatusRequest() != null && request.getStatusRequest() != CheckinStatus.NORMAL) {
                 finalStatus = request.getStatusRequest();
            } else {
                 finalStatus = determineStatus(participant, todayLocal);
            }
        }

        // --- Xử lý trạng thái PENDING nếu cần xác thực ---
        CheckinStatus initialStatus = finalStatus;
        if (finalStatus != CheckinStatus.REST && journey.isRequiresVerification()) {
            initialStatus = CheckinStatus.PENDING;
        }

        Checkin checkin = Checkin.builder()
                .user(currentUser)
                .journey(journey)
                .task(task)
                .imageUrl(imageUrl)
                .thumbnailUrl(imageUrl)
                .emotion(request.getEmotion())
                .status(initialStatus) // Lưu status thực tế (có thể là PENDING)
                .caption(request.getCaption())
                .visibility(request.getVisibility()) 
                .createdAt(LocalDateTime.now())
                .checkinDate(todayLocal) // [FIX LỖI QUAN TRỌNG]: Thêm trường này để tránh lỗi NOT NULL
                .build();

        checkin = checkinRepository.save(checkin);
        
        // Cập nhật stats (Streak, LastCheckin)
        updateParticipantStats(participant, finalStatus, todayLocal);

        // Chỉ bắn sự kiện tính điểm/thông báo nếu KHÔNG phải chờ duyệt
        if (initialStatus != CheckinStatus.PENDING) {
            eventPublisher.publishEvent(new CheckinSuccessEvent(
                    checkin.getId(),
                    currentUser.getId(),
                    journey.getId(),
                    checkin.getCreatedAt()
            ));
        }

        return checkinMapper.toResponse(checkin);
    }

    private CheckinStatus determineStatus(JourneyParticipant participant, LocalDate todayLocal) {
        if (participant.getLastCheckinAt() == null) {
            return CheckinStatus.NORMAL;
        }
        LocalDate lastCheckin = participant.getLastCheckinAt();
        long daysGap = java.time.temporal.ChronoUnit.DAYS.between(lastCheckin, todayLocal);
        
        // Gap > 1 nghĩa là đã bỏ lỡ ít nhất 1 ngày -> COMEBACK (Reset streak)
        if (daysGap > 1) {
            return CheckinStatus.COMEBACK;
        }
        return CheckinStatus.NORMAL;
    }

    private void updateParticipantStats(JourneyParticipant participant, CheckinStatus status, LocalDate todayLocal) {
        if (status == CheckinStatus.REST) {
            participant.setLastCheckinAt(todayLocal);
            // Vé nghỉ phép giữ nguyên streak, không reset cũng không cộng
        } else {
            if (status == CheckinStatus.COMEBACK) {
                participant.setCurrentStreak(1); // Reset về 1
            } else {
                participant.setCurrentStreak(participant.getCurrentStreak() + 1); // Cộng dồn
            }
            participant.setLastCheckinAt(todayLocal);
        }
        participantRepository.save(participant);
    }

    // --- CÁC HÀM GET FEED (CÓ LỌC CHẶN) ---

    @Override
    @Transactional(readOnly = true)
    public Page<CheckinResponse> getJourneyFeed(UUID journeyId, Pageable pageable, User currentUser) {
        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, currentUser.getId())) {
            throw new BadRequestException("Không có quyền xem");
        }
        // Feed trong nhóm thì thường vẫn hiện hết, trừ khi bạn muốn chặn cả trong nhóm.
        // Nếu muốn chặn trong nhóm, cần viết lại query findByJourneyId... để nhận list excludedUserIds
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

        return checkinRepository.findUnifiedFeed(currentUser.getId(), cursor, excludedUserIds, pageable)
                .stream()
                .map(checkinMapper::toResponse)
                .map(this::enrichResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinResponse> getJourneyFeedByCursor(UUID journeyId, User currentUser, LocalDateTime cursor, int limit) {
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
    
    // --- COMMENT & UPDATE/DELETE ---

    @Override
    @Transactional
    public CommentResponse postComment(UUID checkinId, String content, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        if (checkin.getJourney().getInteractionType() == InteractionType.RESTRICTED) {
            throw new BadRequestException("Hành trình này đã tắt tính năng bình luận.");
        }
        
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
    public Page<CommentResponse> getComments(UUID checkinId, Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Set<Long> excludedUserIds = getExcludedUserIds(currentUserId);
        
        return commentRepository.findByCheckinId(checkinId, excludedUserIds, pageable)
                .map(checkinMapper::toCommentResponse);
    }

    @Override
    @Transactional
    public CheckinResponse updateCheckin(UUID checkinId, String caption, User currentUser) {
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
    public void deleteCheckin(UUID checkinId, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        if (!checkin.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền xóa bài viết này");
        }

        Journey journey = checkin.getJourney();
        
        if (checkin.getStatus() == CheckinStatus.REST && journey.isRequiresFreezeTicket()) {
            User user = userRepository.findById(currentUser.getId()).orElse(currentUser);
            user.setFreezeStreakCount(user.getFreezeStreakCount() + 1); 
            userRepository.save(user);
        }

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
            participant.setLastCheckinAt(null);
            participantRepository.save(participant);
            return;
        }

        LocalDateTime lastCheckinTime = history.get(0);
        LocalDate lastCheckinDate = lastCheckinTime.atZone(ZoneId.of("UTC"))
                                                   .withZoneSameInstant(userZone)
                                                   .toLocalDate();
        participant.setLastCheckinAt(lastCheckinDate);

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