package com.mindrevol.backend.modules.journey.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
// import com.mindrevol.backend.modules.habit.service.HabitService; // <--- XÓA
import com.mindrevol.backend.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.backend.modules.journey.entity.*;
import com.mindrevol.backend.modules.journey.event.JourneyJoinedEvent; // <--- IMPORT MỚI
import com.mindrevol.backend.modules.journey.mapper.JourneyMapper;
import com.mindrevol.backend.modules.journey.repository.JourneyInvitationRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.journey.service.JourneyInvitationService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher; // <--- IMPORT MỚI
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JourneyInvitationServiceImpl implements JourneyInvitationService {

    private final JourneyInvitationRepository invitationRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final UserRepository userRepository;
    // private final HabitService habitService; // <--- XÓA DEPENDENCY NÀY
    private final NotificationService notificationService;
    private final JourneyMapper journeyMapper;
    
    private final ApplicationEventPublisher eventPublisher; // <--- INJECT MỚI

    // ... [Giữ nguyên hàm inviteFriendToJourney] ...
    @Override
    @Transactional
    public void inviteFriendToJourney(User inviter, UUID journeyId, Long friendId) {
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, inviter.getId())) {
            throw new BadRequestException("Bạn không phải thành viên của hành trình này");
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
                journey.getId().toString(), 
                inviter.getAvatarUrl()
        );
        log.info("User {} invited User {} to Journey {}", inviter.getId(), friendId, journeyId);
    }

    @Override
    @Transactional
    public void acceptInvitation(User currentUser, Long invitationId) {
        // 1. Tìm lời mời
        JourneyInvitation invitation = invitationRepository.findByIdAndInviteeId(invitationId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lời mời không tồn tại hoặc không dành cho bạn"));

        if (invitation.getStatus() != JourneyInvitationStatus.PENDING) {
            throw new BadRequestException("Lời mời này đã được xử lý hoặc hết hạn");
        }

        Journey journey = invitation.getJourney();

        // 2. Double check
        if (participantRepository.existsByJourneyIdAndUserId(journey.getId(), currentUser.getId())) {
            invitation.setStatus(JourneyInvitationStatus.ACCEPTED);
            invitationRepository.save(invitation);
            return;
        }

        // 3. Thêm vào nhóm
        JourneyParticipant participant = JourneyParticipant.builder()
                .journey(journey)
                .user(currentUser)
                .role(JourneyRole.MEMBER)
                .currentStreak(0)
                .build();
        participantRepository.save(participant);

        // 4. Bắn Event (Thay vì gọi HabitService)
        eventPublisher.publishEvent(new JourneyJoinedEvent(journey, currentUser));

        // 5. Cập nhật lời mời
        invitation.setStatus(JourneyInvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        log.info("User {} accepted invitation to Journey {}", currentUser.getId(), journey.getId());
    }

    // ... [Các hàm rejectInvitation, getMyPendingInvitations giữ nguyên] ...
    @Override
    @Transactional
    public void rejectInvitation(User currentUser, Long invitationId) {
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
}