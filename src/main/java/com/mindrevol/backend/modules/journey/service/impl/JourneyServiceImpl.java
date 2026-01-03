package com.mindrevol.backend.modules.journey.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.journey.dto.request.CreateJourneyRequest;
import com.mindrevol.backend.modules.journey.dto.response.*;
import com.mindrevol.backend.modules.journey.entity.*;
import com.mindrevol.backend.modules.journey.event.JourneyCreatedEvent;
import com.mindrevol.backend.modules.journey.event.JourneyJoinedEvent;
import com.mindrevol.backend.modules.journey.repository.JourneyInvitationRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRequestRepository;
import com.mindrevol.backend.modules.journey.service.JourneyService;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.Friendship;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.FriendshipRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
public class JourneyServiceImpl implements JourneyService {

    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final JourneyRequestRepository journeyRequestRepository;
    private final JourneyInvitationRepository journeyInvitationRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final CheckinRepository checkinRepository;
    private final CheckinMapper checkinMapper;

    // --- 1. LẤY HÀNH TRÌNH ĐANG CHẠY (ACTIVE) ---
    @Override
    @Transactional 
    public List<UserActiveJourneyResponse> getUserActiveJourneys(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        List<JourneyParticipant> participants = participantRepository.findAllActiveByUserId(userId);
        List<JourneyParticipant> validParticipants = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (JourneyParticipant p : participants) {
            Journey j = p.getJourney();
            
            // Lazy Update: Nếu quá hạn -> Đóng ngay
            if (j.getStatus() == JourneyStatus.ONGOING && 
                j.getEndDate() != null && 
                j.getEndDate().isBefore(today)) {
                
                log.info("Lazy update (Active List): Journey {} expired. Switching to COMPLETED.", j.getId());
                j.setStatus(JourneyStatus.COMPLETED);
                journeyRepository.save(j);
                continue; 
            }
            validParticipants.add(p);
        }

        return mapToUserJourneyResponse(validParticipants, userId);
    }

    // --- 2. LẤY HÀNH TRÌNH ĐÃ KẾT THÚC (FINISHED) ---
    @Override
    @Transactional 
    public List<UserActiveJourneyResponse> getUserFinishedJourneys(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        List<JourneyParticipant> allParticipants = participantRepository.findAllByUserId(userId);
        List<JourneyParticipant> finishedParticipants = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (JourneyParticipant p : allParticipants) {
            Journey j = p.getJourney();
            
            if (j.getStatus() == JourneyStatus.COMPLETED) {
                finishedParticipants.add(p);
            } 
            else if (j.getStatus() == JourneyStatus.ONGOING && 
                     j.getEndDate() != null && 
                     j.getEndDate().isBefore(today)) {
                
                log.info("Lazy update (Finished List): Journey {} expired. Switching to COMPLETED.", j.getId());
                j.setStatus(JourneyStatus.COMPLETED);
                journeyRepository.save(j);
                
                finishedParticipants.add(p);
            }
        }

        return mapToUserJourneyResponse(finishedParticipants, userId);
    }

    // [CẬP NHẬT] Map thêm endDate
    private List<UserActiveJourneyResponse> mapToUserJourneyResponse(List<JourneyParticipant> participants, String userId) {
        return participants.stream().map(participant -> {
            Journey journey = participant.getJourney();
            
            List<Checkin> myCheckins = checkinRepository.findByJourneyIdAndUserId(journey.getId(), userId);
            List<CheckinResponse> checkinResponses = myCheckins.stream()
                    .sorted(Comparator.comparing(Checkin::getCreatedAt).reversed())
                    .map(checkinMapper::toResponse)
                    .collect(Collectors.toList());

            boolean hasNewUpdates = false;
            Pageable limitOne = PageRequest.of(0, 1);
            var latestPage = checkinRepository.findByJourneyIdOrderByCreatedAtDesc(journey.getId(), limitOne);
            
            if (latestPage.hasContent()) {
                Checkin latestCheckin = latestPage.getContent().get(0);
                if (!latestCheckin.getUser().getId().equals(userId)) {
                    hasNewUpdates = true;
                }
            }

            return UserActiveJourneyResponse.builder()
                    .id(journey.getId())
                    .name(journey.getName())
                    .description(journey.getDescription())
                    .status(journey.getStatus().name())
                    .visibility(journey.getVisibility().name())
                    .startDate(journey.getStartDate())
                    .endDate(journey.getEndDate()) // [MỚI] Thêm dòng này
                    .totalCheckins(participant.getTotalCheckins())
                    .checkins(checkinResponses)
                    .hasNewUpdates(hasNewUpdates)
                    .build();
        }).collect(Collectors.toList());
    }

