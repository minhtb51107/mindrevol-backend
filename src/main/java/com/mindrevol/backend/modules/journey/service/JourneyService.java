package com.mindrevol.backend.modules.journey.service;

import com.mindrevol.backend.modules.journey.dto.request.CreateJourneyRequest;
import com.mindrevol.backend.modules.journey.dto.response.*;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;

import java.util.List;

public interface JourneyService {
    JourneyResponse createJourney(CreateJourneyRequest request, String userId);
    JourneyResponse joinJourney(String inviteCode, String userId);
    JourneyResponse getJourneyDetail(String userId, String journeyId);
    List<JourneyResponse> getMyJourneys(String userId);
    void leaveJourney(String journeyId, String userId);
    JourneyResponse updateJourney(String journeyId, CreateJourneyRequest request, String userId);
    void kickMember(String journeyId, String memberId, String requesterId);
    void transferOwnership(String journeyId, String currentOwnerId, String newOwnerId);
    List<JourneyParticipantResponse> getJourneyParticipants(String journeyId);
    void deleteJourney(String journeyId, String userId);
    Journey getJourneyEntity(String journeyId);
    List<JourneyRequestResponse> getPendingRequests(String journeyId, String userId);
    void approveRequest(String journeyId, String requestId, String ownerId);
    void rejectRequest(String journeyId, String requestId, String ownerId);
    
    // --- Method cho Profile ---
    List<UserActiveJourneyResponse> getUserActiveJourneys(String userId);
    
    // [MỚI] Thêm hàm này để lấy hành trình đã kết thúc
    List<UserActiveJourneyResponse> getUserFinishedJourneys(String userId);

    JourneyAlertResponse getJourneyAlerts(String userId);
    List<UserSummaryResponse> getInvitableFriends(String journeyId, String userId);
}