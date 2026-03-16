package com.mindrevol.backend.modules.box.service.impl;

import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.backend.modules.box.dto.request.UpdateBoxRequest;
import com.mindrevol.backend.modules.box.dto.response.BoxMemberResponse;
import com.mindrevol.backend.modules.box.dto.response.BoxResponse;
import com.mindrevol.backend.modules.box.entity.Box;
import com.mindrevol.backend.modules.box.entity.BoxInvitation;
import com.mindrevol.backend.modules.box.entity.BoxMember;
import com.mindrevol.backend.modules.box.entity.BoxRole;
import com.mindrevol.backend.modules.box.event.BoxMemberInvitedEvent;
import com.mindrevol.backend.modules.box.mapper.BoxMapper;
import com.mindrevol.backend.modules.box.repository.BoxInvitationRepository;
import com.mindrevol.backend.modules.box.repository.BoxMemberRepository;
import com.mindrevol.backend.modules.box.repository.BoxRepository;
import com.mindrevol.backend.modules.box.service.BoxService;
import com.mindrevol.backend.modules.chat.service.ChatService; 
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.journey.entity.JourneyInvitationStatus;
import com.mindrevol.backend.modules.journey.mapper.JourneyMapper;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoxServiceImpl implements BoxService {

    private final BoxRepository boxRepository;
    private final BoxMemberRepository boxMemberRepository;
    private final UserRepository userRepository;
    private final BoxMapper boxMapper;
    
    private final JourneyRepository journeyRepository;
    private final JourneyMapper journeyMapper;
    private final CheckinRepository checkinRepository;
    
    private final ApplicationEventPublisher eventPublisher;
    private final ChatService chatService;

    // [THÊM MỚI] Inject Repository thư mời
    private final BoxInvitationRepository boxInvitationRepository;

    @Override
    @Transactional
    public BoxResponse createBox(CreateBoxRequest request, String userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Box box = boxMapper.toEntity(request);
        box.setOwner(owner);
        box.setIsArchived(false);
        box = boxRepository.save(box);

        BoxMember adminMember = BoxMember.builder().box(box).user(owner).role(BoxRole.ADMIN).build();
        boxMemberRepository.save(adminMember);

        chatService.createBoxConversation(box.getId(), box.getName(), userId);

        return boxMapper.toResponse(box, 1L);
    }

    @Override
    @Transactional(readOnly = true)
    public BoxResponse getBoxDetails(String boxId, String userId) {
        Box box = getBoxById(boxId);
        checkMembership(boxId, userId);
        return boxMapper.toResponse(box, boxMemberRepository.countByBoxId(boxId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoxResponse> getMyBoxes(String userId, Pageable pageable) {
        return boxRepository.findBoxesByUserId(userId, pageable)
                .map(box -> boxMapper.toResponse(box, boxMemberRepository.countByBoxId(box.getId())));
    }

    @Override
    @Transactional
    public BoxResponse updateBox(String boxId, UpdateBoxRequest request, String userId) {
        Box box = getBoxById(boxId);
        checkAdminRole(boxId, userId);
        
        // Map các trường cũ bình thường
        boxMapper.updateEntityFromRequest(request, box);
        
        // [QUAN TRỌNG] Ghi đè thủ công 2 trường vị trí để đảm bảo luôn được lưu
        if (request.getTextPosition() != null) {
            box.setTextPosition(request.getTextPosition());
        }
        if (request.getAvatarPosition() != null) {
            box.setAvatarPosition(request.getAvatarPosition());
        }
        // Ghi đè coverImage nếu có
        if (request.getCoverImage() != null) {
            box.setCoverImage(request.getCoverImage());
        }
        
        box = boxRepository.save(box);
        
        chatService.updateBoxConversationInfo(boxId, box.getName());
        
        return boxMapper.toResponse(box, boxMemberRepository.countByBoxId(boxId));
    }

    @Override
    @Transactional
    public void archiveBox(String boxId, String userId) {
        Box box = getBoxById(boxId);
        checkAdminRole(boxId, userId);
        box.setIsArchived(true);
        boxRepository.save(box);
    }

    @Override
    @Transactional 
    public void inviteMember(String boxId, String targetUserId, String requesterId) {
        Box box = getBoxById(boxId);
        checkMembership(boxId, requesterId);

        if (boxMemberRepository.existsByBoxIdAndUserId(boxId, targetUserId)) {
            throw new BadRequestException("Người dùng đã ở trong không gian này");
        }

        // [SỬA LỖI] Thay vì dùng Redis, ta kiểm tra và lưu vào Database
        if (boxInvitationRepository.existsByBoxIdAndInviteeIdAndStatus(boxId, targetUserId, JourneyInvitationStatus.PENDING)) {
            throw new BadRequestException("Người dùng này đã có lời mời đang chờ xử lý.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng cần mời"));
        User requesterUser = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Lỗi xác thực người mời"));

        BoxInvitation invitation = BoxInvitation.builder()
                .box(box)
                .inviter(requesterUser)
                .invitee(targetUser)
                .status(JourneyInvitationStatus.PENDING)
                .build();
        boxInvitationRepository.save(invitation);

        // Vẫn bắn Event để Notification Service (WebSocket) gửi thông báo đẩy
        eventPublisher.publishEvent(new BoxMemberInvitedEvent(box, requesterUser, targetUser));
    }

    @Override
    @Transactional
    public void acceptInvite(String boxId, String userId) {
        // [SỬA LỖI] Tìm lời mời trong Database thay vì Redis
        BoxInvitation invitation = boxInvitationRepository.findByBoxIdAndInviteeIdAndStatus(boxId, userId, JourneyInvitationStatus.PENDING)
                .orElseThrow(() -> new BadRequestException("Lời mời không tồn tại, đã bị từ chối hoặc đã hết hạn."));

        if (!boxMemberRepository.existsByBoxIdAndUserId(boxId, userId)) {
            Box box = invitation.getBox();
            User user = invitation.getInvitee();
            
            BoxMember newMember = BoxMember.builder()
                    .box(box).user(user).role(BoxRole.MEMBER).build();
            boxMemberRepository.save(newMember);
            
            chatService.addUserToBoxConversation(boxId, userId);
        }
        
        // Cập nhật trạng thái
        invitation.setStatus(JourneyInvitationStatus.ACCEPTED);
        boxInvitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public void rejectInvite(String boxId, String userId) {
        // [SỬA LỖI] Tìm lời mời trong DB và đổi trạng thái
        BoxInvitation invitation = boxInvitationRepository.findByBoxIdAndInviteeIdAndStatus(boxId, userId, JourneyInvitationStatus.PENDING)
                .orElseThrow(() -> new BadRequestException("Lời mời không tồn tại hoặc đã xử lý."));
        
        invitation.setStatus(JourneyInvitationStatus.REJECTED);
        boxInvitationRepository.save(invitation);
    }

    // ... (Các hàm addMember, removeMember, disbandBox, transferOwnership, getBoxMembers, getBoxJourneys giữ nguyên như bản trước) ...
    @Override
    @Transactional
    public void addMember(String boxId, String targetUserId, String requesterId) {
        Box box = getBoxById(boxId);
        User targetUser = userRepository.findById(targetUserId).orElseThrow();
        if (!boxMemberRepository.existsByBoxIdAndUserId(boxId, targetUserId)) {
            boxMemberRepository.save(BoxMember.builder().box(box).user(targetUser).role(BoxRole.MEMBER).build());
            chatService.addUserToBoxConversation(boxId, targetUserId);
        }
    }

    @Override
    @Transactional
    public void removeMember(String boxId, String targetUserId, String requesterId) {
        BoxMember targetMember = boxMemberRepository.findByBoxIdAndUserId(boxId, targetUserId)
                .orElseThrow(() -> new BadRequestException("Thành viên không tồn tại trong không gian này"));
        
        if (targetUserId.equals(requesterId)) {
            if (targetMember.getRole() == BoxRole.ADMIN) throw new BadRequestException("Chủ phòng không thể tự rời đi.");
            boxMemberRepository.delete(targetMember);
            chatService.removeUserFromBoxConversation(boxId, targetUserId);
            return;
        }
        
        checkAdminRole(boxId, requesterId);
        if (targetMember.getRole() == BoxRole.ADMIN) throw new BadRequestException("Không thể đuổi Chủ phòng.");
        boxMemberRepository.delete(targetMember);
        chatService.removeUserFromBoxConversation(boxId, targetUserId);
    }

    @Override
    @Transactional
    public void disbandBox(String boxId, String userId) {
        Box box = getBoxById(boxId);
        if (!box.getOwner().getId().equals(userId)) throw new BadRequestException("Chỉ chủ phòng mới có quyền");
        box.setDeletedAt(LocalDateTime.now());
        boxRepository.save(box);
    }

    @Override
    @Transactional
    public void transferOwnership(String boxId, String newOwnerId, String requesterId) {
        Box box = getBoxById(boxId);
        if (!box.getOwner().getId().equals(requesterId)) throw new BadRequestException("Chỉ chủ phòng mới có quyền chuyển nhượng");
        BoxMember newOwnerMember = boxMemberRepository.findByBoxIdAndUserId(boxId, newOwnerId).orElseThrow(() -> new BadRequestException("Người nhận không phải là thành viên"));
        BoxMember currentOwnerMember = boxMemberRepository.findByBoxIdAndUserId(boxId, requesterId).orElseThrow();
        User newOwnerUser = userRepository.findById(newOwnerId).orElseThrow();
        box.setOwner(newOwnerUser);
        newOwnerMember.setRole(BoxRole.ADMIN);
        currentOwnerMember.setRole(BoxRole.MEMBER); 
        boxRepository.save(box);
        boxMemberRepository.save(newOwnerMember);
        boxMemberRepository.save(currentOwnerMember);
    }

    @Override
    @Transactional(readOnly = true) 
    public Page<BoxMemberResponse> getBoxMembers(String boxId, String userId, Pageable pageable) {
        checkMembership(boxId, userId);
        return boxMemberRepository.findByBoxId(boxId, pageable)
                .map(member -> BoxMemberResponse.builder().userId(member.getUser().getId()).fullname(member.getUser().getFullname()).avatarUrl(member.getUser().getAvatarUrl()).role(member.getRole()).joinedAt(member.getJoinedAt()).build());
    }

    @Override
    @Transactional(readOnly = true) 
    public Page<JourneyResponse> getBoxJourneys(String boxId, String userId, Pageable pageable) {
        checkMembership(boxId, userId);
        return journeyRepository.findJourneysByBoxId(boxId, pageable)
                .map(journey -> {
                    JourneyResponse response = journeyMapper.toResponse(journey);
                    List<String> images = checkinRepository.findPreviewImagesByJourneyId(
                            journey.getId(), 
                            PageRequest.of(0, 31)
                    );
                    response.setPreviewImages(images);
                    return response;
                });    
    }

    private Box getBoxById(String boxId) {
        return boxRepository.findById(boxId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy không gian"));
    }

    private void checkMembership(String boxId, String userId) {
        if (!boxMemberRepository.existsByBoxIdAndUserId(boxId, userId)) {
            throw new BadRequestException("Bạn không có quyền truy cập không gian này");
        }
    }

    private void checkAdminRole(String boxId, String userId) {
        BoxMember member = boxMemberRepository.findByBoxIdAndUserId(boxId, userId).orElseThrow(() -> new BadRequestException("Bạn không ở trong không gian này"));
        if (member.getRole() != BoxRole.ADMIN) {
            throw new BadRequestException("Chỉ Quản trị viên mới có quyền thực hiện hành động này");
        }
    }
}