package com.mindrevol.backend.modules.journey.service;

import com.mindrevol.backend.modules.journey.dto.request.*;
import com.mindrevol.backend.modules.journey.dto.response.*;
import java.util.List;
import java.util.UUID;

public interface JourneyService {
    // Đổi User -> Long userId
    JourneyResponse createJourney(CreateJourneyRequest request, Long userId);
    
    JourneyResponse joinJourney(JoinJourneyRequest request, Long userId);
    
    List<JourneyResponse> getMyJourneys(Long userId);
    
    void leaveJourney(UUID journeyId, Long userId);
    
    JourneyResponse updateJourneySettings(UUID journeyId, UpdateJourneySettingsRequest request, Long userId);
    
    void kickMember(UUID journeyId, Long memberId, Long requesterId);
    
    JourneyWidgetResponse getWidgetInfo(UUID journeyId, Long userId);
    
    // Các hàm này cần User admin để check role, ta cũng truyền ID xuống để Service tự lấy User
    void approveJoinRequest(UUID requestId, Long adminId);
    
    void rejectJoinRequest(UUID requestId, Long adminId);
    
    List<JourneyResponse> getDiscoveryTemplates();
    
    JourneyResponse forkJourney(UUID templateId, Long userId);
    
    void nudgeMember(UUID journeyId, Long memberId, Long requesterId);
    
    // Hàm này chỉ đọc dữ liệu, truyền ID là đủ
    List<RoadmapStatusResponse> getJourneyRoadmap(UUID journeyId, Long userId);
    
    void transferOwnership(UUID journeyId, Long currentOwnerId, Long newOwnerId);
    
    List<JourneyParticipantResponse> getJourneyParticipants(UUID journeyId);
    
    void deleteJourney(UUID journeyId, Long userId);
}