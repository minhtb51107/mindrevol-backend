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

    // Ngưỡng báo cáo Fake để hệ thống tự động gỡ bài
    // Trong MVP set cứng là 2 phiếu report (hoặc có thể cấu hình theo số thành viên nhóm)
    private static final int FAKE_REPORT_THRESHOLD = 2; 

    @Override
    @Transactional
    public void castVote(UUID checkinId, User voter, boolean isApproved) {
        // isApproved = true -> Vote Uy tín (Like/Support) - Chỉ mang tính chất tinh thần
        // isApproved = false -> Report Fake (Quan trọng) - Dùng để kích hoạt cơ chế trừng phạt

        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài check-in không tồn tại"));

        if (checkin.getStatus() == CheckinStatus.REJECTED) {
            throw new BadRequestException("Bài này đã bị gỡ bỏ rồi, không cần báo cáo nữa.");
        }

        if (checkin.getUser().getId().equals(voter.getId())) {
            throw new BadRequestException("Không thể tự vote cho chính mình.");
        }

        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(checkin.getJourney().getId(), voter.getId())
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

        // 3. Logic Xử lý Report Fake (Chỉ quan tâm khi isApproved = false)
        if (!isApproved) { 
            long fakeCount = verificationRepository.countRejections(checkinId); // Đếm tổng số phiếu reject
            
            // Đặc quyền Admin: 1 phiếu của Admin có sức nặng bằng cả Threshold -> Xóa luôn
            boolean isAdmin = participant.getRole() == JourneyRole.ADMIN;
            
            if (isAdmin || fakeCount >= FAKE_REPORT_THRESHOLD) {
                punishUser(checkin);
            }
        }
    }

    private void punishUser(Checkin checkin) {
        // 1. Đổi trạng thái bài viết sang REJECTED
        checkin.setStatus(CheckinStatus.REJECTED);
        checkinRepository.save(checkin);

        // 2. Thu hồi điểm và streak (Trừng phạt)
        // Gọi sang GamificationService để thực hiện trừ điểm và lùi ngày check-in
        gamificationService.revokeGamification(checkin);

        // 3. Gửi thông báo cho người vi phạm
        notificationService.sendAndSaveNotification(
                checkin.getUser().getId(),
                null, // System sender
                NotificationType.SYSTEM,
                "Bài check-in bị gỡ! 🚨",
                "Cộng đồng đã báo cáo ảnh của bạn không hợp lệ. Điểm và chuỗi Streak đã bị thu hồi.",
                checkin.getId().toString(),
                null
        );
        log.info("Checkin {} marked as REJECTED due to community reports.", checkin.getId());
    }
}