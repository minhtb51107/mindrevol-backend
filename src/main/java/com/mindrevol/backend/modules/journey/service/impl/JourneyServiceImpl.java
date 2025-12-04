package com.mindrevol.backend.modules.journey.service.impl;

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
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRequestRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyTaskRepository;
import com.mindrevol.backend.modules.journey.service.JourneyService;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import com.mindrevol.backend.modules.user.entity.User;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    
    private final GamificationService gamificationService;
    private final JourneyMapper journeyMapper;
    private final CheckinRepository checkinRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    // --- INJECT THÊM ---
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;
    // -------------------

    @Override
    @Transactional
    public JourneyResponse createJourney(CreateJourneyRequest request, User currentUser) {
        // 1. Validate đầu vào
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // 2. Sinh mã mời
        String inviteCode = generateUniqueInviteCode();

        // =========================================================================
        // 3. LOGIC CỐT LÕI (ĐÃ SỬA): Mapping "Mục tiêu" -> "Luật chơi"
        // =========================================================================
        
        // Mặc định cho HABIT / ROADMAP (Kỷ luật cao, Tập trung cá nhân)
        boolean hasStreak = true;
        boolean reqTicket = true;
        boolean isHardcore = true;
        InteractionType interactionType = InteractionType.PRIVATE_REPLY; // <--- CỐT LÕI: Kiểu Locket

        // Ghi đè cấu hình nếu là loại khác
        if (request.getType() == JourneyType.MEMORIES) {
            hasStreak = false;
            reqTicket = false;
            isHardcore = false;
            interactionType = InteractionType.GROUP_DISCUSS; // <--- CỐT LÕI: Kiểu Facebook Group
        } 
        else if (request.getType() == JourneyType.PROJECT) {
            hasStreak = false; 
            reqTicket = false;
            isHardcore = false;
            interactionType = InteractionType.GROUP_DISCUSS; // Cần thảo luận
        }
        else if (request.getType() == JourneyType.CHALLENGE) {
            hasStreak = true;
            reqTicket = false;
            isHardcore = false;
            interactionType = InteractionType.RESTRICTED; // <--- CỐT LÕI: Kiểu Channel thông báo
        }

        // 4. Build Journey (Lưu xuống DB với các cờ đã tính toán ở trên)
        Journey journey = Journey.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .theme(request.getTheme() != null ? request.getTheme() : "DEFAULT")
                .inviteCode(inviteCode)
                .status(JourneyStatus.ACTIVE)
                .creator(currentUser)
                // --- CÁC TRƯỜNG TỰ ĐỘNG ---
                .hasStreak(hasStreak)
                .requiresFreezeTicket(reqTicket)
                .isHardcore(isHardcore)
                .interactionType(interactionType) // Đã map đúng triết lý
                // ---------------------------
                .build();

        Journey savedJourney = journeyRepository.save(journey);

        // 5. Xử lý Roadmap (Nếu có danh sách task)
        if (request.getType() == JourneyType.ROADMAP && request.getRoadmapTasks() != null) {
             List<JourneyTask> tasks = request.getRoadmapTasks().stream()
                 .map(t -> JourneyTask.builder()
                     .journey(savedJourney)
                     .title(t.getTitle())
                     .description(t.getDescription())
                     .dayNo(t.getDayNo()) 
                     .build())
                 .collect(Collectors.toList());
             journeyTaskRepository.saveAll(tasks);
        }
        
        // 6. Thêm người tạo vào làm thành viên (Dùng đúng JourneyParticipant)
        JourneyParticipant creatorParticipant = JourneyParticipant.builder()
                .journey(savedJourney)
                .user(currentUser)
                //.joinedAt(LocalDateTime.now()) // Nếu entity của bạn có trường này
                .role(JourneyRole.OWNER)       // <--- Đảm bảo Enum JourneyRole đã có OWNER
                .currentStreak(0)
                // .status(ParticipantStatus.ACTIVE) // Nếu entity có trường status
                .build();

        participantRepository.save(creatorParticipant);
        
        // 7. Trả về Response
        return journeyMapper.toResponse(savedJourney);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoadmapStatusResponse> getJourneyRoadmap(UUID journeyId, Long currentUserId) {
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
    public JourneyResponse joinJourney(JoinJourneyRequest request, User currentUser) {
        Journey journey = journeyRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hành trình với mã này"));

        if (participantRepository.existsByJourneyIdAndUserId(journey.getId(), currentUser.getId())) {
            throw new BadRequestException("Bạn đã tham gia hành trình này rồi");
        }

        if (journey.isRequireApproval()) {
            Optional<JourneyRequest> existingReq = journeyRequestRepository.findByJourneyIdAndUserId(journey.getId(), currentUser.getId());
            if (existingReq.isPresent()) {
                if (existingReq.get().getStatus() == RequestStatus.PENDING) {
                    throw new BadRequestException("Yêu cầu tham gia của bạn đang chờ duyệt.");
                } else if (existingReq.get().getStatus() == RequestStatus.REJECTED) {
                    throw new BadRequestException("Yêu cầu tham gia của bạn đã bị từ chối.");
                }
            }

            JourneyRequest newReq = JourneyRequest.builder()
                    .journey(journey)
                    .user(currentUser)
                    .status(RequestStatus.PENDING)
                    .build();
            journeyRequestRepository.save(newReq);

            return journeyMapper.toResponse(journey); 
        }

        JourneyParticipant participant = JourneyParticipant.builder()
                .journey(journey)
                .user(currentUser)
                .role(JourneyRole.MEMBER)
                .currentStreak(0)
                .build();
        participantRepository.save(participant);

        eventPublisher.publishEvent(new JourneyJoinedEvent(journey, currentUser));

        return journeyMapper.toResponse(journey);
    }

    @Transactional
    public void approveJoinRequest(UUID requestId, User admin) {
        JourneyRequest req = journeyRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại"));
        
        JourneyParticipant adminPart = participantRepository.findByJourneyIdAndUserId(req.getJourney().getId(), admin.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên nhóm này"));
        if (adminPart.getRole() != JourneyRole.ADMIN) {
            throw new BadRequestException("Chỉ quản trị viên mới được duyệt thành viên");
        }

        if (req.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Yêu cầu này đã được xử lý");
        }

        JourneyParticipant participant = JourneyParticipant.builder()
                .journey(req.getJourney())
                .user(req.getUser())
                .role(JourneyRole.MEMBER)
                .currentStreak(0)
                .build();
        participantRepository.save(participant);

        req.setStatus(RequestStatus.APPROVED);
        journeyRequestRepository.save(req);

        eventPublisher.publishEvent(new JourneyJoinedEvent(req.getJourney(), req.getUser()));
    }

    @Transactional
    public void rejectJoinRequest(UUID requestId, User admin) {
        JourneyRequest req = journeyRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại"));

        JourneyParticipant adminPart = participantRepository.findByJourneyIdAndUserId(req.getJourney().getId(), admin.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên nhóm này"));
        if (adminPart.getRole() != JourneyRole.ADMIN) {
            throw new BadRequestException("Chỉ quản trị viên mới được duyệt thành viên");
        }

        req.setStatus(RequestStatus.REJECTED);
        journeyRequestRepository.save(req);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JourneyResponse> getMyJourneys(User currentUser) {
        List<JourneyParticipant> participants = participantRepository.findAllByUserId(currentUser.getId());
        return participants.stream()
                .map(p -> {
                    if (p.getJourney().isHasStreak()) {
                        gamificationService.refreshUserStreak(p.getJourney().getId(), currentUser.getId());
                    }
                    return journeyMapper.toResponse(p.getJourney());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void leaveJourney(UUID journeyId, User currentUser) {
        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journeyId, currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không ở trong hành trình này"));
        if (participant.getJourney().getCreator().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Bạn là người tạo nhóm, không thể tự rời. Hãy xóa nhóm nếu muốn kết thúc.");
        }
        participantRepository.delete(participant);
    }

    @Override
    @Transactional
    public JourneyResponse updateJourneySettings(UUID journeyId, UpdateJourneySettingsRequest request, User currentUser) {
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));
        JourneyParticipant adminPart = participantRepository.findByJourneyIdAndUserId(journeyId, currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên nhóm này"));

        if (adminPart.getRole() != JourneyRole.ADMIN) {
            throw new BadRequestException("Chỉ Quản trị viên mới được thay đổi cài đặt");
        }

        if (request.getName() != null) journey.setName(request.getName());
        if (request.getDescription() != null) journey.setDescription(request.getDescription());
        if (request.getTheme() != null) journey.setTheme(request.getTheme());
        if (request.getHasStreak() != null) journey.setHasStreak(request.getHasStreak());
        if (request.getRequiresFreezeTicket() != null) journey.setRequiresFreezeTicket(request.getRequiresFreezeTicket());
        if (request.getIsHardcore() != null) journey.setHardcore(request.getIsHardcore());

        Journey updatedJourney = journeyRepository.save(journey);
        return journeyMapper.toResponse(updatedJourney);
    }

    @Override
    @Transactional
    public void kickMember(UUID journeyId, Long memberId, User currentUser) {
        JourneyParticipant requester = participantRepository.findByJourneyIdAndUserId(journeyId, currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không ở trong hành trình này"));

        if (requester.getRole() != JourneyRole.ADMIN) {
            throw new BadRequestException("Bạn không có quyền mời thành viên ra khỏi nhóm");
        }
        if (currentUser.getId().equals(memberId)) {
            throw new BadRequestException("Bạn không thể tự kick chính mình. Hãy dùng chức năng Rời nhóm.");
        }

        JourneyParticipant victim = participantRepository.findByJourneyIdAndUserId(journeyId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong nhóm"));

        boolean isRequesterCreator = requester.getJourney().getCreator().getId().equals(currentUser.getId());
        boolean isVictimAdmin = victim.getRole() == JourneyRole.ADMIN;

        if (isVictimAdmin && !isRequesterCreator) {
            throw new BadRequestException("Bạn không thể kick một Quản trị viên khác (Chỉ người tạo nhóm mới có quyền này)");
        }
        participantRepository.delete(victim);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "journey_widget", key = "#journeyId + '-' + #userId")
    public JourneyWidgetResponse getWidgetInfo(UUID journeyId, Long userId) {
        log.info("Fetching Widget Info from Database for User {} Journey {}", userId, journeyId);

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
                    widgetStatus = WidgetStatus.REST;
                    label = "Đang nghỉ phép ❄️";
                } else if (lastCheckin.getStatus() == CheckinStatus.COMEBACK) {
                    widgetStatus = WidgetStatus.COMEBACK_COMPLETED;
                    label = "Đã trở lại! 🔥";
                } else if (lastCheckin.getStatus() == CheckinStatus.FAILED) {
                    widgetStatus = WidgetStatus.FAILED_STREAK;
                    label = "Thất bại 😢";
                } else {
                    widgetStatus = WidgetStatus.COMPLETED;
                    label = "Tuyệt vời! ✅";
                }
            } else {
                if (participant.getJourney().isHasStreak()) {
                    if (checkinDateLocal.isBefore(todayLocal.minusDays(1))) {
                         widgetStatus = WidgetStatus.FAILED_STREAK;
                         label = "Bạn đã mất chuỗi 😭";
                    } else {
                        widgetStatus = WidgetStatus.PENDING;
                        label = "Sẵn sàng chưa? 📸";
                    }
                } else {
                    widgetStatus = WidgetStatus.PENDING;
                    label = "Chia sẻ khoảnh khắc nào! 📸";
                }
            }
        } else {
             widgetStatus = WidgetStatus.PENDING;
             label = "Bắt đầu ngay nào! 🚀";
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
    public JourneyResponse forkJourney(UUID templateId, User currentUser) {
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
                    .map(task -> JourneyTask.builder()
                            .journey(savedClone)
                            .dayNo(task.getDayNo())
                            .title(task.getTitle())
                            .description(task.getDescription())
                            .build())
                    .collect(Collectors.toList());
            
            journeyTaskRepository.saveAll(clonedTasks);
            savedClone.setRoadmap(clonedTasks);
        }

        JourneyParticipant participant = JourneyParticipant.builder()
                .journey(savedClone)
                .user(currentUser)
                .role(JourneyRole.ADMIN)
                .currentStreak(0)
                .build();
        participantRepository.save(participant);

        eventPublisher.publishEvent(new JourneyCreatedEvent(savedClone, currentUser));

        return journeyMapper.toResponse(savedClone);
    }

    // --- MỚI: NUDGE (CHỌC GHẸO) ---
    @Override
    @Transactional
    public void nudgeMember(UUID journeyId, Long memberId, User currentUser) {
        // 1. Kiểm tra quyền hạn
        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, currentUser.getId())) {
            throw new BadRequestException("Bạn không phải thành viên của hành trình này.");
        }
        
        JourneyParticipant target = participantRepository.findByJourneyIdAndUserId(journeyId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong nhóm."));

        if (target.getUser().getId().equals(currentUser.getId())) {
             throw new BadRequestException("Bạn không thể tự nhắc nhở chính mình (hãy đặt báo thức đi!).");
        }

        // Kiểm tra xem họ đã check-in hôm nay chưa
        // (Sử dụng Timezone của họ để check)
        String tz = target.getUser().getTimezone() != null ? target.getUser().getTimezone() : "UTC";
        LocalDate todayTarget = LocalDate.now(ZoneId.of(tz));
        
        if (target.getLastCheckinAt() != null && target.getLastCheckinAt().isEqual(todayTarget)) {
            throw new BadRequestException("Người này đã check-in hôm nay rồi!");
        }

        // 2. Rate Limit (Chống Spam): 1 lần/ngày/cặp user
        String redisKey = "nudge:" + journeyId + ":" + currentUser.getId() + ":" + memberId;
        
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            throw new BadRequestException("Bạn đã nhắc nhở người này hôm nay rồi. Đừng spam nhé!");
        }
        
        // Lưu cache đánh dấu đã nudge, hết hạn sau 24h
        redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);

        // 3. Gửi Thông Báo
        notificationService.sendAndSaveNotification(
                memberId,
                currentUser.getId(),
                NotificationType.NUDGE,
                "Nhắc nhở nhẹ! 👋",
                currentUser.getFullname() + " đang chờ bạn check-in trong hành trình " + target.getJourney().getName(),
                journeyId.toString(),
                currentUser.getAvatarUrl()
        );
        
        log.info("User {} nudged User {} in Journey {}", currentUser.getId(), memberId, journeyId);
    }
}