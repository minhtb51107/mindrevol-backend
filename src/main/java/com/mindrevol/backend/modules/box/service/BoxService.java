package com.mindrevol.backend.modules.box.service;

import com.mindrevol.backend.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.backend.modules.box.dto.request.UpdateBoxRequest;
import com.mindrevol.backend.modules.box.dto.response.BoxMemberResponse;
import com.mindrevol.backend.modules.box.dto.response.BoxResponse;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoxService {
    BoxResponse createBox(CreateBoxRequest request, String userId);
    BoxResponse getBoxDetails(String boxId, String userId);
    Page<BoxResponse> getMyBoxes(String userId, Pageable pageable);
    
    BoxResponse updateBox(String boxId, UpdateBoxRequest request, String userId);
    void archiveBox(String boxId, String userId);
    
    void addMember(String boxId, String targetUserId, String requesterId);
    void removeMember(String boxId, String targetUserId, String requesterId);
    
    void disbandBox(String boxId, String userId);
    void transferOwnership(String boxId, String newOwnerId, String requesterId);
    
    Page<BoxMemberResponse> getBoxMembers(String boxId, String userId, Pageable pageable);
    Page<JourneyResponse> getBoxJourneys(String boxId, String userId, Pageable pageable);
    void inviteMember(String boxId, String targetUserId, String requesterId);
    void acceptInvite(String boxId, String userId);
    void rejectInvite(String boxId, String userId);
}