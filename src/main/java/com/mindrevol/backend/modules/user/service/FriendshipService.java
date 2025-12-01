package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.user.dto.response.FriendshipResponse;
import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FriendshipService {
    
    // Gửi lời mời kết bạn
    FriendshipResponse sendFriendRequest(Long requesterId, Long targetUserId);

    // Chấp nhận lời mời
    FriendshipResponse acceptFriendRequest(Long userId, Long friendshipId);

    // Từ chối lời mời
    void declineFriendRequest(Long userId, Long friendshipId);

    // Hủy kết bạn hoặc Hủy lời mời đã gửi
    void removeFriendship(Long userId, Long targetUserId);

    // Lấy danh sách bạn bè
    Page<FriendshipResponse> getMyFriends(Long userId, Pageable pageable);

    // Lấy danh sách lời mời đang chờ tôi chấp nhận
    Page<FriendshipResponse> getIncomingRequests(Long userId, Pageable pageable);

    // Lấy danh sách lời mời tôi đã gửi đi
    Page<FriendshipResponse> getOutgoingRequests(Long userId, Pageable pageable);
}