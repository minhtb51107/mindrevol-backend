package com.mindrevol.backend.modules.checkin.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.entity.CheckinVerification;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.checkin.repository.CheckinVerificationRepository;
import com.mindrevol.backend.modules.checkin.service.VerificationService;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.entity.JourneyRole;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import com.mindrevol.backend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    private final CheckinRepository checkinRepository;
    private final CheckinVerificationRepository verificationRepository;
    private final JourneyParticipantRepository participantRepository;
    private final GamificationService gamificationService;
    private final NotificationService notificationService;

    private static final double REJECT_THRESHOLD_PERCENTAGE = 0.3;
    private static final int MIN_VOTES_REQUIRED = 2;

    @Override
    @Transactional
    public void castVote(String checkinId, User voter, boolean isApproved) { // [UUID] String
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài check-in không tồn tại"));

        if (checkin.getStatus() == CheckinStatus.REJECTED) {
            throw new BadRequestException("Bài viết này đã bị gỡ bỏ.");
        }

        if (checkin.getUser().getId().equals(voter.getId())) {
            throw new BadRequestException("Không thể tự vote cho chính mình.");
        }

        JourneyParticipant voterParticipant = participantRepository.findByJourneyIdAndUserId(checkin.getJourney().getId(), voter.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên nhóm này"));

        if (verificationRepository.existsByCheckinIdAndVoterId(checkinId, voter.getId())) {
            throw new BadRequestException("Bạn đã bỏ phiếu cho bài này rồi.");
        }

        CheckinVerification verification = CheckinVerification.builder()
                .checkin(checkin)
                .voter(voter)
                .isApproved(isApproved)
                .build();
        verificationRepository.save(verification);

        if (!isApproved) {
            handleRejectVote(checkin, voterParticipant);
        }
    }

    private void handleRejectVote(Checkin checkin, JourneyParticipant voterParticipant) {
        String journeyId = checkin.getJourney().getId();

        long currentRejectCount = verificationRepository.countRejections(checkin.getId());
        long totalMembers = participantRepository.countByJourneyId(journeyId);

        long dynamicThreshold = (long) Math.ceil(totalMembers * REJECT_THRESHOLD_PERCENTAGE);
        long requiredVotes = Math.max(MIN_VOTES_REQUIRED, dynamicThreshold);

        log.info("Checkin {} - Rejections: {}/{}. Total Members: {}", 
                checkin.getId(), currentRejectCount, requiredVotes, totalMembers);

        boolean isOwner = (voterParticipant.getRole() == JourneyRole.OWNER);

        if (isOwner || currentRejectCount >= requiredVotes) {
            punishUser(checkin);
        }
    }

    private void punishUser(Checkin checkin) {
        log.warn("Checkin {} marked as REJECTED. Initiating punishment.", checkin.getId());

        checkin.setStatus(CheckinStatus.REJECTED);
        checkinRepository.save(checkin);

        gamificationService.revokeGamification(checkin);

        notificationService.sendAndSaveNotification(
                checkin.getUser().getId(),
                null, 
                NotificationType.SYSTEM,
                "Bài check-in bị gỡ! 🚨",
                "Cộng đồng đã báo cáo ảnh của bạn không hợp lệ.",
                checkin.getId(),
                null 
        );
    }
}