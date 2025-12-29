package com.mindrevol.backend.modules.journey.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.journey.dto.request.CreateJourneyRequest;
import com.mindrevol.backend.modules.journey.dto.response.JourneyParticipantResponse;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.journey.service.JourneyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/journeys")
@RequiredArgsConstructor
public class JourneyController {

    private final JourneyService journeyService;

    @PostMapping
    public ApiResponse<JourneyResponse> createJourney(@Valid @RequestBody CreateJourneyRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.createJourney(request, userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<JourneyResponse> getJourney(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        // Hàm này chưa có trong interface, bạn thêm vào interface: JourneyResponse getJourneyDetail(Long userId, Long journeyId);
        // Trong impl gọi hàm getJourneyDetail đã viết ở trên
        return ApiResponse.success(null); // TODO: Gọi service
    }

    @PostMapping("/join/{inviteCode}")
    public ApiResponse<JourneyResponse> joinJourney(@PathVariable String inviteCode) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.joinJourney(inviteCode, userId));
    }

    @GetMapping("/me")
    public ApiResponse<List<JourneyResponse>> getMyJourneys() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.getMyJourneys(userId));
    }

    @DeleteMapping("/{id}/leave")
    public ApiResponse<Void> leaveJourney(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        journeyService.leaveJourney(id, userId);
        return ApiResponse.success(null, "Đã rời hành trình");
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ApiResponse<Void> kickMember(@PathVariable Long id, @PathVariable Long memberId) {
        Long userId = SecurityUtils.getCurrentUserId();
        journeyService.kickMember(id, memberId, userId);
        return ApiResponse.success(null, "Đã mời thành viên ra khỏi nhóm");
    }

    @GetMapping("/{id}/participants")
    public ApiResponse<List<JourneyParticipantResponse>> getParticipants(@PathVariable Long id) {
        return ApiResponse.success(journeyService.getJourneyParticipants(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteJourney(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        journeyService.deleteJourney(id, userId);
        return ApiResponse.success(null, "Đã giải tán hành trình");
    }
}