    // --- CÁC HÀM KHÁC GIỮ NGUYÊN ---
    
    @Override
    @Transactional(readOnly = true)
    public JourneyAlertResponse getJourneyAlerts(String userId) {
        long pendingInvites = journeyInvitationRepository.countByInviteeIdAndStatus(userId, JourneyInvitationStatus.PENDING);
        List<String> myOwnedJourneyIds = participantRepository.findAllByUserId(userId).stream()
                .filter(p -> p.getRole() == JourneyRole.OWNER).map(p -> p.getJourney().getId()).collect(Collectors.toList());
        long totalRequests = 0;
        List<String> idsWithRequests = new ArrayList<>();
        if (!myOwnedJourneyIds.isEmpty()) {
            for (String jId : myOwnedJourneyIds) {
                long reqCount = journeyRequestRepository.countByJourneyIdAndStatus(jId, RequestStatus.PENDING);
                if (reqCount > 0) { totalRequests += reqCount; idsWithRequests.add(jId); }
            }
        }
        return JourneyAlertResponse.builder().journeyPendingInvitations(pendingInvites).waitingApprovalRequests(totalRequests).journeyIdsWithRequests(idsWithRequests).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getInvitableFriends(String journeyId, String userId) {
        if (!journeyRepository.existsById(journeyId)) throw new ResourceNotFoundException("Hành trình không tồn tại");
        List<JourneyParticipant> participants = participantRepository.findAllByJourneyId(journeyId);
        Set<String> participantIds = participants.stream().map(p -> p.getUser().getId()).collect(Collectors.toSet());
        List<Friendship> friendships = friendshipRepository.findAllAcceptedFriendsList(userId);
        return friendships.stream().map(f -> f.getFriend(userId)).filter(friend -> !participantIds.contains(friend.getId()))
                .map(friend -> UserSummaryResponse.builder().id(friend.getId()).fullname(friend.getFullname()).avatarUrl(friend.getAvatarUrl()).handle(friend.getHandle()).build()).collect(Collectors.toList());
    }

    private User getUserEntity(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void checkAndCompleteExpiredJourney(Journey journey) {
        if (journey.getStatus() == JourneyStatus.ONGOING && 
            journey.getEndDate() != null && 
            journey.getEndDate().isBefore(LocalDate.now())) {
            
            journey.setStatus(JourneyStatus.COMPLETED);
            journeyRepository.save(journey);
        }
    }

    @Override
    @Transactional
    public JourneyResponse createJourney(CreateJourneyRequest request, String userId) {
        User currentUser = getUserEntity(userId);
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        Journey journey = Journey.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .visibility(request.getVisibility())
                .requireApproval(true) 
                .status(determineStatus(request.getStartDate()))
                .inviteCode(RandomStringUtils.randomAlphanumeric(8).toUpperCase())
                .creator(currentUser)
                .build();
        
        journey = journeyRepository.save(journey);
        JourneyParticipant owner = JourneyParticipant.builder().journey(journey).user(currentUser).role(JourneyRole.OWNER).joinedAt(LocalDateTime.now()).build();
        participantRepository.save(owner);
        eventPublisher.publishEvent(new JourneyCreatedEvent(journey, currentUser));
        return mapToResponse(journey, owner, null);
    }

    @Override
    @Transactional
    public JourneyResponse joinJourney(String inviteCode, String userId) {
        User currentUser = getUserEntity(userId);
        Journey journey = journeyRepository.findByInviteCode(inviteCode).orElseThrow(() -> new ResourceNotFoundException("Mã mời không hợp lệ"));
        checkAndCompleteExpiredJourney(journey);
        
        if (journey.getStatus() == JourneyStatus.COMPLETED) throw new BadRequestException("Hành trình đã kết thúc.");
        if (participantRepository.existsByJourneyIdAndUserId(journey.getId(), userId)) return getJourneyDetail(userId, journey.getId());

        if (journey.isRequireApproval()) {
            if (journeyRequestRepository.existsByJourneyIdAndUserIdAndStatus(journey.getId(), userId, RequestStatus.PENDING)) return mapToResponse(journey, null, "PENDING");
            journeyRequestRepository.save(JourneyRequest.builder().journey(journey).user(currentUser).status(RequestStatus.PENDING).build());
            return mapToResponse(journey, null, "PENDING");
        }

        JourneyParticipant member = JourneyParticipant.builder().journey(journey).user(currentUser).role(JourneyRole.MEMBER).joinedAt(LocalDateTime.now()).build();
        participantRepository.save(member);
        eventPublisher.publishEvent(new JourneyJoinedEvent(journey, currentUser));
        return mapToResponse(journey, member, null);
    }

    @Override
    public JourneyResponse getJourneyDetail(String userId, String journeyId) {
        Journey journey = getJourneyEntity(journeyId);
        checkAndCompleteExpiredJourney(journey);
        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journeyId, userId).orElse(null);
        String pendingStatus = null;
        if (participant == null && journey.isRequireApproval()) {
             if(journeyRequestRepository.existsByJourneyIdAndUserIdAndStatus(journeyId, userId, RequestStatus.PENDING)) pendingStatus = "PENDING";
        }
        if (journey.getVisibility() == JourneyVisibility.PRIVATE && participant == null && pendingStatus == null) throw new BadRequestException("Đây là hành trình riêng tư.");
        return mapToResponse(journey, participant, pendingStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JourneyResponse> getMyJourneys(String userId) {
        List<JourneyResponse> joined = participantRepository.findAllByUserId(userId).stream()
                .map(p -> { checkAndCompleteExpiredJourney(p.getJourney()); return mapToResponse(p.getJourney(), p, null); })
                .collect(Collectors.toList());
        List<JourneyResponse> pending = journeyRequestRepository.findAllByUserIdAndStatus(userId, RequestStatus.PENDING).stream()
                .map(req -> mapToResponse(req.getJourney(), null, "PENDING"))
                .collect(Collectors.toList());
        
        List<JourneyResponse> all = new ArrayList<>(joined);
        all.addAll(pending);
        return all;
    }

    @Override
    @Transactional
    public void leaveJourney(String journeyId, String userId) {
        JourneyParticipant p = participantRepository.findByJourneyIdAndUserId(journeyId, userId).orElseThrow(() -> new BadRequestException("Bạn không tham gia"));
        if (p.getRole() == JourneyRole.OWNER) throw new BadRequestException("Chủ hành trình không thể rời đi.");
        participantRepository.delete(p);
    }

    @Override
    @Transactional
    public JourneyResponse updateJourney(String journeyId, CreateJourneyRequest request, String userId) {
        Journey journey = getJourneyEntity(journeyId);
        JourneyParticipant p = participantRepository.findByJourneyIdAndUserId(journeyId, userId).orElseThrow(() -> new BadRequestException("Không tham gia"));
        if (p.getRole() != JourneyRole.OWNER) throw new BadRequestException("Chỉ chủ hành trình mới được sửa.");
        journey.setName(request.getName());
        journey.setDescription(request.getDescription());
        journey.setVisibility(request.getVisibility());
        return mapToResponse(journeyRepository.save(journey), p, null);
    }

    @Override
    @Transactional
    public void kickMember(String journeyId, String memberId, String requesterId) {
        JourneyParticipant req = participantRepository.findByJourneyIdAndUserId(journeyId, requesterId).orElseThrow(() -> new BadRequestException("Không tham gia"));
        if (req.getRole() != JourneyRole.OWNER) throw new BadRequestException("Chỉ chủ mới được kick.");
        JourneyParticipant vic = participantRepository.findByJourneyIdAndUserId(journeyId, memberId).orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại"));
        if (vic.getRole() == JourneyRole.OWNER) throw new BadRequestException("Không thể kick chính mình.");
        participantRepository.delete(vic);
    }

    @Override
    @Transactional
    public void transferOwnership(String journeyId, String currentOwnerId, String newOwnerId) {
        JourneyParticipant owner = participantRepository.findByJourneyIdAndUserId(journeyId, currentOwnerId).orElseThrow(() -> new BadRequestException("Lỗi xác thực"));
        if (owner.getRole() != JourneyRole.OWNER) throw new BadRequestException("Không phải chủ.");
        JourneyParticipant newOwner = participantRepository.findByJourneyIdAndUserId(journeyId, newOwnerId).orElseThrow(() -> new BadRequestException("Người nhận không trong nhóm."));
        owner.setRole(JourneyRole.MEMBER);
        newOwner.setRole(JourneyRole.OWNER);
        participantRepository.save(owner);
        participantRepository.save(newOwner);
        Journey j = getJourneyEntity(journeyId);
        j.setCreator(newOwner.getUser());
        journeyRepository.save(j);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JourneyParticipantResponse> getJourneyParticipants(String journeyId) {
        return participantRepository.findAllByJourneyId(journeyId).stream().map(p -> {
            User u = p.getUser();
            UserSummaryResponse uDto = (u != null) ? UserSummaryResponse.builder().id(u.getId()).fullname(u.getFullname()).avatarUrl(u.getAvatarUrl()).handle(u.getHandle()).build() : null;
            return JourneyParticipantResponse.builder().id(p.getId()).user(uDto).role(p.getRole().name()).joinedAt(p.getJoinedAt()).currentStreak(p.getCurrentStreak()).totalCheckins(p.getTotalCheckins()).lastCheckinAt(p.getLastCheckinAt()).build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteJourney(String journeyId, String userId) {
        JourneyParticipant p = participantRepository.findByJourneyIdAndUserId(journeyId, userId).orElseThrow(() -> new BadRequestException("Không tham gia"));
        if (p.getRole() != JourneyRole.OWNER) throw new BadRequestException("Chỉ chủ mới được xóa.");
        journeyRepository.deleteById(journeyId);
    }

    @Override
    public Journey getJourneyEntity(String journeyId) {
        return journeyRepository.findById(journeyId).orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JourneyRequestResponse> getPendingRequests(String journeyId, String userId) {
        JourneyParticipant req = participantRepository.findByJourneyIdAndUserId(journeyId, userId).orElseThrow(() -> new BadRequestException("Không tham gia"));
        if (req.getRole() != JourneyRole.OWNER) throw new BadRequestException("Chỉ chủ mới xem được.");
        return journeyRequestRepository.findAllByJourneyIdAndStatus(journeyId, RequestStatus.PENDING).stream().map(r -> JourneyRequestResponse.builder().id(r.getId()).userId(r.getUser().getId()).fullname(r.getUser().getFullname()).avatarUrl(r.getUser().getAvatarUrl()).handle(r.getUser().getHandle()).requestedAt(r.getCreatedAt()).status(r.getStatus()).build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveRequest(String journeyId, String requestId, String ownerId) {
        JourneyParticipant owner = participantRepository.findByJourneyIdAndUserId(journeyId, ownerId).orElseThrow(() -> new BadRequestException("Không thuộc nhóm"));
        if (owner.getRole() != JourneyRole.OWNER) throw new BadRequestException("Chỉ chủ mới được duyệt.");
        JourneyRequest req = journeyRequestRepository.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("Yêu cầu k tồn tại"));
        if (req.getStatus() != RequestStatus.PENDING) throw new BadRequestException("Đã xử lý.");
        
        User u = req.getUser();
        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, u.getId())) {
            participantRepository.save(JourneyParticipant.builder().journey(req.getJourney()).user(u).role(JourneyRole.MEMBER).joinedAt(LocalDateTime.now()).build());
            eventPublisher.publishEvent(new JourneyJoinedEvent(req.getJourney(), u));
        }
        req.setStatus(RequestStatus.ACCEPTED);
        journeyRequestRepository.save(req);
    }

    @Override
    @Transactional
    public void rejectRequest(String journeyId, String requestId, String ownerId) {
        JourneyParticipant owner = participantRepository.findByJourneyIdAndUserId(journeyId, ownerId).orElseThrow(() -> new BadRequestException("Không thuộc nhóm"));
        if (owner.getRole() != JourneyRole.OWNER) throw new BadRequestException("Chỉ chủ mới được từ chối.");
        JourneyRequest req = journeyRequestRepository.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("Yêu cầu k tồn tại"));
        req.setStatus(RequestStatus.REJECTED);
        journeyRequestRepository.save(req);
    }

    private JourneyStatus determineStatus(LocalDate startDate) {
        if (LocalDate.now().isBefore(startDate)) return JourneyStatus.UPCOMING;
        return JourneyStatus.ONGOING;
    }

    private JourneyResponse mapToResponse(Journey journey, JourneyParticipant currentParticipant, String overrideRole) {
        long totalMembers = participantRepository.countByJourneyId(journey.getId());
        JourneyResponse.CurrentUserStatus userStatus = null;
        if (currentParticipant != null) {
            boolean checkedInToday = currentParticipant.getLastCheckinAt() != null && currentParticipant.getLastCheckinAt().toLocalDate().isEqual(LocalDate.now());
            userStatus = JourneyResponse.CurrentUserStatus.builder().role(currentParticipant.getRole().name()).currentStreak(currentParticipant.getCurrentStreak()).totalCheckins(currentParticipant.getTotalCheckins()).hasCheckedInToday(checkedInToday).build();
        } else if (overrideRole != null) {
            userStatus = JourneyResponse.CurrentUserStatus.builder().role(overrideRole).currentStreak(0).totalCheckins(0).hasCheckedInToday(false).build();
        }
        String creatorId = (journey.getCreator() != null) ? String.valueOf(journey.getCreator().getId()) : null;
        return JourneyResponse.builder().id(journey.getId()).name(journey.getName()).description(journey.getDescription()).startDate(journey.getStartDate()).endDate(journey.getEndDate()).visibility(journey.getVisibility()).status(journey.getStatus()).inviteCode(journey.getInviteCode()).creatorId(creatorId).participantCount((int) totalMembers).currentUserStatus(userStatus).requireApproval(journey.isRequireApproval()).build();
    }
}