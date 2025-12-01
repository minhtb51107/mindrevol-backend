package com.mindrevol.backend.modules.journey.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.habit.service.HabitService;
import com.mindrevol.backend.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.backend.modules.journey.entity.*;
import com.mindrevol.backend.modules.journey.repository.JourneyInvitationRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.journey.service.JourneyInvitationService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JourneyInvitationServiceImpl implements JourneyInvitationService {

    private final JourneyInvitationRepository invitationRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final HabitService habitService; // Để tạo Habit khi join

    @Override
    @Transactional
    public void inviteFriendToJourney(User inviter, UUID journeyId, Long friendId) {
        // 1. Kiểm tra Journey
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        // 2. Kiểm tra quyền mời (Phải là thành viên trong nhóm mới được mời người khác)
        // Nếu muốn chặt hơn: Chỉ Admin mới được mời -> check role participant
        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, inviter.getId())) {
            throw new BadRequestException("Bạn không phải thành viên của hành trình này");
        }

        // 3. Kiểm tra người được mời
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        // 4. Kiểm tra xem người kia đã ở trong nhóm chưa
        if (participantRepository.existsByJourneyIdAndUserId(journeyId, friendId)) {
            throw new BadRequestException("Người này đã tham gia hành trình rồi");
        }

        // 5. Kiểm tra xem đã mời chưa (tránh spam)
        if (invitationRepository.existsByJourneyIdAndInviteeIdAndStatus(journeyId, friendId, JourneyInvitationStatus.PENDING)) {
            throw new BadRequestException("Đã gửi lời mời cho người này rồi, hãy chờ họ phản hồi");
        }

        // 6. Tạo lời mời
        JourneyInvitation invitation = JourneyInvitation.builder()
                .journey(journey)
                .inviter(inviter)
                .invitee(friend)
                .status(JourneyInvitationStatus.PENDING)
                .build();

        invitationRepository.save(invitation);
        
        // TODO: Bắn Notification cho friend ("inviter.getFullname() mời bạn tham gia journey.getName()")
        log.info("User {} invited User {} to Journey {}", inviter.getId(), friendId, journeyId);
    }

    @Override
    @Transactional
    public void acceptInvitation(User currentUser, Long invitationId) {
        // 1. Tìm lời mời (và phải đúng là mời mình)
        JourneyInvitation invitation = invitationRepository.findByIdAndInviteeId(invitationId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lời mời không tồn tại hoặc không dành cho bạn"));

        if (invitation.getStatus() != JourneyInvitationStatus.PENDING) {
            throw new BadRequestException("Lời mời này đã được xử lý hoặc hết hạn");
        }

        Journey journey = invitation.getJourney();

        // 2. Double check xem đã vào nhóm chưa (có thể vào bằng code trước đó rồi)
        if (participantRepository.existsByJourneyIdAndUserId(journey.getId(), currentUser.getId())) {
            // Nếu đã vào rồi thì chỉ cần update status invitation cho sạch data
            invitation.setStatus(JourneyInvitationStatus.ACCEPTED);
            invitationRepository.save(invitation);
            return;
        }

        // 3. Thêm vào nhóm (Logic giống hệt Join bằng Code)
        JourneyParticipant participant = JourneyParticipant.builder()
                .journey(journey)
                .user(currentUser)
                .role(JourneyRole.MEMBER)
                .currentStreak(0)
                .build();
        participantRepository.save(participant);

        // 4. Tạo Habit tương ứng (Logic nghiệp vụ của dự án)
        habitService.createHabitFromJourney(journey.getId(), journey.getName(), currentUser);

        // 5. Cập nhật lời mời
        invitation.setStatus(JourneyInvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        log.info("User {} accepted invitation to Journey {}", currentUser.getId(), journey.getId());
    }

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
                .map(this::toResponse);
    }

    private JourneyInvitationResponse toResponse(JourneyInvitation invitation) {
        return JourneyInvitationResponse.builder()
                .id(invitation.getId())
                .journeyId(invitation.getJourney().getId())
                .journeyName(invitation.getJourney().getName())
                .inviterName(invitation.getInviter().getFullname())
                .inviterAvatar(invitation.getInviter().getAvatarUrl())
                .status(invitation.getStatus())
                .sentAt(invitation.getCreatedAt().toLocalDateTime())
                .build();
    }
}