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
import com.mindrevol.backend.modules.journey.service.JourneyService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.journey.repository.JourneyRequestRepository;
import com.mindrevol.backend.modules.journey.entity.JourneyRequest;
import com.mindrevol.backend.modules.journey.entity.RequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JourneyServiceImpl implements JourneyService {

    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final GamificationService gamificationService;
    private final JourneyMapper journeyMapper;
    private final CheckinRepository checkinRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final JourneyRequestRepository journeyRequestRepository;

    @Override
    @Transactional
    public JourneyResponse createJourney(CreateJourneyRequest request, User currentUser) {
        String inviteCode = generateUniqueInviteCode();

        boolean hasStreak = true;
        boolean reqTicket = true;
        boolean isHardcore = true;

        if (request.getType() == JourneyType.MEMORIES || request.getType() == JourneyType.PROJECT) {
            hasStreak = false;
            reqTicket = false;
            isHardcore = false;
        }

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
                .hasStreak(hasStreak)
                .requiresFreezeTicket(reqTicket)
                .isHardcore(isHardcore)
                .build();

        if (request.getType() == JourneyType.ROADMAP) {
            if (request.getRoadmapTasks() == null || request.getRoadmapTasks().isEmpty()) {
                throw new BadRequestException("Hành trình lộ trình cần có ít nhất 1 nhiệm vụ!");
            }
            List<JourneyTask> tasks = request.getRoadmapTasks().stream()
                    .map(t -> JourneyTask.builder()
                            .journey(journey)
                            .dayNo(t.getDayNo())
                            .title(t.getTitle())
                            .description(t.getDescription())
                            .build())
                    .collect(Collectors.toList());
            journey.setRoadmap(tasks);
        }

        Journey savedJourney = journeyRepository.save(journey);

        // Bắn Event
        eventPublisher.publishEvent(new JourneyCreatedEvent(savedJourney, currentUser));

        JourneyParticipant participant = JourneyParticipant.builder()
                .journey(savedJourney)
                .user(currentUser)
                .role(JourneyRole.ADMIN)
                .currentStreak(0)
                .build();

        participantRepository.save(participant);

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

        // --- LOGIC MỚI: Check Approval ---
        if (journey.isRequireApproval()) {
            // Kiểm tra xem đã gửi request chưa
            Optional<JourneyRequest> existingReq = journeyRequestRepository.findByJourneyIdAndUserId(journey.getId(), currentUser.getId());
            if (existingReq.isPresent()) {
                if (existingReq.get().getStatus() == RequestStatus.PENDING) {
                    throw new BadRequestException("Yêu cầu tham gia của bạn đang chờ duyệt.");
                } else if (existingReq.get().getStatus() == RequestStatus.REJECTED) {
                    throw new BadRequestException("Yêu cầu tham gia của bạn đã bị từ chối.");
                }
            }

            // Tạo Request mới
            JourneyRequest newReq = JourneyRequest.builder()
                    .journey(journey)
                    .user(currentUser)
                    .status(RequestStatus.PENDING)
                    .build();
            journeyRequestRepository.save(newReq);

            // Trả về response nhưng có flag đặc biệt để Frontend biết là đang Pending
            // Tạm thời ta vẫn trả về JourneyResponse, nhưng isJoined = false
            return journeyMapper.toResponse(journey); 
        }
        // --------------------------------

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

    // --- THÊM HÀM MỚI: DUYỆT THÀNH VIÊN ---
    @Transactional
    public void approveJoinRequest(UUID requestId, User admin) {
        JourneyRequest req = journeyRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại"));
        
        // Check quyền Admin
        JourneyParticipant adminPart = participantRepository.findByJourneyIdAndUserId(req.getJourney().getId(), admin.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên nhóm này"));
        if (adminPart.getRole() != JourneyRole.ADMIN) {
            throw new BadRequestException("Chỉ quản trị viên mới được duyệt thành viên");
        }

        if (req.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Yêu cầu này đã được xử lý");
        }

        // Chấp nhận: Thêm vào nhóm
        JourneyParticipant participant = JourneyParticipant.builder()
                .journey(req.getJourney())
                .user(req.getUser())
                .role(JourneyRole.MEMBER)
                .currentStreak(0)
                .build();
        participantRepository.save(participant);

        // Update Request status
        req.setStatus(RequestStatus.APPROVED);
        journeyRequestRepository.save(req);

        // Bắn Event Join (để tạo Habit...)
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
                    // Lazy reset streak nếu cần thiết (phòng trường hợp Job chưa chạy)
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

        // Sử dụng Timezone của User nếu có để tính Widget chính xác
        String tz = participant.getUser().getTimezone() != null ? participant.getUser().getTimezone() : "UTC";
        ZoneId userZone = ZoneId.of(tz);
        LocalDate todayLocal = LocalDate.now(userZone);

        if (lastCheckinOpt.isPresent()) {
            Checkin lastCheckin = lastCheckinOpt.get();
            thumbnailUrl = lastCheckin.getThumbnailUrl();
            
            // Check ngày theo Timezone User
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
                // Hôm nay chưa làm
                if (participant.getJourney().isHasStreak()) {
                    // Logic check streak gãy
                    // Nếu lastCheckin < today - 1 (tức là cách đây 2 ngày trở lên) => Gãy
                    if (checkinDateLocal.isBefore(todayLocal.minusDays(1))) {
                         widgetStatus = WidgetStatus.FAILED_STREAK;
                         label = "Bạn đã mất chuỗi 😭";
                    } else {
                        // Vẫn còn cơ hội (checkin hôm qua rồi, nay chưa)
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
}