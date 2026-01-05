package com.mindrevol.backend.modules.journey.service.impl;

import com.mindrevol.backend.common.constant.AppConstants;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.backend.modules.journey.entity.*;
import com.mindrevol.backend.modules.journey.event.JourneyJoinedEvent;
import com.mindrevol.backend.modules.journey.mapper.JourneyMapper;
import com.mindrevol.backend.modules.journey.repository.JourneyInvitationRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRequestRepository;
import com.mindrevol.backend.modules.journey.service.JourneyInvitationService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JourneyInvitationServiceImpl implements JourneyInvitationService {

    private final JourneyInvitationRepository invitationRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final JourneyMapper journeyMapper;
    private final JourneyRequestRepository journeyRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void inviteFriendToJourney(User inviter, String journeyId, String friendId) {
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        // 1. Kiểm tra người mời có trong nhóm không
        JourneyParticipant inviterParticipant = participantRepository.findByJourneyIdAndUserId(journeyId, inviter.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên của hành trình này"));

        // 2. Kiểm tra quyền mời (Private thì chỉ Owner được mời)
        if (journey.getVisibility() == JourneyVisibility.PRIVATE) {
            if (inviterParticipant.getRole() != JourneyRole.OWNER) {
                throw new BadRequestException("Hành trình riêng tư: Chỉ chủ phòng mới được mời thành viên.");
            }
        }

        // 3. [LOGIC MỚI] Kiểm tra giới hạn thành viên (Dựa trên gói của CREATOR)
        User owner = journey.getCreator();
        int limit = owner.isPremium() ? AppConstants.MAX_PARTICIPANTS_GOLD : AppConstants.MAX_PARTICIPANTS_FREE;
        
        long currentMembers = participantRepository.countByJourneyId(journeyId);
        if (currentMembers >= limit) {
            throw new BadRequestException("Hành trình đã đạt giới hạn thành viên (" + limit + " người). Chủ phòng cần nâng cấp Gold để mở rộng.");
        }

        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (participantRepository.existsByJourneyIdAndUserId(journeyId, friendId)) {
            throw new BadRequestException("Người này đã tham gia hành trình rồi");
        }

        if (invitationRepository.existsByJourneyIdAndInviteeIdAndStatus(journeyId, friendId, JourneyInvitationStatus.PENDING)) {
            throw new BadRequestException("Đã gửi lời mời cho người này rồi, hãy chờ họ phản hồi");
        }

        JourneyInvitation invitation = JourneyInvitation.builder()
                .journey(journey)
                .inviter(inviter)
                .invitee(friend)
                .status(JourneyInvitationStatus.PENDING)
                .build();

        invitationRepository.save(invitation);
        
        notificationService.sendAndSaveNotification(
                friend.getId(),
                inviter.getId(),
                NotificationType.JOURNEY_INVITE,
                "Lời mời tham gia hành trình 🚀",
                inviter.getFullname() + " mời bạn tham gia: " + journey.getName(),
                journey.getId(), 
                inviter.getAvatarUrl()
        );
        log.info("User {} invited User {} to Journey {}", inviter.getId(), friendId, journeyId);
    }

    @Override
    @Transactional
    public void acceptInvitation(User currentUser, String invitationId) {
        JourneyInvitation invitation = invitationRepository.findByIdAndInviteeId(invitationId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lời mời không tồn tại hoặc không dành cho bạn"));

        if (invitation.getStatus() != JourneyInvitationStatus.PENDING) {
            throw new BadRequestException("Lời mời này đã được xử lý hoặc hết hạn");
        }

        Journey journey = invitation.getJourney();

        // [LOGIC MỚI] Check lại giới hạn (Race condition protection)
        User owner = journey.getCreator();
        int limit = owner.isPremium() ? AppConstants.MAX_PARTICIPANTS_GOLD : AppConstants.MAX_PARTICIPANTS_FREE;
        
        long currentMembers = participantRepository.countByJourneyId(journey.getId());
        if (currentMembers >= limit) {
             throw new BadRequestException("Rất tiếc, hành trình này vừa đủ người rồi (" + limit + " thành viên).");
        }

        // Nếu đã là thành viên -> Dọn dẹp
        if (participantRepository.existsByJourneyIdAndUserId(journey.getId(), currentUser.getId())) {
            invitation.setStatus(JourneyInvitationStatus.ACCEPTED);
            invitationRepository.save(invitation);
            cleanupPendingRequests(journey.getId(), currentUser.getId());
            return;
        }

        // Vào nhóm
        JourneyParticipant participant = JourneyParticipant.builder()
                .journey(journey) 
                .user(currentUser)
                .role(JourneyRole.MEMBER)
                .currentStreak(0)
                .totalCheckins(0)
                .joinedAt(LocalDateTime.now())
                .build();
        
        participantRepository.save(participant);

        invitation.setStatus(JourneyInvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        cleanupPendingRequests(journey.getId(), currentUser.getId());
        eventPublisher.publishEvent(new JourneyJoinedEvent(journey, currentUser));
        
        log.info("User {} joined Journey {} directly via invitation", currentUser.getId(), journey.getId());
    }

    @Override
    @Transactional
    public void rejectInvitation(User currentUser, String invitationId) {
        JourneyInvitation invitation = invitationRepository.findByIdAndInviteeId(invitationId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lời mời không tồn tại"));

        if (invitation.getStatus() != JourneyInvitationStatus.PENDING) {
            throw new BadRequestException("Lời mời không hợp lệ");
        }

        invitation.setStatus(JourneyInvitationStatus.REJECTED);
        invitationRepository.save(invitation);
    }

    @Override
    public Page<JourneyInvitationResponse> getMyPendingInvitations(User currentUser, Pageable pageable) {
        return invitationRepository.findPendingInvitationsForUser(currentUser.getId(), pageable)
                .map(journeyMapper::toInvitationResponse);
    }

    private void cleanupPendingRequests(String journeyId, String userId) {
        List<JourneyRequest> pendingRequests = journeyRequestRepository.findAllByJourneyIdAndStatus(journeyId, RequestStatus.PENDING);
        for (JourneyRequest req : pendingRequests) {
            if (req.getUser().getId().equals(userId)) {
                req.setStatus(RequestStatus.ACCEPTED); 
                journeyRequestRepository.save(req);
            }
        }
    }
}