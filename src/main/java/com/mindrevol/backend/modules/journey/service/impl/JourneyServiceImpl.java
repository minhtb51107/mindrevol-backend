package com.mindrevol.backend.modules.journey.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.modules.habit.service.HabitService;
import com.mindrevol.backend.modules.journey.dto.request.*;
import com.mindrevol.backend.modules.journey.dto.response.*;
import com.mindrevol.backend.modules.journey.entity.*;
import com.mindrevol.backend.modules.journey.mapper.JourneyMapper;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.journey.service.JourneyService;
import com.mindrevol.backend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JourneyServiceImpl implements JourneyService {

    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final HabitService habitService;
    private final GamificationService gamificationService;
    private final JourneyMapper journeyMapper;
    private final CheckinRepository checkinRepository;
    
    @Override
    @Transactional
    public JourneyResponse createJourney(CreateJourneyRequest request, User currentUser) {
        
        String inviteCode = generateUniqueInviteCode();

        // --- 1. LOGIC MỚI: ÁP DỤNG TEMPLATE CẤU HÌNH ---
        boolean hasStreak = true;
        boolean reqTicket = true;
        boolean isHardcore = true;

        if (request.getType() == JourneyType.MEMORIES || request.getType() == JourneyType.PROJECT) {
            // Chế độ "Giải trí / Công việc": Tắt áp lực
            hasStreak = false;       // Không cần đếm chuỗi liên tục
            reqTicket = false;       // Nghỉ thoải mái không mất vé
            isHardcore = false;      // Nhắc nhở nhẹ nhàng, mời gọi
        } 
        // Mặc định HABIT, ROADMAP, CHALLENGE sẽ là TRUE (Chế độ Kỷ luật)

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
                // Lưu cấu hình vào DB
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

        // Chỉ tạo Habit cho loại HABIT (các loại khác tự quản lý theo cách riêng)
        if (request.getType() == JourneyType.HABIT) {
            habitService.createHabitFromJourney(savedJourney.getId(), savedJourney.getName(), currentUser);
        }

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
    public List<RoadmapStatusResponse> getJourneyRoadmap(UUID journeyId, Long currentUserId) {
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        if (journey.getType() != JourneyType.ROADMAP) {
            throw new BadRequestException("Hành trình này không có lộ trình (Roadmap)");
        }

        List<JourneyTask> tasks = journey.getRoadmap();
        Set<UUID> completedTaskIds = checkinRepository.findCompletedTaskIdsByUserAndJourney(currentUserId, journeyId);

        return tasks.stream().map(task -> RoadmapStatusResponse.builder()
                .taskId(task.getId())
                .dayNo(task.getDayNo())
                .title(task.getTitle())
                .description(task.getDescription())
                .isCompleted(completedTaskIds.contains(task.getId()))
                .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JourneyResponse joinJourney(JoinJourneyRequest request, User currentUser) {
        Journey journey = journeyRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hành trình với mã này"));

        if (participantRepository.existsByJourneyIdAndUserId(journey.getId(), currentUser.getId())) {
            throw new BadRequestException("Bạn đã tham gia hành trình này rồi");
        }

        JourneyParticipant participant = JourneyParticipant.builder()
                .journey(journey)
                .user(currentUser)
                .role(JourneyRole.MEMBER)
                .currentStreak(0)
                .build();
        participantRepository.save(participant);
        
        // Nếu là Habit thì sync sang Habit module
        if (journey.getType() == JourneyType.HABIT) {
            habitService.createHabitFromJourney(journey.getId(), journey.getName(), currentUser);
        }

        return journeyMapper.toResponse(journey);
    }

    @Override
    public List<JourneyResponse> getMyJourneys(User currentUser) {
        List<JourneyParticipant> participants = participantRepository.findAllByUserId(currentUser.getId());
        
        return participants.stream()
                .map(p -> {
                    // Logic refresh streak: Chỉ chạy nếu hành trình CÓ BẬT tính năng Streak
                    if (p.getJourney().isHasStreak()) {
                        gamificationService.refreshUserStreak(p.getJourney().getId(), currentUser.getId());
                    }
                    return journeyMapper.toResponse(p.getJourney());
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void kickMember(UUID journeyId, Long memberId, User currentUser) {
        JourneyParticipant requester = participantRepository.findByJourneyIdAndUserId(journeyId, currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không ở trong hành trình này"));

        if (requester.getRole() != JourneyRole.ADMIN) {
            throw new BadRequestException("Chỉ Admin mới có quyền mời thành viên ra khỏi nhóm");
        }

        if (currentUser.getId().equals(memberId)) {
            throw new BadRequestException("Bạn không thể tự kick chính mình. Hãy dùng chức năng Rời nhóm.");
        }

        JourneyParticipant victim = participantRepository.findByJourneyIdAndUserId(journeyId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong nhóm"));

        participantRepository.delete(victim);
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
    public JourneyWidgetResponse getWidgetInfo(UUID journeyId, Long userId) {
        
        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journeyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn không tham gia hành trình này"));

        Optional<Checkin> lastCheckinOpt = checkinRepository.findTopByJourneyIdAndUserIdOrderByCreatedAtDesc(journeyId, userId);

        boolean isCompletedToday = false;
        String thumbnailUrl = null;
        WidgetStatus widgetStatus = WidgetStatus.PENDING;
        String label = "Hãy check-in ngay!";

        if (lastCheckinOpt.isPresent()) {
            Checkin lastCheckin = lastCheckinOpt.get();
            thumbnailUrl = lastCheckin.getThumbnailUrl();
            
            if (lastCheckin.getCreatedAt().toLocalDate().isEqual(LocalDate.now())) {
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
                // Check logic Streak nếu hành trình có bật Streak
                if (participant.getJourney().isHasStreak()) {
                    if (participant.getCurrentStreak() == 0 && participant.getJoinedAt().toLocalDate().isBefore(LocalDate.now())) {
                        widgetStatus = WidgetStatus.FAILED_STREAK;
                        label = "Bạn đã mất chuỗi 😭";
                    } else {
                        widgetStatus = WidgetStatus.PENDING;
                        label = "Sẵn sàng chưa? 📸";
                    }
                } else {
                    // Nếu không tính streak (Memories) -> Luôn hiện trạng thái chờ bình thường
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
}