package com.mindrevol.backend.modules.journey.service;

import com.mindrevol.backend.modules.journey.dto.request.*;
import com.mindrevol.backend.modules.journey.dto.response.*;
import com.mindrevol.backend.modules.user.entity.User;

import java.util.List;
import java.util.UUID;

public interface JourneyService {
    JourneyResponse createJourney(CreateJourneyRequest request, User currentUser);
    JourneyResponse joinJourney(JoinJourneyRequest request, User currentUser);
    
    void leaveJourney(UUID journeyId, User currentUser);
    JourneyResponse updateJourneySettings(UUID journeyId, UpdateJourneySettingsRequest request, User currentUser);

    List<JourneyResponse> getMyJourneys(User currentUser);
    void kickMember(UUID journeyId, Long memberId, User currentUser);
    List<RoadmapStatusResponse> getJourneyRoadmap(UUID journeyId, Long currentUserId);
    JourneyWidgetResponse getWidgetInfo(UUID journeyId, Long userId);
    void approveJoinRequest(UUID requestId, User admin);
    void rejectJoinRequest(UUID requestId, User admin);

    // Template Discovery
    List<JourneyResponse> getDiscoveryTemplates();
    JourneyResponse forkJourney(UUID templateId, User currentUser);

    // --- MỚI: Nudge (Chọc ghẹo) ---
    void nudgeMember(UUID journeyId, Long memberId, User currentUser);
}