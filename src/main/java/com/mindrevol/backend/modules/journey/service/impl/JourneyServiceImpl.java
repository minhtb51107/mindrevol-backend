package com.mindrevol.backend.modules.journey.service.impl;

import com.mindrevol.backend.common.constant.AppConstants;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.modules.journey.dto.request.*;
import com.mindrevol.backend.modules.journey.dto.response.*;
import com.mindrevol.backend.modules.journey.entity.*;
import com.mindrevol.backend.modules.journey.event.JourneyCreatedEvent;
import com.mindrevol.backend.modules.journey.event.JourneyJoinedEvent;
import com.mindrevol.backend.modules.journey.mapper.JourneyMapper;
import com.mindrevol.backend.modules.journey.repository.*;
import com.mindrevol.backend.modules.journey.service.JourneyService;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository; // Import UserRepository

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JourneyServiceImpl implements JourneyService {

    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final JourneyTaskRepository journeyTaskRepository;
    private final JourneyRequestRepository journeyRequestRepository;
    private final UserRepository userRepository; // [MỚI] Inject UserRepository
    
    private final GamificationService gamificationService;
    private final JourneyMapper journeyMapper;
    private final CheckinRepository checkinRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    // Helper method để tìm User (Code dùng chung)
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public JourneyResponse createJourney(CreateJourneyRequest request, Long userId) {
        User currentUser = getUser(userId); // Lấy User từ DB

        // Check giới hạn
        long activeJourneys = journeyRepository.countByCreatorIdAndStatus(userId, JourneyStatus.ACTIVE);
        if (activeJourneys >= AppConstants.LIMIT_OWNED_JOURNEYS_FREE) {
            throw new BadRequestException("Bạn chỉ được tạo tối đa " + AppConstants.LIMIT_OWNED_JOURNEYS_FREE + " hành trình cùng lúc.");
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        String inviteCode = generateUniqueInviteCode();
        boolean hasStreak = true, reqTicket = true, isHardcore = true;
        InteractionType interactionType = InteractionType.PRIVATE_REPLY;

        if (request.getType() == JourneyType.MEMORIES || request.getType() == JourneyType.PROJECT) {
            hasStreak = false; reqTicket = false; isHardcore = false; interactionType = InteractionType.GROUP_DISCUSS;
        } else if (request.getType() == JourneyType.CHALLENGE) {
            reqTicket = false; isHardcore = false; interactionType = InteractionType.RESTRICTED;
        }

        Journey journey = Journey.builder()
                .name(request.getName()).description(request.getDescription()).type(request.getType())
                .startDate(request.getStartDate()).endDate(request.getEndDate())
                .theme(request.getTheme() != null ? request.getTheme() : "DEFAULT")
                .inviteCode(inviteCode).status(JourneyStatus.ACTIVE).creator(currentUser)
                .hasStreak(hasStreak).requiresFreezeTicket(reqTicket).isHardcore(isHardcore)
                .interactionType(interactionType).build();

        Journey savedJourney = journeyRepository.save(journey);

        if (request.getType() == JourneyType.ROADMAP && request.getRoadmapTasks() != null) {
             List<JourneyTask> tasks = request.getRoadmapTasks().stream()
                 .map(t -> JourneyTask.builder().journey(savedJourney).title(t.getTitle())
                     .description(t.getDescription()).dayNo(t.getDayNo()).build())
                 .collect(Collectors.toList());
             journeyTaskRepository.saveAll(tasks);
        }
        
        participantRepository.save(JourneyParticipant.builder()
                .journey(savedJourney).user(currentUser).role(JourneyRole.OWNER).currentStreak(0).build());
        
        return journeyMapper.toResponse(savedJourney);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoadmapStatusResponse> getJourneyRoadmap(UUID journeyId, Long currentUserId) {
        // Method này không cần User entity, chỉ cần ID để query checkin là đủ
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        if (journey.getType() != JourneyType.ROADMAP) {
            throw new BadRequestException("Hành trình này không có lộ trình (Roadmap)");
        }

        List<JourneyTask> tasks = journey.getRoadmap();
        Set<UUID> completedTaskIds = checkinRepository.findCompletedTaskIdsByUserAndJourney(currentUserId, journeyId);

        return tasks.stream().map(task -> {
            RoadmapStatusResponse res = journeyMapper.toRoadmapResponse(task);
            res.setCompleted(completedTaskIds.contains(task.getId()));
            return res;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JourneyResponse joinJourney(JoinJourneyRequest request, Long userId) {
        User currentUser = getUser(userId);
        Journey journey = journeyRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hành trình với mã này"));

        if (participantRepository.existsByJourneyIdAndUserId(journey.getId(), userId)) {
            throw new BadRequestException("Bạn đã tham gia hành trình này rồi");
        }

        long currentMembers = participantRepository.countByJourneyId(journey.getId());
        if (currentMembers >= AppConstants.LIMIT_MEMBERS_PER_JOURNEY_FREE) {
             throw new BadRequestException("Hành trình này đã đầy thành viên.");
        }

        if (journey.isRequireApproval()) {
            Optional<JourneyRequest> existingReq = journeyRequestRepository.findByJourneyIdAndUserId(journey.getId(), userId);
            if (existingReq.isPresent() && existingReq.get().getStatus() == RequestStatus.PENDING) {
                throw new BadRequestException("Yêu cầu tham gia của bạn đang chờ duyệt.");
            }
            journeyRequestRepository.save(JourneyRequest.builder().journey(journey).user(currentUser).status(RequestStatus.PENDING).build());
            return journeyMapper.toResponse(journey); 
        }

        participantRepository.save(JourneyParticipant.builder().journey(journey).user(currentUser).role(JourneyRole.MEMBER).currentStreak(0).build());
        eventPublisher.publishEvent(new JourneyJoinedEvent(journey, currentUser));
        return journeyMapper.toResponse(journey);
    }

    @Override
    @Transactional
    public void approveJoinRequest(UUID requestId, Long adminId) {
        JourneyRequest req = journeyRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại"));
        
        JourneyParticipant adminPart = participantRepository.findByJourneyIdAndUserId(req.getJourney().getId(), adminId)
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên nhóm này"));
        if (adminPart.getRole() != JourneyRole.ADMIN && adminPart.getRole() != JourneyRole.OWNER) {
            throw new BadRequestException("Chỉ quản trị viên mới được duyệt thành viên");
        }

        long currentMembers = participantRepository.countByJourneyId(req.getJourney().getId());
        if (currentMembers >= AppConstants.LIMIT_MEMBERS_PER_JOURNEY_FREE) {
             throw new BadRequestException("Nhóm đã đầy! Không thể duyệt thêm thành viên.");
        }

        if (req.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Yêu cầu này đã được xử lý");
        }

        participantRepository.save(JourneyParticipant.builder().journey(req.getJourney()).user(req.getUser()).role(JourneyRole.MEMBER).currentStreak(0).build());
        req.setStatus(RequestStatus.APPROVED);
        journeyRequestRepository.save(req);
        eventPublisher.publishEvent(new JourneyJoinedEvent(req.getJourney(), req.getUser()));
    }

    @Override
    @Transactional
    public void rejectJoinRequest(UUID requestId, Long adminId) {
        JourneyRequest req = journeyRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại"));

        JourneyParticipant adminPart = participantRepository.findByJourneyIdAndUserId(req.getJourney().getId(), adminId)
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên nhóm này"));
        if (adminPart.getRole() != JourneyRole.ADMIN && adminPart.getRole() != JourneyRole.OWNER) {
            throw new BadRequestException("Chỉ quản trị viên mới được duyệt thành viên");
        }

        req.setStatus(RequestStatus.REJECTED);
        journeyRequestRepository.save(req);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JourneyResponse> getMyJourneys(Long userId) {
        List<JourneyParticipant> participants = participantRepository.findAllByUserId(userId);
        return participants.stream()
                .map(p -> {
                    if (p.getJourney().isHasStreak()) {
                        gamificationService.refreshUserStreak(p.getJourney().getId(), userId);
                    }
                    return journeyMapper.toResponse(p.getJourney());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void leaveJourney(UUID journeyId, Long userId) {
        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journeyId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không ở trong hành trình này"));
        if (participant.getJourney().getCreator().getId().equals(userId)) {
            throw new BadRequestException("Bạn là người tạo nhóm, không thể tự rời. Hãy xóa nhóm nếu muốn kết thúc.");
        }
        participantRepository.delete(participant);
    }

    @Override
    @Transactional
    public JourneyResponse updateJourneySettings(UUID journeyId, UpdateJourneySettingsRequest request, Long userId) {
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));
        
        JourneyParticipant adminPart = participantRepository.findByJourneyIdAndUserId(journeyId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên nhóm này"));

        if (adminPart.getRole() != JourneyRole.ADMIN && adminPart.getRole() != JourneyRole.OWNER) {
            throw new BadRequestException("Chỉ Quản trị viên mới được thay đổi cài đặt");
        }

        // --- Cập nhật các trường ---
        if (request.getName() != null) journey.setName(request.getName());
        if (request.getDescription() != null) journey.setDescription(request.getDescription());
        if (request.getTheme() != null) journey.setTheme(request.getTheme());
        
        // Settings flags
        if (request.getHasStreak() != null) journey.setHasStreak(request.getHasStreak());
        if (request.getRequiresFreezeTicket() != null) journey.setRequiresFreezeTicket(request.getRequiresFreezeTicket());
        if (request.getIsHardcore() != null) journey.setHardcore(request.getIsHardcore());
        
        // [MỚI] Cập nhật Require Approval
        if (request.getRequireApproval() != null) {
            journey.setRequireApproval(request.getRequireApproval());
        }

        return journeyMapper.toResponse(journeyRepository.save(journey));
    }

    @Override
    @Transactional
    public void kickMember(UUID journeyId, Long memberId, Long requesterId) {
        JourneyParticipant requester = participantRepository.findByJourneyIdAndUserId(journeyId, requesterId)
                .orElseThrow(() -> new BadRequestException("Bạn không ở trong hành trình này"));

        if (requester.getRole() != JourneyRole.ADMIN && requester.getRole() != JourneyRole.OWNER) {
            throw new BadRequestException("Bạn không có quyền mời thành viên ra khỏi nhóm");
        }
        if (requesterId.equals(memberId)) {
            throw new BadRequestException("Bạn không thể tự kick chính mình.");
        }

        JourneyParticipant victim = participantRepository.findByJourneyIdAndUserId(journeyId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong nhóm"));

        boolean isRequesterCreator = requester.getJourney().getCreator().getId().equals(requesterId);
        boolean isVictimAdmin = victim.getRole() == JourneyRole.ADMIN;

        if (isVictimAdmin && !isRequesterCreator) {
            throw new BadRequestException("Bạn không thể kick một Quản trị viên khác");
        }
        participantRepository.delete(victim);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "journey_widget", key = "#journeyId + '-' + #userId")
    public JourneyWidgetResponse getWidgetInfo(UUID journeyId, Long userId) {
        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journeyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn không tham gia hành trình này"));

        Optional<Checkin> lastCheckinOpt = checkinRepository.findTopByJourneyIdAndUserIdOrderByCreatedAtDesc(journeyId, userId);

        boolean isCompletedToday = false;
        String thumbnailUrl = null;
        WidgetStatus widgetStatus;
        String label;

        String tz = participant.getUser().getTimezone() != null ? participant.getUser().getTimezone() : "UTC";
        ZoneId userZone = ZoneId.of(tz);
        LocalDate todayLocal = LocalDate.now(userZone);

        if (lastCheckinOpt.isPresent()) {
            Checkin lastCheckin = lastCheckinOpt.get();
            thumbnailUrl = lastCheckin.getThumbnailUrl();
            LocalDate checkinDateLocal = lastCheckin.getCreatedAt().atZone(ZoneId.of("UTC")).withZoneSameInstant(userZone).toLocalDate();

            if (checkinDateLocal.isEqual(todayLocal)) {
                isCompletedToday = true;
                if (lastCheckin.getStatus() == CheckinStatus.REST) {
                    widgetStatus = WidgetStatus.REST; label = "Đang nghỉ phép ❄️";
                } else if (lastCheckin.getStatus() == CheckinStatus.COMEBACK) {
                    widgetStatus = WidgetStatus.COMEBACK_COMPLETED; label = "Đã trở lại! 🔥";
                } else if (lastCheckin.getStatus() == CheckinStatus.FAILED) {
                    widgetStatus = WidgetStatus.FAILED_STREAK; label = "Thất bại 😢";
                } else {
                    widgetStatus = WidgetStatus.COMPLETED; label = "Tuyệt vời! ✅";
                }
            } else {
                if (participant.getJourney().isHasStreak()) {
                    if (checkinDateLocal.isBefore(todayLocal.minusDays(1))) {
                         widgetStatus = WidgetStatus.FAILED_STREAK; label = "Bạn đã mất chuỗi 😭";
                    } else {
                        widgetStatus = WidgetStatus.PENDING; label = "Sẵn sàng chưa? 📸";
                    }
                } else {
                    widgetStatus = WidgetStatus.PENDING; label = "Chia sẻ khoảnh khắc nào! 📸";
                }
            }
        } else {
             widgetStatus = WidgetStatus.PENDING; label = "Bắt đầu ngay nào! 🚀";
        }

        return JourneyWidgetResponse.builder()
                .journeyName(participant.getJourney().getName())
                .currentStreak(participant.getCurrentStreak())
                .isCompletedToday(isCompletedToday)
                .latestThumbnailUrl(thumbnailUrl)
                .status(widgetStatus)
                .statusLabel(label)
                .ownerName(participant.getUser().getFullname())
                .ownerAvatar(participant.getUser().getAvatarUrl())
                .build();
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (journeyRepository.existsByInviteCode(code));
        return code;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JourneyResponse> getDiscoveryTemplates() {
        return journeyRepository.findAllTemplates().stream()
                .map(journeyMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JourneyResponse forkJourney(UUID templateId, Long userId) {
        User currentUser = getUser(userId);
        long activeJourneys = journeyRepository.countByCreatorIdAndStatus(userId, JourneyStatus.ACTIVE);
        if (activeJourneys >= AppConstants.LIMIT_OWNED_JOURNEYS_FREE) {
            throw new BadRequestException("Bạn đã đạt giới hạn số lượng hành trình. Không thể sao chép thêm.");
        }

        Journey original = journeyRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình mẫu không tồn tại"));

        if (!original.isTemplate()) {
            throw new BadRequestException("Hành trình này không được phép sao chép.");
        }

        Journey clone = original.copyForUser(currentUser);
        clone.setInviteCode(generateUniqueInviteCode());
        clone.setCreatedAt(LocalDateTime.now());
        clone.setStartDate(LocalDate.now()); 
        
        if (original.getStartDate() != null && original.getEndDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(original.getStartDate(), original.getEndDate());
            clone.setEndDate(LocalDate.now().plusDays(days));
        }

        Journey savedClone = journeyRepository.save(clone);

        if (original.getType() == JourneyType.ROADMAP && !original.getRoadmap().isEmpty()) {
            List<JourneyTask> clonedTasks = original.getRoadmap().stream()
                    .map(task -> JourneyTask.builder().journey(savedClone).dayNo(task.getDayNo())
                            .title(task.getTitle()).description(task.getDescription()).build())
                    .collect(Collectors.toList());
            journeyTaskRepository.saveAll(clonedTasks);
            savedClone.setRoadmap(clonedTasks);
        }

        participantRepository.save(JourneyParticipant.builder().journey(savedClone).user(currentUser).role(JourneyRole.OWNER).currentStreak(0).build());
        eventPublisher.publishEvent(new JourneyCreatedEvent(savedClone, currentUser));
        return journeyMapper.toResponse(savedClone);
    }

    @Override
    @Transactional
    public void nudgeMember(UUID journeyId, Long memberId, Long requesterId) {
        User requester = getUser(requesterId);
        
        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, requesterId)) {
            throw new BadRequestException("Bạn không phải thành viên của hành trình này.");
        }
        
        JourneyParticipant target = participantRepository.findByJourneyIdAndUserId(journeyId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong nhóm."));

        if (target.getUser().getId().equals(requesterId)) {
             throw new BadRequestException("Bạn không thể tự nhắc nhở chính mình.");
        }

        String tz = target.getUser().getTimezone() != null ? target.getUser().getTimezone() : "UTC";
        LocalDate todayTarget = LocalDate.now(ZoneId.of(tz));
        
        if (target.getLastCheckinAt() != null && target.getLastCheckinAt().isEqual(todayTarget)) {
            throw new BadRequestException("Người này đã check-in hôm nay rồi!");
        }

        String redisKey = "nudge:" + journeyId + ":" + requesterId + ":" + memberId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            throw new BadRequestException("Bạn đã nhắc nhở người này hôm nay rồi. Đừng spam nhé!");
        }
        
        redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);

        notificationService.sendAndSaveNotification(
                memberId, requesterId, NotificationType.NUDGE,
                "Nhắc nhở nhẹ! 👋",
                requester.getFullname() + " đang chờ bạn check-in trong hành trình " + target.getJourney().getName(),
                journeyId.toString(), requester.getAvatarUrl()
        );
        
        log.info("User {} nudged User {} in Journey {}", requesterId, memberId, journeyId);
    }
    
    @Override
    @Transactional
    public void transferOwnership(UUID journeyId, Long currentOwnerId, Long newOwnerId) {
        if (currentOwnerId.equals(newOwnerId)) {
            throw new BadRequestException("Bạn đang là chủ phòng rồi.");
        }

        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        if (!journey.getCreator().getId().equals(currentOwnerId)) {
            throw new BadRequestException("Bạn không phải là chủ phòng của hành trình này.");
        }

        // Tìm chủ cũ (Current Owner)
        JourneyParticipant ownerPart = participantRepository.findByJourneyIdAndUserId(journeyId, currentOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Lỗi dữ liệu chủ phòng"));

        // Tìm chủ mới (Target Member)
        JourneyParticipant newOwnerPart = participantRepository.findByJourneyIdAndUserId(journeyId, newOwnerId)
                .orElseThrow(() -> new BadRequestException("Thành viên được chỉ định không có trong nhóm."));

        // Chuyển quyền
        ownerPart.setRole(JourneyRole.ADMIN); // Hoặc MEMBER tùy bạn
        newOwnerPart.setRole(JourneyRole.OWNER);
        
        // Cập nhật người tạo ở bảng Journey (quan trọng để query countByCreator hoạt động đúng)
        journey.setCreator(newOwnerPart.getUser()); 

        participantRepository.save(ownerPart);
        participantRepository.save(newOwnerPart);
        journeyRepository.save(journey);
        
        log.info("Transferred ownership of journey {} from {} to {}", journeyId, currentOwnerId, newOwnerId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<JourneyParticipantResponse> getJourneyParticipants(UUID journeyId) {
        return participantRepository.findAllByJourneyId(journeyId).stream()
                .map(p -> JourneyParticipantResponse.builder()
                        .userId(p.getUser().getId())
                        .fullname(p.getUser().getFullname())
                        .handle(p.getUser().getHandle())
                        .avatarUrl(p.getUser().getAvatarUrl())
                        .role(p.getRole())
                        .build())
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void deleteJourney(UUID journeyId, Long userId) {
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        if (!journey.getCreator().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xóa hành trình này.");
        }

        // Xóa hành trình (Cascade sẽ xóa hết participants, tasks, checkins liên quan)
        journeyRepository.delete(journey);
    }
}