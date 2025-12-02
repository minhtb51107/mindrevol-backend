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
import com.mindrevol.backend.modules.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CheckinMapper checkinMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PointHistoryRepository pointHistoryRepository;

    @Override
    public CheckinResponse createCheckin(CheckinRequest request, User currentUser) {
        Journey journey = journeyRepository.findById(request.getJourneyId())
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        if (!participantRepository.existsByJourneyIdAndUserId(journey.getId(), currentUser.getId())) {
            throw new BadRequestException("Bạn không phải thành viên");
        }

        // Upload Ảnh (Chỉ upload ảnh gốc, bỏ qua thumbnail để tối ưu tốc độ)
        String imageUrl = "";
        
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            imageUrl = fileStorageService.uploadFile(request.getFile());
        }

        return saveCheckinTransaction(currentUser, journey, request, imageUrl);
    }

    @Transactional
    protected CheckinResponse saveCheckinTransaction(User currentUser, Journey journey, CheckinRequest request, String imageUrl) {
        
        JourneyTask task = null;

        // Logic Task cho Roadmap
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
            }
        }

        CheckinStatus finalStatus;

        if (request.getStatusRequest() == CheckinStatus.REST) {
            // === NGƯỜI DÙNG XIN NGHỈ ===
            
            // Logic dùng cấu hình từ DB
            if (journey.isRequiresFreezeTicket()) {
                // Nếu hành trình YÊU CẦU VÉ (Kỷ luật)
                if (currentUser.getFreezeStreakCount() <= 0) {
                    throw new BadRequestException("Bạn đã hết vé Nghỉ Phép! Hãy tích điểm để đổi thêm.");
                }
                currentUser.setFreezeStreakCount(currentUser.getFreezeStreakCount() - 1);
                userRepository.save(currentUser);
                log.info("Used Freeze Streak ticket for User {}", currentUser.getId());
            } else {
                // Nếu hành trình KHÔNG CẦN VÉ (Giải trí)
                log.info("Free REST status for User {} (No ticket required)", currentUser.getId());
            }

            finalStatus = CheckinStatus.REST;

        } else {
            finalStatus = determineStatus(request.getStatusRequest(), journey.getId(), currentUser.getId());
        }

        // --- LƯU CHECKIN ---
        Checkin checkin = Checkin.builder()
                .user(currentUser)
                .journey(journey)
                .task(task)
                .imageUrl(imageUrl)
                .thumbnailUrl(imageUrl) // Tạm thời dùng ảnh gốc làm thumbnail, Worker sẽ update lại sau
                .emotion(request.getEmotion())
                .status(finalStatus)
                .caption(request.getCaption())
                .build();

        checkin = checkinRepository.save(checkin);
        
        // Chỉ cộng điểm khi NORMAL hoặc COMEBACK
        if (checkin.getStatus() == CheckinStatus.NORMAL || checkin.getStatus() == CheckinStatus.COMEBACK) {
            long pointsEarned = (checkin.getStatus() == CheckinStatus.COMEBACK) ? 5L : 10L;

            currentUser.setPoints(currentUser.getPoints() + pointsEarned);
            userRepository.save(currentUser);

            PointHistory history = PointHistory.builder()
                    .user(currentUser)
                    .amount(pointsEarned)
                    .balanceAfter(currentUser.getPoints())
                    .reason("Thưởng check-in")
                    .source(PointSource.CHECKIN)
                    .build();
            pointHistoryRepository.save(history);
        }

        // Bắn sự kiện (ImageProcessingListener sẽ bắt sự kiện này để xử lý thumbnail ngầm)
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