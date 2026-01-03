package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.user.dto.response.FriendshipResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FriendshipService {
    
    // [UUID] Toàn bộ Long -> String
    FriendshipResponse sendFriendRequest(String requesterId, String targetUserId);

    FriendshipResponse acceptFriendRequest(String userId, String friendshipId);

    void declineFriendRequest(String currentUserId, String friendshipId);

    void removeFriendship(String currentUserId, String targetUserId);

    Page<FriendshipResponse> getMyFriends(String currentUserId, Pageable pageable);

    Page<FriendshipResponse> getIncomingRequests(String currentUserId, Pageable pageable);

    Page<FriendshipResponse> getOutgoingRequests(String userId, Pageable pageable);

	Page<FriendshipResponse> getUserFriends(String userId, Pageable pageable);
}