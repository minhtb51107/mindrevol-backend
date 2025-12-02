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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.cache.annotation.Cacheable; // Import Cache
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
@Slf4j
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
    public void leaveJourney(UUID journeyId, User currentUser) {
        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journeyId, currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không ở trong hành trình này"));

        // Nếu là Creator thì không được rời (trừ khi chuyển quyền hoặc xóa nhóm - logic nâng cao sau này)
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

        // Chỉ Admin của nhóm mới được sửa (Check trong bảng participant)
        JourneyParticipant adminPart = participantRepository.findByJourneyIdAndUserId(journeyId, currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên nhóm này"));
        
        if (adminPart.getRole() != JourneyRole.ADMIN) {
            throw new BadRequestException("Chỉ Quản trị viên mới được thay đổi cài đặt");
        }

        // Cập nhật các trường nếu có gửi lên (Partial Update)
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

        // 1. Phải là Admin mới được kick
        if (requester.getRole() != JourneyRole.ADMIN) {
            throw new BadRequestException("Bạn không có quyền mời thành viên ra khỏi nhóm");
        }

        // 2. Không thể tự kick mình
        if (currentUser.getId().equals(memberId)) {
            throw new BadRequestException("Bạn không thể tự kick chính mình. Hãy dùng chức năng Rời nhóm.");
        }

        JourneyParticipant victim = participantRepository.findByJourneyIdAndUserId(journeyId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong nhóm"));

        // 3. LOGIC MỚI: Check quyền cấp cao (Creator > Admin > Member)
        boolean isRequesterCreator = requester.getJourney().getCreator().getId().equals(currentUser.getId());
        boolean isVictimAdmin = victim.getRole() == JourneyRole.ADMIN;

        // Nếu Victim là Admin, thì chỉ có Creator mới được kick
        if (isVictimAdmin && !isRequesterCreator) {
            throw new BadRequestException("Bạn không thể kick một Quản trị viên khác (Chỉ người tạo nhóm mới có quyền này)");
        }

        participantRepository.delete(victim);
    }

    @Override
    @Transactional(readOnly = true)
    // Cache kết quả widget trong 10 phút. Key là kết hợp giữa journeyId và userId
    @Cacheable(value = "journey_widget", key = "#journeyId + '-' + #userId") 
    public JourneyWidgetResponse getWidgetInfo(UUID journeyId, Long userId) {
        log.info("Fetching Widget Info from Database for User {} Journey {}", userId, journeyId); // Log để kiểm tra cache có hoạt động không

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
                if (participant.getJourney().isHasStreak()) {
                    if (participant.getCurrentStreak() == 0 && participant.getJoinedAt().toLocalDate().isBefore(LocalDate.now())) {
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
}