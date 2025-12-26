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

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    private final CheckinRepository checkinRepository;
    private final CheckinVerificationRepository verificationRepository;
    private final JourneyParticipantRepository participantRepository;
    private final GamificationService gamificationService;
    private final NotificationService notificationService;

    // Cấu hình tỷ lệ: 30% thành viên báo xấu -> Gỡ bài
    private static final double REJECT_THRESHOLD_PERCENTAGE = 0.3;
    // Ngưỡng tối thiểu (để tránh nhóm quá nhỏ 1-2 người report là bay màu ngay)
    private static final int MIN_VOTES_REQUIRED = 2;

    @Override
    @Transactional
    public void castVote(UUID checkinId, User voter, boolean isApproved) {
        // isApproved = true -> Vote Uy tín (Support)
        // isApproved = false -> Report Fake (Reject)

        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài check-in không tồn tại"));

        // Nếu bài đã bị gỡ hoặc admin duyệt rồi thì thôi
        if (checkin.getStatus() == CheckinStatus.REJECTED) {
            throw new BadRequestException("Bài viết này đã bị gỡ bỏ.");
        }

        if (checkin.getUser().getId().equals(voter.getId())) {
            throw new BadRequestException("Không thể tự vote cho chính mình.");
        }

        // Kiểm tra quyền thành viên
        JourneyParticipant voterParticipant = participantRepository.findByJourneyIdAndUserId(checkin.getJourney().getId(), voter.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên nhóm này"));

        // 1. Kiểm tra xem đã vote chưa
        if (verificationRepository.existsByCheckinIdAndVoterId(checkinId, voter.getId())) {
            throw new BadRequestException("Bạn đã bỏ phiếu cho bài này rồi.");
        }

        // 2. Lưu phiếu bầu
        CheckinVerification verification = CheckinVerification.builder()
                .checkin(checkin)
                .voter(voter)
                .isApproved(isApproved)
                .build();
        verificationRepository.save(verification);

        // 3. Logic Xử lý Report Fake (Chỉ quan tâm khi vote Reject)
        if (!isApproved) {
            handleRejectVote(checkin, voterParticipant);
        }
    }

    private void handleRejectVote(Checkin checkin, JourneyParticipant voterParticipant) {
        UUID journeyId = checkin.getJourney().getId();

        // Đếm tổng số phiếu Reject hiện tại
        long currentRejectCount = verificationRepository.countRejections(checkin.getId());

        // Lấy tổng số thành viên trong hành trình
        long totalMembers = participantRepository.countByJourneyId(journeyId);

        // Tính ngưỡng phiếu cần thiết để gỡ bài
        // Ví dụ: Nhóm 10 người -> Cần max(2, 10 * 0.3) = 3 phiếu
        // Ví dụ: Nhóm 5 người -> Cần max(2, 5 * 0.3 = 1.5) = 2 phiếu
        long dynamicThreshold = (long) Math.ceil(totalMembers * REJECT_THRESHOLD_PERCENTAGE);
        long requiredVotes = Math.max(MIN_VOTES_REQUIRED, dynamicThreshold);

        log.info("Checkin {} - Rejections: {}/{}. Total Members: {}", 
                checkin.getId(), currentRejectCount, requiredVotes, totalMembers);

        // ĐẶC QUYỀN ADMIN/OWNER: 1 phiếu của Admin có sức nặng tuyệt đối -> Xóa luôn
        boolean isAdmin = (voterParticipant.getRole() == JourneyRole.ADMIN || voterParticipant.getRole() == JourneyRole.OWNER);

        if (isAdmin || currentRejectCount >= requiredVotes) {
            punishUser(checkin);
        }
    }

    private void punishUser(Checkin checkin) {
        log.warn("Checkin {} marked as REJECTED. Initiating punishment.", checkin.getId());

        // 1. Đổi trạng thái bài viết sang REJECTED
        checkin.setStatus(CheckinStatus.REJECTED);
        checkinRepository.save(checkin);

        // 2. Thu hồi điểm và streak (Trừng phạt)
        gamificationService.revokeGamification(checkin);

        // 3. Gửi thông báo cho người vi phạm
        notificationService.sendAndSaveNotification(
                checkin.getUser().getId(),
                null, // System sender
                NotificationType.SYSTEM,
                "Bài check-in bị gỡ! 🚨",
                "Cộng đồng đã báo cáo ảnh của bạn không hợp lệ. Điểm và chuỗi Streak của bài này đã bị thu hồi.",
                checkin.getId().toString(),
                null // Không cần image
        );
    }
}