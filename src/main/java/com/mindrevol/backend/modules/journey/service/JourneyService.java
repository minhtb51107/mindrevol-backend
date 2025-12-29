package com.mindrevol.backend.modules.journey.service;

import com.mindrevol.backend.modules.journey.dto.request.*;
import com.mindrevol.backend.modules.journey.dto.response.*;
import com.mindrevol.backend.modules.journey.entity.Journey;

import java.util.List;

public interface JourneyService {
    JourneyResponse createJourney(CreateJourneyRequest request, Long userId);
    
    // [ĐỔI] Join bằng inviteCode (đơn giản hơn là request object)
    JourneyResponse joinJourney(String inviteCode, Long userId);
    
    List<JourneyResponse> getMyJourneys(Long userId);
    
    void leaveJourney(Long journeyId, Long userId);
    
    JourneyResponse updateJourney(Long journeyId, CreateJourneyRequest request, Long userId); // Update chung
    
    void kickMember(Long journeyId, Long memberId, Long requesterId);
    
    // Widget cho Home Screen
    // JourneyWidgetResponse getWidgetInfo(Long journeyId, Long userId); 
    // -> Tạm bỏ Widget phức tạp, dùng getMyJourneys là đủ info hiển thị rồi.
    
    // Quản lý Request (nếu để Private)
    // void approveJoinRequest(Long requestId, Long adminId);
    // void rejectJoinRequest(Long requestId, Long adminId);
    
    // Template
    // List<JourneyResponse> getDiscoveryTemplates();
    // JourneyResponse forkJourney(Long templateId, Long userId);
    
    // Tính năng vui vẻ
    // void nudgeMember(Long journeyId, Long memberId, Long requesterId);
    
    void transferOwnership(Long journeyId, Long currentOwnerId, Long newOwnerId);
    
    List<JourneyParticipantResponse> getJourneyParticipants(Long journeyId);
    
    void deleteJourney(Long journeyId, Long userId);

    // List<JourneyRequestResponse> getPendingRequests(Long journeyId, Long userId);

    // Helper cho nội bộ
    Journey getJourneyEntity(Long journeyId);

	JourneyResponse getJourneyDetail(Long userId, Long journeyId);
}