package com.mindrevol.backend.modules.journey.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.journey.dto.request.CreateJourneyRequest;
import com.mindrevol.backend.modules.journey.dto.response.JourneyParticipantResponse;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.journey.entity.*;
import com.mindrevol.backend.modules.journey.event.JourneyCreatedEvent;
import com.mindrevol.backend.modules.journey.event.JourneyJoinedEvent;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.journey.service.JourneyService;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JourneyServiceImpl implements JourneyService {

    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // --- HELPER: Lấy User entity ---
    private User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // --- HELPER: Kiểm tra hết hạn ---
    private void checkAndCompleteExpiredJourney(Journey journey) {
        if (journey.getStatus() == JourneyStatus.ONGOING && 
            journey.getEndDate() != null && 
            journey.getEndDate().isBefore(LocalDate.now())) {
            
            journey.setStatus(JourneyStatus.COMPLETED);
            journeyRepository.save(journey);
            log.info("Auto-completed expired journey: {}", journey.getId());
        }
    }

    @Override
    @Transactional
    public JourneyResponse createJourney(CreateJourneyRequest request, Long userId) {
        User currentUser = getUserEntity(userId);

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        Journey journey = Journey.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .visibility(request.getVisibility())
                .requireApproval(request.isRequireApproval())
                .status(determineStatus(request.getStartDate()))
                .inviteCode(generateInviteCode())
                .creator(currentUser)
                .build();
        
        journey = journeyRepository.save(journey);

        JourneyParticipant owner = JourneyParticipant.builder()
                .journey(journey)
                .user(currentUser)
                .role(JourneyRole.OWNER)
                .joinedAt(LocalDateTime.now())
                .build();
        
        participantRepository.save(owner);

        eventPublisher.publishEvent(new JourneyCreatedEvent(journey, currentUser));

        return mapToResponse(journey, owner);
    }

    @Override
    @Transactional
    public JourneyResponse joinJourney(String inviteCode, Long userId) {
        User currentUser = getUserEntity(userId);

        Journey journey = journeyRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Mã mời không hợp lệ"));

        checkAndCompleteExpiredJourney(journey);
        if (journey.getStatus() == JourneyStatus.COMPLETED) {
            throw new BadRequestException("Hành trình này đã kết thúc.");
        }

        if (participantRepository.existsByJourneyIdAndUserId(journey.getId(), userId)) {
            return getJourneyDetail(userId, journey.getId());
        }

        JourneyParticipant member = JourneyParticipant.builder()
                .journey(journey)
                .user(currentUser)
                .role(JourneyRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();
        
        participantRepository.save(member);

        eventPublisher.publishEvent(new JourneyJoinedEvent(journey, currentUser));

        return mapToResponse(journey, member);
    }

    @Override
    public JourneyResponse getJourneyDetail(Long userId, Long journeyId) {
        Journey journey = getJourneyEntity(journeyId);
        checkAndCompleteExpiredJourney(journey);
        
        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journeyId, userId)
                .orElse(null);
                
        if (journey.getVisibility() == JourneyVisibility.PRIVATE && participant == null) {
             throw new BadRequestException("Đây là hành trình riêng tư.");
        }

        return mapToResponse(journey, participant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JourneyResponse> getMyJourneys(Long userId) {
        return participantRepository.findAllByUserId(userId).stream()
                .map(p -> {
                    // [FIX] Thay p.getJourneyId() bằng p.getJourney()
                    // Vì p đã join fetch journey rồi nên lấy object luôn là ngon nhất
                    Journey j = p.getJourney();
                    checkAndCompleteExpiredJourney(j);
                    return mapToResponse(j, p);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void leaveJourney(Long journeyId, Long userId) {
        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journeyId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không tham gia hành trình này"));
        
        if (participant.getRole() == JourneyRole.OWNER) {
            throw new BadRequestException("Chủ hành trình không thể rời đi. Hãy xóa hành trình hoặc chuyển quyền.");
        }
        
        participantRepository.delete(participant);
    }

    @Override
    @Transactional
    public JourneyResponse updateJourney(Long journeyId, CreateJourneyRequest request, Long userId) {
        Journey journey = getJourneyEntity(journeyId);
        
        JourneyParticipant p = participantRepository.findByJourneyIdAndUserId(journeyId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không tham gia hành trình này"));
        if (p.getRole() != JourneyRole.OWNER) {
            throw new BadRequestException("Chỉ chủ hành trình mới được chỉnh sửa.");
        }

        journey.setName(request.getName());
        journey.setDescription(request.getDescription());
        journey.setVisibility(request.getVisibility());
        
        return mapToResponse(journeyRepository.save(journey), p);
    }

    @Override
    @Transactional
    public void kickMember(Long journeyId, Long memberId, Long requesterId) {
        JourneyParticipant requester = participantRepository.findByJourneyIdAndUserId(journeyId, requesterId)
                .orElseThrow(() -> new BadRequestException("Bạn không tham gia hành trình này"));
        if (requester.getRole() != JourneyRole.OWNER) {
            throw new BadRequestException("Chỉ chủ hành trình mới được mời thành viên ra khỏi nhóm.");
        }

        JourneyParticipant victim = participantRepository.findByJourneyIdAndUserId(journeyId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại"));
        
        if (victim.getRole() == JourneyRole.OWNER) {
            throw new BadRequestException("Không thể kick chính mình.");
        }

        participantRepository.delete(victim);
    }

    @Override
    @Transactional
    public void transferOwnership(Long journeyId, Long currentOwnerId, Long newOwnerId) {
        JourneyParticipant currentOwner = participantRepository.findByJourneyIdAndUserId(journeyId, currentOwnerId)
                .orElseThrow(() -> new BadRequestException("Lỗi xác thực chủ sở hữu"));
        
        if (currentOwner.getRole() != JourneyRole.OWNER) {
            throw new BadRequestException("Bạn không phải chủ hành trình.");
        }

        JourneyParticipant newOwner = participantRepository.findByJourneyIdAndUserId(journeyId, newOwnerId)
                .orElseThrow(() -> new BadRequestException("Người nhận không có trong nhóm."));

        currentOwner.setRole(JourneyRole.MEMBER);
        newOwner.setRole(JourneyRole.OWNER);
        
        participantRepository.save(currentOwner);
        participantRepository.save(newOwner);
        
        Journey journey = getJourneyEntity(journeyId);
        // [FIX] newOwner.getUser() thay vì getUserId()
        journey.setCreator(newOwner.getUser()); 
        journeyRepository.save(journey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JourneyParticipantResponse> getJourneyParticipants(Long journeyId) {
        List<JourneyParticipant> participants = participantRepository.findAllByJourneyId(journeyId);
        
        return participants.stream().map(p -> {
            // [FIX] Lấy User object trực tiếp từ quan hệ @ManyToOne
            // Thay vì gọi userRepository.findById(p.getUserId()) gây N+1 query
            User user = p.getUser();
            
            UserSummaryResponse userDto = (user != null) ? UserSummaryResponse.builder()
                    .id(user.getId())
                    .fullname(user.getFullname())
                    .avatarUrl(user.getAvatarUrl())
                    .handle(user.getHandle())
                    .build() : null;

            return JourneyParticipantResponse.builder()
                    .id(p.getId())
                    .user(userDto)
                    .role(p.getRole().name())
                    .joinedAt(p.getJoinedAt())
                    .currentStreak(p.getCurrentStreak())
                    .totalCheckins(p.getTotalCheckins())
                    .lastCheckinAt(p.getLastCheckinAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteJourney(Long journeyId, Long userId) {
        JourneyParticipant p = participantRepository.findByJourneyIdAndUserId(journeyId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không tham gia hành trình này"));
        
        if (p.getRole() != JourneyRole.OWNER) {
            throw new BadRequestException("Chỉ chủ hành trình mới được xóa.");
        }
        
        journeyRepository.deleteById(journeyId);
    }

    @Override
    public Journey getJourneyEntity(Long journeyId) {
        return journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));
    }

    // --- Helper Methods ---

    private JourneyStatus determineStatus(LocalDate startDate) {
        LocalDate now = LocalDate.now();
        if (now.isBefore(startDate)) return JourneyStatus.UPCOMING;
        return JourneyStatus.ONGOING;
    }

    private String generateInviteCode() {
        return RandomStringUtils.randomAlphanumeric(8).toUpperCase();
    }

    private JourneyResponse mapToResponse(Journey journey, JourneyParticipant currentParticipant) {
        long totalMembers = participantRepository.countByJourneyId(journey.getId());

        JourneyResponse.CurrentUserStatus userStatus = null;
        if (currentParticipant != null) {
            boolean checkedInToday = false;
            if (currentParticipant.getLastCheckinAt() != null) {
                checkedInToday = currentParticipant.getLastCheckinAt().toLocalDate().isEqual(LocalDate.now());
            }
            
            userStatus = JourneyResponse.CurrentUserStatus.builder()
                    .role(currentParticipant.getRole().name())
                    .currentStreak(currentParticipant.getCurrentStreak())
                    .totalCheckins(currentParticipant.getTotalCheckins())
                    .hasCheckedInToday(checkedInToday)
                    .build();
        }

        return JourneyResponse.builder()
                .id(journey.getId())
                .name(journey.getName())
                .description(journey.getDescription())
                .startDate(journey.getStartDate())
                .endDate(journey.getEndDate())
                .visibility(journey.getVisibility())
                .status(journey.getStatus())
                .inviteCode(journey.getInviteCode())
                .totalMembers((int) totalMembers)
                .currentUserStatus(userStatus)
                .build();
    }
}