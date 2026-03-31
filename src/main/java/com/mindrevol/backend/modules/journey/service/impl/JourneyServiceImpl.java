package com.mindrevol.backend.modules.journey.service.impl;

import com.mindrevol.backend.common.constant.AppConstants;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.box.entity.Box;
import com.mindrevol.backend.modules.box.repository.BoxMemberRepository;
import com.mindrevol.backend.modules.box.repository.BoxRepository;
import com.mindrevol.backend.modules.box.entity.BoxMember;
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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    
    private final BoxRepository boxRepository;
    private final BoxMemberRepository boxMemberRepository; // Thêm để check quyền Box

    private LocalDate getTodayInUserTimezone(User user) {
        String tz = user.getTimezone() != null ? user.getTimezone() : "UTC";
        try {
            return LocalDate.now(ZoneId.of(tz));
        } catch (Exception e) {
            return LocalDate.now(ZoneId.of("UTC"));
        }
    }

    private boolean isUserInBox(String boxId, String userId) {
        if (boxId == null) return false;
        return boxMemberRepository.existsByBoxIdAndUserId(boxId, userId);
    }

    private boolean hasAccessToJourney(Journey journey, String userId) {
        if (journey.getBox() != null && isUserInBox(journey.getBox().getId(), userId)) {
            return true;
        }
        return participantRepository.existsByJourneyIdAndUserId(journey.getId(), userId);
    }

    @Override
    @Transactional
    public void toggleProfileVisibility(String journeyId, String userId) {
        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journeyId, userId)
                .orElseThrow(() -> new BadRequestException("Cài đặt này chỉ dành cho Khách (Guest) tham gia trực tiếp. Nếu bạn là thành viên Box, hãy điều chỉnh quyền riêng tư trong Box."));
        participant.setProfileVisible(!participant.isProfileVisible());
        participantRepository.save(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserActiveJourneyResponse> getUserActiveJourneys(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        LocalDate today = getTodayInUserTimezone(user);
        
        List<Journey> activeJourneys = journeyRepository.findActiveJourneysByUserIdWithMembers(userId, today);
        return activeJourneys.stream().map(journey -> {
            JourneyParticipant p = participantRepository.findByJourneyIdAndUserId(journey.getId(), userId).orElse(null);
            return mapSingleJourneyToResponse(journey, p, userId);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserActiveJourneyResponse> getUserPublicJourneys(String targetUserId, String currentUserId) {
        User targetUser = userRepository.findById(targetUserId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!targetUserId.equals(currentUserId)) {
            List<Friendship> friends = friendshipRepository.findAllAcceptedFriendsList(currentUserId);
            boolean isFriend = (friends != null) && friends.stream().anyMatch(f -> f.getFriend(currentUserId).getId().equals(targetUserId));
            if (!isFriend) return new ArrayList<>(); 
        }

        List<Journey> publicJourneys = journeyRepository.findPublicJourneysByUserId(targetUserId);
        return publicJourneys.stream().map(journey -> {
            JourneyParticipant p = participantRepository.findByJourneyIdAndUserId(journey.getId(), targetUserId).orElse(null);
            return mapSingleJourneyToResponse(journey, p, currentUserId);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserActiveJourneyResponse> getUserPrivateJourneys(String targetUserId, String currentUserId) {
        if (!targetUserId.equals(currentUserId)) {
            throw new BadRequestException("Bạn không có quyền xem hành trình riêng tư của người khác.");
        }
        List<Journey> privateJourneys = journeyRepository.findPrivateJourneysByUserId(targetUserId);
        return privateJourneys.stream().map(journey -> {
            JourneyParticipant p = participantRepository.findByJourneyIdAndUserId(journey.getId(), targetUserId).orElse(null);
            return mapSingleJourneyToResponse(journey, p, currentUserId);
        }).collect(Collectors.toList());
    }

    private UserActiveJourneyResponse mapSingleJourneyToResponse(Journey journey, JourneyParticipant targetParticipant, String currentUserId) {
        boolean isViewerMember = currentUserId != null && hasAccessToJourney(journey, currentUserId);

        List<Checkin> journeyCheckins = checkinRepository.findVisibleCheckinsByJourneyId(journey.getId(), isViewerMember);
        List<CheckinResponse> checkinResponses = journeyCheckins.stream().map(checkinMapper::toResponse).collect(Collectors.toList());

        List<JourneyParticipant> members = journey.getParticipants();
        if (members == null) members = participantRepository.findAllByJourneyId(journey.getId());
        
        if (!isViewerMember) members = members.stream().filter(JourneyParticipant::isProfileVisible).collect(Collectors.toList());

        // Lấy số lượng từ Box + Guest
        long totalBoxMembers = journey.getBox() != null ? boxMemberRepository.countByBoxId(journey.getBox().getId()) : 0;
        long totalGuests = participantRepository.countByJourneyId(journey.getId());
        long totalMembers = totalBoxMembers + totalGuests;

        List<String> memberAvatars = members.stream().limit(3).map(mp -> mp.getUser().getAvatarUrl()).collect(Collectors.toList());

        boolean hasNewUpdates = false;
        String latestCheckinImage = null;
        if (!journeyCheckins.isEmpty()) {
            Checkin latestCheckin = journeyCheckins.get(0); 
            if (currentUserId != null && !latestCheckin.getUser().getId().equals(currentUserId)) hasNewUpdates = true;
            for (Checkin c : journeyCheckins) {
                if (c.getImageUrl() != null && !c.getImageUrl().isEmpty()) {
                    latestCheckinImage = c.getImageUrl();
                    break;
                }
            }
        }

        String finalThumbnail = journey.getThumbnailUrl();
        if ((finalThumbnail == null || finalThumbnail.isEmpty()) && latestCheckinImage != null) finalThumbnail = latestCheckinImage;

        long daysRemaining = 0;
        if (journey.getEndDate() != null) {
            LocalDate now = LocalDate.now();
            if (journey.getEndDate().isAfter(now) || journey.getEndDate().isEqual(now)) {
                daysRemaining = ChronoUnit.DAYS.between(now, journey.getEndDate());
            }
        }

        String themeString = (journey.getTheme() != null) ? journey.getTheme().name() : JourneyTheme.OTHER.name();
        int totalCheckins = targetParticipant != null ? targetParticipant.getTotalCheckins() : 0;
        boolean isProfileVisible = targetParticipant != null ? targetParticipant.isProfileVisible() : true;

        return UserActiveJourneyResponse.builder()
                .id(journey.getId())
                .name(journey.getName())
                .description(journey.getDescription())
                .status(journey.getStatus().name())
                .visibility(journey.getVisibility().name())
                .startDate(journey.getStartDate())
                .endDate(journey.getEndDate())
                .thumbnailUrl(finalThumbnail)
                .theme(themeString)
                .themeColor(journey.getThemeColor())
                .avatar(journey.getAvatar())
                .memberAvatars(memberAvatars)
                .totalMembers((int)totalMembers)
                .daysRemaining(daysRemaining)
                .totalCheckins(totalCheckins)
                .checkins(checkinResponses)
                .hasNewUpdates(hasNewUpdates)
                .isProfileVisible(isProfileVisible)
                .build();
    }

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
        Journey journey = getJourneyEntity(journeyId);
        List<JourneyParticipant> participants = participantRepository.findAllByJourneyId(journeyId);
        Set<String> participantIds = participants.stream().map(p -> p.getUser().getId()).collect(Collectors.toSet());
        
        // Nếu journey trong Box, không mời những người đã ở trong Box
        if (journey.getBox() != null) {
            List<BoxMember> boxMembers = boxMemberRepository.findByBoxId(journey.getBox().getId());
            participantIds.addAll(boxMembers.stream().map(m -> m.getUser().getId()).collect(Collectors.toSet()));
        }

        List<Friendship> friendships = friendshipRepository.findAllAcceptedFriendsList(userId);
        return friendships.stream().map(f -> f.getFriend(userId)).filter(friend -> !participantIds.contains(friend.getId()))
                .map(friend -> UserSummaryResponse.builder().id(friend.getId()).fullname(friend.getFullname()).avatarUrl(friend.getAvatarUrl()).handle(friend.getHandle()).build()).collect(Collectors.toList());
    }

    private User getUserEntity(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public JourneyResponse createJourney(CreateJourneyRequest request, String userId) {
        User currentUser = getUserEntity(userId);
        LocalDate today = getTodayInUserTimezone(currentUser);
        long activeCount = participantRepository.countActiveByUserId(userId, today); 
        int limit = currentUser.isPremium() ? AppConstants.MAX_ACTIVE_JOURNEYS_GOLD : AppConstants.MAX_ACTIVE_JOURNEYS_FREE;
        
        if (activeCount >= limit) throw new BadRequestException("Bạn đã đạt giới hạn " + limit + " hành trình đang hoạt động.");
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) throw new BadRequestException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");

        JourneyTheme theme = (request.getTheme() != null) ? request.getTheme() : JourneyTheme.OTHER;

        Journey journey = Journey.builder().name(request.getName()).description(request.getDescription()).startDate(request.getStartDate()).endDate(request.getEndDate()).visibility(request.getVisibility()).requireApproval(true).status(determineStatus(request.getStartDate())).inviteCode(RandomStringUtils.randomAlphanumeric(8).toUpperCase()).creator(currentUser).theme(theme).thumbnailUrl(request.getThumbnailUrl()).themeColor(request.getThemeColor() != null ? request.getThemeColor() : "#3b82f6").avatar(request.getAvatar() != null ? request.getAvatar() : "🚀").build();
        
        if (request.getBoxId() != null && !request.getBoxId().trim().isEmpty()) {
            Box box = boxRepository.findById(request.getBoxId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Không gian đã chọn."));
            if (!isUserInBox(box.getId(), userId)) throw new BadRequestException("Bạn không thuộc Không gian này.");
            journey.setBox(box);
        }
        
        journey = journeyRepository.save(journey);
        
        // Chỉ lưu vào JourneyParticipant nếu hành trình này độc lập (không thuộc Box)
        if (journey.getBox() == null) {
            JourneyParticipant owner = JourneyParticipant.builder().journey(journey).user(currentUser).role(JourneyRole.OWNER).joinedAt(LocalDateTime.now()).isProfileVisible(true).build();
            participantRepository.save(owner);
        }
        
        eventPublisher.publishEvent(new JourneyCreatedEvent(journey, currentUser));
        
        JourneyParticipant pInfo = journey.getBox() == null ? participantRepository.findByJourneyIdAndUserId(journey.getId(), userId).orElse(null) : null;
        return mapToResponse(journey, pInfo, journey.getBox() != null, null);
    }

    @Override
    @Transactional 
    public JourneyResponse joinJourney(String inviteCode, String userId) {
        User currentUser = getUserEntity(userId);
        Journey journeyInfo = journeyRepository.findByInviteCode(inviteCode).orElseThrow(() -> new ResourceNotFoundException("Mã mời không hợp lệ"));
        Journey journey = journeyRepository.findByIdWithLock(journeyInfo.getId()).orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        if (hasAccessToJourney(journey, userId)) return getJourneyDetail(userId, journey.getId());

        LocalDate today = getTodayInUserTimezone(currentUser);
        if (journey.getEndDate() != null && journey.getEndDate().isBefore(today)) throw new BadRequestException("Hành trình đã kết thúc.");

        if (journey.isRequireApproval()) {
            if (journeyRequestRepository.existsByJourneyIdAndUserIdAndStatus(journey.getId(), userId, RequestStatus.PENDING)) return mapToResponse(journey, null, false, "PENDING");
            journeyRequestRepository.save(JourneyRequest.builder().journey(journey).user(currentUser).status(RequestStatus.PENDING).build());
            return mapToResponse(journey, null, false, "PENDING");
        }

        validateJourneyCapacity(journey);

        // Tham gia qua mã mời => Trở thành GUEST
        JourneyParticipant member = JourneyParticipant.builder().journey(journey).user(currentUser).role(JourneyRole.GUEST).joinedAt(LocalDateTime.now()).isProfileVisible(true).build();
        participantRepository.save(member);
        eventPublisher.publishEvent(new JourneyJoinedEvent(journey, currentUser));
        return mapToResponse(journey, member, false, null);
    }

    @Override
    public JourneyResponse getJourneyDetail(String userId, String journeyId) {
        Journey journey = getJourneyEntity(journeyId);
        boolean isBoxMember = journey.getBox() != null && isUserInBox(journey.getBox().getId(), userId);
        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journeyId, userId).orElse(null);
        
        String pendingStatus = null;
        if (!isBoxMember && participant == null && journey.isRequireApproval()) {
             if(journeyRequestRepository.existsByJourneyIdAndUserIdAndStatus(journeyId, userId, RequestStatus.PENDING)) pendingStatus = "PENDING";
        }
        
        if (journey.getVisibility() == JourneyVisibility.PRIVATE && participant == null && !isBoxMember && pendingStatus == null) {
            throw new BadRequestException("Đây là hành trình riêng tư.");
        }
        
        return mapToResponse(journey, participant, isBoxMember, pendingStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JourneyResponse> getMyJourneys(String userId) {
        // Gom cả Journey của Box + Journey mà User là Guest
        List<Journey> visibleJourneys = journeyRepository.findAllJourneysForUser(userId);
        List<JourneyResponse> responses = visibleJourneys.stream().map(j -> {
            boolean isBoxMember = j.getBox() != null && isUserInBox(j.getBox().getId(), userId);
            JourneyParticipant p = participantRepository.findByJourneyIdAndUserId(j.getId(), userId).orElse(null);
            return mapToResponse(j, p, isBoxMember, null);
        }).collect(Collectors.toList());

        List<JourneyResponse> pending = journeyRequestRepository.findAllByUserIdAndStatus(userId, RequestStatus.PENDING).stream()
            .map(req -> mapToResponse(req.getJourney(), null, false, "PENDING")).collect(Collectors.toList());
        
        responses.addAll(pending);
        return responses;
    }

    @Override
    @Transactional
    public void leaveJourney(String journeyId, String userId) {
        Journey journey = getJourneyEntity(journeyId);
        if (journey.getBox() != null && isUserInBox(journey.getBox().getId(), userId)) {
            throw new BadRequestException("Bạn là thành viên Box nên không thể rời từng Hành trình lẻ. Vui lòng rời Box.");
        }
        JourneyParticipant p = participantRepository.findByJourneyIdAndUserId(journeyId, userId).orElseThrow(() -> new BadRequestException("Bạn không tham gia (với tư cách Khách)"));
        if (p.getRole() == JourneyRole.OWNER) throw new BadRequestException("Chủ hành trình không thể rời đi.");
        participantRepository.delete(p);
    }

    @Override
    @Transactional
    public JourneyResponse updateJourney(String journeyId, CreateJourneyRequest request, String userId) {
        Journey journey = getJourneyEntity(journeyId);
        
        // Chỉ Creator hoặc Owner mới được sửa
        boolean isCreator = journey.getCreator().getId().equals(userId);
        JourneyParticipant p = participantRepository.findByJourneyIdAndUserId(journeyId, userId).orElse(null);
        boolean isOwner = p != null && p.getRole() == JourneyRole.OWNER;

        if (!isCreator && !isOwner) throw new BadRequestException("Chỉ người tạo hoặc chủ hành trình mới được sửa.");
        
        journey.setName(request.getName());
        journey.setDescription(request.getDescription());
        journey.setVisibility(request.getVisibility());
        if (request.getTheme() != null) journey.setTheme(request.getTheme());
        if (request.getThumbnailUrl() != null) journey.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getThemeColor() != null) journey.setThemeColor(request.getThemeColor());
        if (request.getAvatar() != null) journey.setAvatar(request.getAvatar());

        return mapToResponse(journeyRepository.save(journey), p, journey.getBox() != null && isUserInBox(journey.getBox().getId(), userId), null);
    }

    @Override
    @Transactional
    public void kickMember(String journeyId, String memberId, String requesterId) {
        Journey journey = getJourneyEntity(journeyId);
        boolean isCreator = journey.getCreator().getId().equals(requesterId);
        if (!isCreator) throw new BadRequestException("Chỉ người tạo mới được kick.");

        JourneyParticipant vic = participantRepository.findByJourneyIdAndUserId(journeyId, memberId).orElseThrow(() -> new ResourceNotFoundException("Thành viên không phải là Khách (Guest) trong hành trình này"));
        participantRepository.delete(vic);
    }

    @Override
    @Transactional
    public void transferOwnership(String journeyId, String currentOwnerId, String newOwnerId) {
        // Tính năng này có thể bỏ qua nếu đã dùng chung quyền Box
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
        Journey journey = getJourneyEntity(journeyId);
        List<JourneyParticipantResponse> responses = new ArrayList<>();

        // 1. Thêm Box Members (Quyền kế thừa)
        if (journey.getBox() != null) {
            List<BoxMember> boxMembers = boxMemberRepository.findByBoxId(journey.getBox().getId());
            for (BoxMember bm : boxMembers) {
                User u = bm.getUser();
                UserSummaryResponse uDto = UserSummaryResponse.builder().id(u.getId()).fullname(u.getFullname()).avatarUrl(u.getAvatarUrl()).handle(u.getHandle()).build();
                responses.add(JourneyParticipantResponse.builder().id("BOX_" + bm.getId()).user(uDto).role("BOX_MEMBER").joinedAt(bm.getJoinedAt()).currentStreak(0).totalCheckins(0).build());
            }
        }

        // 2. Thêm Guests từ JourneyParticipant
        List<JourneyParticipant> participants = participantRepository.findAllByJourneyId(journeyId);
        for (JourneyParticipant p : participants) {
            User u = p.getUser();
            UserSummaryResponse uDto = UserSummaryResponse.builder().id(u.getId()).fullname(u.getFullname()).avatarUrl(u.getAvatarUrl()).handle(u.getHandle()).build();
            responses.add(JourneyParticipantResponse.builder().id(p.getId()).user(uDto).role(p.getRole().name()).joinedAt(p.getJoinedAt()).currentStreak(p.getCurrentStreak()).totalCheckins(p.getTotalCheckins()).lastCheckinAt(p.getLastCheckinAt()).build());
        }

        return responses;
    }

    @Override
    @Transactional
    public void deleteJourney(String journeyId, String userId) {
        Journey journey = getJourneyEntity(journeyId);
        if (!journey.getCreator().getId().equals(userId)) throw new BadRequestException("Chỉ người tạo mới được xóa.");
        journeyRepository.deleteById(journeyId);
    }

    @Override
    public Journey getJourneyEntity(String journeyId) {
        return journeyRepository.findById(journeyId).orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JourneyRequestResponse> getPendingRequests(String journeyId, String userId) {
        Journey journey = getJourneyEntity(journeyId);
        if (!journey.getCreator().getId().equals(userId)) throw new BadRequestException("Chỉ người tạo mới xem được.");
        return journeyRequestRepository.findAllByJourneyIdAndStatus(journeyId, RequestStatus.PENDING).stream().map(r -> JourneyRequestResponse.builder().id(r.getId()).userId(r.getUser().getId()).fullname(r.getUser().getFullname()).avatarUrl(r.getUser().getAvatarUrl()).handle(r.getUser().getHandle()).requestedAt(r.getCreatedAt()).status(r.getStatus()).build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveRequest(String journeyId, String requestId, String ownerId) {
        Journey journey = getJourneyEntity(journeyId);
        if (!journey.getCreator().getId().equals(ownerId)) throw new BadRequestException("Chỉ người tạo mới được duyệt.");
        
        JourneyRequest req = journeyRequestRepository.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("Yêu cầu k tồn tại"));
        if (req.getStatus() != RequestStatus.PENDING) throw new BadRequestException("Đã xử lý.");
        
        User u = req.getUser();
        if (!hasAccessToJourney(journey, u.getId())) {
            validateJourneyCapacity(journey);
            participantRepository.save(JourneyParticipant.builder().journey(journey).user(u).role(JourneyRole.GUEST).joinedAt(LocalDateTime.now()).build());
            eventPublisher.publishEvent(new JourneyJoinedEvent(journey, u));
        }
        req.setStatus(RequestStatus.ACCEPTED);
        journeyRequestRepository.save(req);
    }

    @Override
    @Transactional
    public void rejectRequest(String journeyId, String requestId, String ownerId) {
        Journey journey = getJourneyEntity(journeyId);
        if (!journey.getCreator().getId().equals(ownerId)) throw new BadRequestException("Chỉ người tạo mới được từ chối.");
        JourneyRequest req = journeyRequestRepository.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("Yêu cầu k tồn tại"));
        req.setStatus(RequestStatus.REJECTED);
        journeyRequestRepository.save(req);
    }

    private JourneyStatus determineStatus(LocalDate startDate) {
        if (LocalDate.now().isBefore(startDate)) return JourneyStatus.UPCOMING;
        return JourneyStatus.ONGOING;
    }

    private JourneyResponse mapToResponse(Journey journey, JourneyParticipant currentParticipant, boolean isBoxMember, String overrideRole) {
        long totalBoxMembers = journey.getBox() != null ? boxMemberRepository.countByBoxId(journey.getBox().getId()) : 0;
        long totalGuests = participantRepository.countByJourneyId(journey.getId());
        long totalMembers = totalBoxMembers + totalGuests;

        JourneyResponse.CurrentUserStatus userStatus = null;
        
        if (isBoxMember) {
            userStatus = JourneyResponse.CurrentUserStatus.builder().role("BOX_MEMBER").currentStreak(0).totalCheckins(0).hasCheckedInToday(false).build();
        } else if (currentParticipant != null) {
            boolean checkedInToday = currentParticipant.getLastCheckinAt() != null && currentParticipant.getLastCheckinAt().toLocalDate().isEqual(LocalDate.now());
            userStatus = JourneyResponse.CurrentUserStatus.builder().role(currentParticipant.getRole().name()).currentStreak(currentParticipant.getCurrentStreak()).totalCheckins(currentParticipant.getTotalCheckins()).hasCheckedInToday(checkedInToday).build();
        } else if (overrideRole != null) {
            userStatus = JourneyResponse.CurrentUserStatus.builder().role(overrideRole).currentStreak(0).totalCheckins(0).hasCheckedInToday(false).build();
        }

        String creatorId = (journey.getCreator() != null) ? String.valueOf(journey.getCreator().getId()) : null;
        String boxId = (journey.getBox() != null) ? journey.getBox().getId() : null;

        return JourneyResponse.builder()
                .id(journey.getId())
                .name(journey.getName())
                .description(journey.getDescription())
                .startDate(journey.getStartDate())
                .endDate(journey.getEndDate())
                .visibility(journey.getVisibility())
                .status(journey.getStatus())
                .inviteCode(journey.getInviteCode())
                .creatorId(creatorId)
                .participantCount((int) totalMembers)
                .currentUserStatus(userStatus)
                .requireApproval(journey.isRequireApproval())
                .boxId(boxId) 
                .themeColor(journey.getThemeColor())
                .avatar(journey.getAvatar())
                .build();
    }

    private void validateJourneyCapacity(Journey journey) {
        long currentCount = participantRepository.countByJourneyId(journey.getId());
        User creator = journey.getCreator();
        int limit = AppConstants.MAX_PARTICIPANTS_FREE;
        if (creator.isPremium()) limit = AppConstants.MAX_PARTICIPANTS_GOLD;
        if (currentCount >= limit) {
            throw new BadRequestException(String.format("Hành trình đã đạt giới hạn Khách tham gia (%d/%d).", currentCount, limit));
        }
    }
}