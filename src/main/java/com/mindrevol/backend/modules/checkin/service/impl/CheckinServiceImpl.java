package com.mindrevol.backend.modules.checkin.service.impl;

import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.checkin.dto.request.CheckinRequest;
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
import com.mindrevol.backend.modules.journey.entity.InteractionType;
import com.mindrevol.backend.modules.journey.entity.Journey;
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
import org.springframework.cache.annotation.CacheEvict; // <--- IMPORT QUAN TRỌNG
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Override
    // --- THÊM DÒNG NÀY: Xóa cache widget ngay khi check-in thành công ---
    // Key phải khớp với format bên JourneyServiceImpl: journeyId + '-' + userId
    @CacheEvict(value = "journey_widget", key = "#request.journeyId + '-' + #currentUser.id")
    // -------------------------------------------------------------------
    public CheckinResponse createCheckin(CheckinRequest request, User currentUser) {
        Journey journey = journeyRepository.findById(request.getJourneyId())
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        if (!participantRepository.existsByJourneyIdAndUserId(journey.getId(), currentUser.getId())) {
            throw new BadRequestException("Bạn không phải thành viên");
        }

        String imageUrl = "";
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            imageUrl = fileStorageService.uploadFile(request.getFile());
        }

        return saveCheckinTransaction(currentUser, journey, request, imageUrl);
    }

    // ... [Giữ nguyên toàn bộ phần còn lại của class, không thay đổi logic] ...
    
    @Transactional
    protected CheckinResponse saveCheckinTransaction(User currentUser, Journey journey, CheckinRequest request, String imageUrl) {
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
                log.info("Used Freeze Streak ticket for User {}", currentUser.getId());
            } else {
                log.info("Free REST status for User {} (No ticket required)", currentUser.getId());
            }
            finalStatus = CheckinStatus.REST;
        } else {
            finalStatus = determineStatus(request.getStatusRequest(), journey.getId(), currentUser.getId());
        }

        Checkin checkin = Checkin.builder()
                .user(currentUser)
                .journey(journey)
                .task(task)
                .imageUrl(imageUrl)
                .thumbnailUrl(imageUrl)
                .emotion(request.getEmotion())
                .status(finalStatus)
                .caption(request.getCaption())
                .build();

        checkin = checkinRepository.save(checkin);
        
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

    @Override
    @Transactional
    public CommentResponse postComment(UUID checkinId, String content, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        Journey journey = checkin.getJourney();
        if (journey.getInteractionType() == InteractionType.PRIVATE_REPLY) {
            throw new BadRequestException("Hành trình này đang ở chế độ Riêng tư. Hãy dùng tính năng Nhắn tin (Reply) thay vì Bình luận.");
        }
        if (journey.getInteractionType() == InteractionType.RESTRICTED) {
            throw new BadRequestException("Hành trình này đã tắt tính năng bình luận.");
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
    public Page<CommentResponse> getComments(UUID checkinId, Pageable pageable) {
        return commentRepository.findByCheckinId(checkinId, SecurityUtils.getCurrentUserId(), pageable)
                .map(checkinMapper::toCommentResponse);
    }

    @Override
    public List<CheckinResponse> getUnifiedFeed(User currentUser, LocalDateTime cursor, int limit) {
        if (cursor == null) {
            cursor = LocalDateTime.now();
        }
        Pageable pageable = PageRequest.of(0, limit);
        Set<Long> excludedUserIds = getExcludedUserIds(currentUser.getId());

        return checkinRepository.findUnifiedFeed(currentUser.getId(), cursor, excludedUserIds, pageable)
                .stream()
                .map(checkinMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CheckinResponse> getJourneyFeedByCursor(UUID journeyId, User currentUser, LocalDateTime cursor, int limit) {
        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền xem hành trình này");
        }
        
        if (cursor == null) {
            cursor = LocalDateTime.now();
        }
        Pageable pageable = PageRequest.of(0, limit);
        Set<Long> excludedUserIds = getExcludedUserIds(currentUser.getId());

        return checkinRepository.findJourneyFeedByCursor(journeyId, cursor, excludedUserIds, pageable)
                .stream()
                .map(checkinMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Set<Long> getExcludedUserIds(Long userId) {
        Set<Long> blockedIds = userBlockRepository.findAllBlockedUserIdsInteraction(userId);
        blockedIds.add(-1L); 
        return blockedIds;
    }
}