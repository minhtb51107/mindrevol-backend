package com.mindrevol.backend.modules.checkin.service.impl;

import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.checkin.service.CheckinService;
import com.mindrevol.backend.modules.gamification.entity.PointHistory;
import com.mindrevol.backend.modules.gamification.entity.PointSource;
import com.mindrevol.backend.modules.gamification.repository.PointHistoryRepository;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyTask;
import com.mindrevol.backend.modules.journey.entity.JourneyType;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyTaskRepository;
import com.mindrevol.backend.modules.storage.service.FileStorageService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository; // <--- MỚI: Cần cái này để trừ vé
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final JourneyTaskRepository journeyTaskRepository;
    private final UserRepository userRepository; // <--- Inject UserRepository
    private final FileStorageService fileStorageService;
    private final CheckinMapper checkinMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PointHistoryRepository pointHistoryRepository;

    @Override
    public CheckinResponse createCheckin(CheckinRequest request, User currentUser) {
        // 1. Validate sơ bộ
        Journey journey = journeyRepository.findById(request.getJourneyId())
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        if (!participantRepository.existsByJourneyIdAndUserId(journey.getId(), currentUser.getId())) {
            throw new BadRequestException("Bạn không phải thành viên");
        }

        // 2. Upload Ảnh
        String imageUrl = fileStorageService.uploadFile(request.getFile());
        String thumbnailUrl = fileStorageService.uploadThumbnail(request.getFile());

        // 3. Vào Transaction xử lý logic phức tạp
        return saveCheckinTransaction(currentUser, journey, request, imageUrl, thumbnailUrl);
    }

    @Transactional
    protected CheckinResponse saveCheckinTransaction(User currentUser, Journey journey, CheckinRequest request, String imageUrl, String thumbnailUrl) {
        
        // --- 1. LOGIC KIỂM TRA NHIỆM VỤ (TASK) - ROADMAP ---
        JourneyTask task = null;

        if (journey.getType() == JourneyType.ROADMAP) {
            // Nếu là hành trình có lộ trình -> Bắt buộc hoặc Khuyến khích chọn Task
            if (request.getTaskId() != null) {
                // Lấy thông tin task
                task = journeyTaskRepository.findById(request.getTaskId())
                        .orElseThrow(() -> new ResourceNotFoundException("Nhiệm vụ không tồn tại"));

                // Task phải thuộc Journey này
                if (!task.getJourney().getId().equals(journey.getId())) {
                    throw new BadRequestException("Nhiệm vụ này không thuộc hành trình hiện tại");
                }

                // CHECK QUAN TRỌNG: Đã làm task này chưa?
                boolean alreadyDone = checkinRepository.existsByUserIdAndTaskId(currentUser.getId(), task.getId());
                if (alreadyDone) {
                    throw new BadRequestException("Bạn đã hoàn thành nhiệm vụ '" + task.getTitle() + "' rồi!");
                }
            }
        } else {
            // Nếu là HABIT -> Không được gửi taskId lên
            if (request.getTaskId() != null) {
                throw new BadRequestException("Hành trình thói quen không hỗ trợ chọn nhiệm vụ.");
            }
        }

        // --- 2. XÁC ĐỊNH TRẠNG THÁI (REST vs NORMAL/FAILED/COMEBACK) ---
        CheckinStatus finalStatus;

        if (request.getStatusRequest() == CheckinStatus.REST) {
            // === NGƯỜI DÙNG XIN NGHỈ (SỬ DỤNG ITEM) ===
            
            // a. Kiểm tra trong kho có vé không?
            if (currentUser.getFreezeStreakCount() <= 0) {
                throw new BadRequestException("Bạn đã hết vé Nghỉ Phép (Freeze Streak)! Hãy tích điểm để đổi thêm.");
            }

            // b. Trừ 1 vé và Lưu User
            currentUser.setFreezeStreakCount(currentUser.getFreezeStreakCount() - 1);
            userRepository.save(currentUser);

            finalStatus = CheckinStatus.REST;
            log.info("User {} used a Freeze Streak item. Remaining: {}", currentUser.getId(), currentUser.getFreezeStreakCount());

        } else {
            // Logic cũ (Failed/Comeback/Normal)
            finalStatus = determineStatus(request.getStatusRequest(), journey.getId(), currentUser.getId());
        }

        // --- 3. LƯU CHECKIN ---
        Checkin checkin = Checkin.builder()
                .user(currentUser)
                .journey(journey)
                .task(task) // Lưu task vào (nếu có)
                .imageUrl(imageUrl)
                .thumbnailUrl(thumbnailUrl)
                .emotion(request.getEmotion())
                .status(finalStatus)
                .caption(request.getCaption())
                .build();

        checkin = checkinRepository.save(checkin);
        
     // Logic: Chỉ cộng điểm khi trạng thái là NORMAL hoặc COMEBACK (REST thì không được cộng)
        if (checkin.getStatus() == CheckinStatus.NORMAL || checkin.getStatus() == CheckinStatus.COMEBACK) {
            long pointsEarned = 10L; // Mặc định 10 điểm
            
            // Nếu là Comeback thì thưởng ít hơn chút để phạt nhẹ
            if (checkin.getStatus() == CheckinStatus.COMEBACK) {
                pointsEarned = 5L; 
            }

            // Cộng tiền vào ví User
            currentUser.setPoints(currentUser.getPoints() + pointsEarned);
            userRepository.save(currentUser);

            // Ghi log lịch sử
            PointHistory history = PointHistory.builder()
                    .user(currentUser)
                    .amount(pointsEarned)
                    .balanceAfter(currentUser.getPoints())
                    .reason("Thưởng check-in ngày " + LocalDate.now())
                    .source(PointSource.CHECKIN)
                    .build();
            pointHistoryRepository.save(history);
            
            log.info("Awarded {} points to User {}", pointsEarned, currentUser.getId());
        }

        // --- 4. BẮN SỰ KIỆN ---
        // Sự kiện này sẽ được GamificationEventListener bắt để cộng điểm (nếu Normal) 
        // hoặc giữ streak (nếu Rest)
        eventPublisher.publishEvent(new CheckinSuccessEvent(
                checkin.getId(),
                currentUser.getId(),
                journey.getId(),
                checkin.getCreatedAt()
        ));

        return checkinMapper.toResponse(checkin);
    }

    @Override
    public Page<CheckinResponse> getJourneyFeed(UUID journeyId, Pageable pageable, User currentUser) {
        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, currentUser.getId())) {
            throw new BadRequestException("Không có quyền xem");
        }
        return checkinRepository.findByJourneyIdOrderByCreatedAtDesc(journeyId, pageable)
                .map(checkinMapper::toResponse);
    }

    private CheckinStatus determineStatus(CheckinStatus requested, UUID journeyId, Long userId) {
        if (requested == CheckinStatus.FAILED) return CheckinStatus.FAILED;
        
        Optional<Checkin> last = checkinRepository.findTopByJourneyIdAndUserIdOrderByCreatedAtDesc(journeyId, userId);
        if (last.isPresent() && last.get().getStatus() == CheckinStatus.FAILED) {
            return CheckinStatus.COMEBACK;
        }
        return CheckinStatus.NORMAL;
    }
}