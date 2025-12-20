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
import java.util.Collection;
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
            finalStatus = determineStatus(participant, todayLocal);
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
                .visibility(request.getVisibility()) 
                .createdAt(LocalDateTime.now()) 
                .build();

        checkin = checkinRepository.save(checkin);
        
        updateParticipantStats(participant, finalStatus, todayLocal);

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

        LocalDate lastCheckin = participant.getLastCheckinAt();
        long daysGap = java.time.temporal.ChronoUnit.DAYS.between(lastCheckin, todayLocal);

        if (daysGap > 1) {
            return CheckinStatus.COMEBACK;
        }
        return CheckinStatus.NORMAL;
    }

    private void updateParticipantStats(JourneyParticipant participant, CheckinStatus status, LocalDate todayLocal) {
        if (status == CheckinStatus.REST) {
            participant.setLastCheckinAt(todayLocal);
        } else {
            if (status == CheckinStatus.COMEBACK) {
                participant.setCurrentStreak(1);
            } else {
                participant.setCurrentStreak(participant.getCurrentStreak() + 1);
            }
            participant.setLastCheckinAt(todayLocal);
        }
        participantRepository.save(participant);
    }

    // --- CÁC HÀM GET FEED: ĐÃ THÊM @Transactional(readOnly = true) ---

    @Override
    @Transactional(readOnly = true) // Fix LazyInitializationException
    public Page<CheckinResponse> getJourneyFeed(UUID journeyId, Pageable pageable, User currentUser) {
        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, currentUser.getId())) {
            throw new BadRequestException("Không có quyền xem");
        }
        return checkinRepository.findByJourneyIdOrderByCreatedAtDesc(journeyId, pageable)
                .map(checkinMapper::toResponse)
                .map(this::enrichResponse); 
    }

    @Override
    @Transactional
    public CommentResponse postComment(UUID checkinId, String content, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        if (checkin.getJourney().getInteractionType() == InteractionType.RESTRICTED) {
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
    @Transactional(readOnly = true) // Fix LazyInitializationException cho User trong comment
    public Page<CommentResponse> getComments(UUID checkinId, Pageable pageable) {
        return commentRepository.findByCheckinId(checkinId, SecurityUtils.getCurrentUserId(), pageable)
                .map(checkinMapper::toCommentResponse);
    }

    @Override
    @Transactional(readOnly = true) // Fix LazyInitializationException
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
    @Transactional(readOnly = true) // Fix LazyInitializationException
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

    private Set<Long> getExcludedUserIds(Long userId) {
        Set<Long> blockedIds = userBlockRepository.findAllBlockedUserIdsInteraction(userId);
        blockedIds.add(-1L); 
        return blockedIds;
    }
}