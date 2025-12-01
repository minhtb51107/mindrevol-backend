package com.mindrevol.backend.modules.user.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.user.dto.response.FriendshipResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.Friendship;
import com.mindrevol.backend.modules.user.entity.FriendshipStatus;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.mapper.UserMapper;
import com.mindrevol.backend.modules.user.repository.FriendshipRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

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
        
        // TODO: Gửi Notification cho addressee tại đây ("requester.getFullname() muốn kết bạn")

        return toFriendshipResponse(saved, requesterId);
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

        // TODO: Gửi Notification cho requester ("addressee đã chấp nhận lời mời")

        return toFriendshipResponse(saved, userId);
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
                .map(friendship -> toFriendshipResponse(friendship, userId));
    }

    @Override
    public Page<FriendshipResponse> getIncomingRequests(Long userId, Pageable pageable) {
        return friendshipRepository.findIncomingRequests(userId, pageable)
                .map(friendship -> toFriendshipResponse(friendship, userId));
    }

    @Override
    public Page<FriendshipResponse> getOutgoingRequests(Long userId, Pageable pageable) {
        return friendshipRepository.findOutgoingRequests(userId, pageable)
                .map(friendship -> toFriendshipResponse(friendship, userId));
    }

    // Helper method để map sang DTO Response
    private FriendshipResponse toFriendshipResponse(Friendship friendship, Long currentUserId) {
        // Xác định ai là "bạn" trong mối quan hệ này để trả về thông tin người đó
        User friendUser = friendship.getFriend(currentUserId);
        
        // Dùng UserMapper có sẵn để map User entity sang UserSummaryResponse
        // Lưu ý: UserMapper phải có method toSummaryResponse, nếu chưa có thì dùng toProfileResponse tạm
        UserSummaryResponse friendSummary = UserSummaryResponse.builder()
                .id(friendUser.getId())
                .fullname(friendUser.getFullname())
                .handle(friendUser.getHandle())
                .avatarUrl(friendUser.getAvatarUrl())
                .build();

        return FriendshipResponse.builder()
                .id(friendship.getId())
                .friend(friendSummary)
                .status(friendship.getStatus())
                .isRequester(friendship.getRequester().getId().equals(currentUserId))
                .createdAt(friendship.getCreatedAt().toLocalDateTime())
                .build();
    }
}