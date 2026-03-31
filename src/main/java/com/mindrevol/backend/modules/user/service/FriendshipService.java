package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.user.dto.response.FriendshipResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service quản lý các kết nối bạn bè giữa các người dùng.
 */
public interface FriendshipService {
    
    // Gửi lời mời kết bạn
    FriendshipResponse sendFriendRequest(String requesterId, String targetUserId);

    // Chấp nhận lời mời kết bạn
    FriendshipResponse acceptFriendRequest(String userId, String friendshipId);

    // Từ chối lời mời kết bạn
    void declineFriendRequest(String currentUserId, String friendshipId);

    // Hủy kết bạn (Xóa mối quan hệ bạn bè)
    void removeFriendship(String currentUserId, String targetUserId);

    // Lấy danh sách bạn bè của bản thân
    Page<FriendshipResponse> getMyFriends(String currentUserId, Pageable pageable);

    // Lấy danh sách lời mời kết bạn gửi đến
    Page<FriendshipResponse> getIncomingRequests(String currentUserId, Pageable pageable);

    // Lấy danh sách lời mời kết bạn đã gửi đi
    Page<FriendshipResponse> getOutgoingRequests(String userId, Pageable pageable);

    // Lấy danh sách bạn bè của một người dùng khác (nếu họ công khai)
	Page<FriendshipResponse> getUserFriends(String userId, Pageable pageable);
}