package com.mindrevol.backend.modules.user.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.user.dto.response.FriendshipResponse;
import com.mindrevol.backend.modules.user.entity.Friendship;
import com.mindrevol.backend.modules.user.entity.FriendshipStatus;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.mapper.FriendshipMapper; // Import Mapper mới
import com.mindrevol.backend.modules.user.repository.FriendshipRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mindrevol.backend.modules.notification.entity.NotificationType; // Import
import com.mindrevol.backend.modules.notification.service.NotificationService;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final FriendshipMapper friendshipMapper; 
    private final NotificationService notificationService;

    @Override
    @Transactional
    public FriendshipResponse sendFriendRequest(Long requesterId, Long targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw new BadRequestException("Không thể tự kết bạn với chính mình");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Người gửi không tồn tại"));
        User addressee = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Người nhận không tồn tại"));

        // Kiểm tra xem đã có quan hệ nào chưa
        if (friendshipRepository.existsByUsers(requesterId, targetUserId)) {
            throw new BadRequestException("Đã tồn tại mối quan hệ hoặc lời mời giữa hai người");
        }

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .status(FriendshipStatus.PENDING)
                .build();

        Friendship saved = friendshipRepository.save(friendship);
        
        notificationService.sendAndSaveNotification(
                addressee.getId(),          // Người nhận: Người được mời
                requester.getId(),          // Người gửi: Mình
                NotificationType.FRIEND_REQUEST,
                "Lời mời kết bạn mới 👋",
                requester.getFullname() + " muốn kết bạn với bạn.",
                saved.getId().toString(),   // Reference ID là Friendship ID
                requester.getAvatarUrl()
        );

        return friendshipMapper.toResponse(saved, requesterId);
    }

    @Override
    @Transactional
    public FriendshipResponse acceptFriendRequest(Long userId, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Lời mời không tồn tại"));

        if (!friendship.getAddressee().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền chấp nhận lời mời này");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BadRequestException("Lời mời không còn hiệu lực");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        Friendship saved = friendshipRepository.save(friendship);

        User accepter = friendship.getAddressee(); // Là người đang thực hiện hành động này
        User requester = friendship.getRequester();

        notificationService.sendAndSaveNotification(
                requester.getId(),
                accepter.getId(),
                NotificationType.FRIEND_ACCEPTED,
                "Đã trở thành bạn bè 🤝",
                accepter.getFullname() + " đã chấp nhận lời mời kết bạn.",
                accepter.getId().toString(), // Bấm vào sẽ mở trang cá nhân người kia
                accepter.getAvatarUrl()
        );

        return friendshipMapper.toResponse(saved, userId);
    }

    @Override
    @Transactional
    public void declineFriendRequest(Long userId, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Lời mời không tồn tại"));

        if (!friendship.getAddressee().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền từ chối lời mời này");
        }

        // Xóa luôn bản ghi để họ có thể gửi lại sau này (hoặc set DECLINED tùy logic)
        friendshipRepository.delete(friendship);
    }

    @Override
    @Transactional
    public void removeFriendship(Long userId, Long targetUserId) {
        Friendship friendship = friendshipRepository.findByUsers(userId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mối quan hệ bạn bè"));

        // Cho phép xóa dù là requester hay addressee
        friendshipRepository.delete(friendship);
    }

    @Override
    public Page<FriendshipResponse> getMyFriends(Long userId, Pageable pageable) {
        return friendshipRepository.findAllAcceptedFriends(userId, pageable)
                .map(friendship -> friendshipMapper.toResponse(friendship, userId));
    }

    @Override
    public Page<FriendshipResponse> getIncomingRequests(Long userId, Pageable pageable) {
        return friendshipRepository.findIncomingRequests(userId, pageable)
                .map(friendship -> friendshipMapper.toResponse(friendship, userId));
    }

    @Override
    public Page<FriendshipResponse> getOutgoingRequests(Long userId, Pageable pageable) {
        return friendshipRepository.findOutgoingRequests(userId, pageable)
                .map(friendship -> friendshipMapper.toResponse(friendship, userId));
    }
}