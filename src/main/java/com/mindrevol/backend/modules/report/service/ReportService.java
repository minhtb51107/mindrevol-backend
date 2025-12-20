package com.mindrevol.backend.modules.report.service;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.repository.CheckinCommentRepository;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.report.dto.CreateReportRequest;
import com.mindrevol.backend.modules.report.dto.ResolveReportRequest;
import com.mindrevol.backend.modules.report.entity.Report;
import com.mindrevol.backend.modules.report.entity.ReportStatus;
import com.mindrevol.backend.modules.report.entity.ReportTargetType;
import com.mindrevol.backend.modules.report.repository.ReportRepository;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserStatus;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CheckinRepository checkinRepository;
    private final CheckinCommentRepository commentRepository;
    private final JourneyRepository journeyRepository;

    // --- USER SUBMIT REPORT ---
    @Transactional
    public void createReport(Long reporterId, CreateReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateTargetId(request.getTargetType(), request.getTargetId());

        Report report = Report.builder()
                .reporter(reporter)
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        reportRepository.save(report);
        log.info("User {} reported {} ({})", reporterId, request.getTargetType(), request.getTargetId());
    }

    // --- ADMIN: LẤY DANH SÁCH CHỜ XỬ LÝ ---
    public Page<Report> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtAsc(ReportStatus.PENDING, pageable);
    }

    // --- ADMIN: XỬ LÝ VI PHẠM (CORE LOGIC) ---
    @Transactional
    public void resolveReport(Long reportId, Long handlerId, ResolveReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BadRequestException("Báo cáo này đã được xử lý trước đó.");
        }

        User handler = userRepository.findById(handlerId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        // 1. Thực hiện hành động trừng phạt (nếu Admin chọn)
        if (request.getStatus() == ReportStatus.RESOLVED) {
            
            // A. Khóa tài khoản người vi phạm
            if (request.isBanUser()) {
                banTargetUser(report.getTargetType(), report.getTargetId());
            }

            // B. Xóa nội dung vi phạm
            if (request.isDeleteContent()) {
                deleteTargetContent(report.getTargetType(), report.getTargetId());
            }
        }

        // 2. Cập nhật trạng thái Report
        report.setStatus(request.getStatus());
        report.setAdminNote(request.getAdminNote());
        report.setHandler(handler);
        
        reportRepository.save(report);
        log.info("Admin {} resolved report {} with status {}", handlerId, reportId, request.getStatus());
    }

    // --- HELPER METHODS ---

    private void banTargetUser(ReportTargetType type, String targetId) {
        Long userIdToBan = null;

        // Xác định ID người dùng cần Ban dựa trên đối tượng bị báo cáo
        switch (type) {
            case USER -> userIdToBan = Long.parseLong(targetId);
            case CHECKIN -> checkinRepository.findById(UUID.fromString(targetId))
                    .ifPresent(c -> banUserById(c.getUser().getId()));
            case COMMENT -> commentRepository.findById(UUID.fromString(targetId))
                    .ifPresent(c -> banUserById(c.getUser().getId()));
            // Journey thì thường ban Creator, nhưng logic phức tạp nên tạm bỏ qua
        }

        if (userIdToBan != null) {
            banUserById(userIdToBan);
        }
    }

    private void banUserById(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.BANNED);
            userRepository.save(user);
            log.info("BANNED User ID: {}", userId);
        });
    }

    private void deleteTargetContent(ReportTargetType type, String targetId) {
        try {
            switch (type) {
                case CHECKIN -> {
                    UUID checkinId = UUID.fromString(targetId);
                    if (checkinRepository.existsById(checkinId)) {
                        checkinRepository.deleteById(checkinId);
                        log.info("Deleted Checkin: {}", checkinId);
                    }
                }
                case COMMENT -> {
                	UUID commentId = UUID.fromString(targetId);
                    if (commentRepository.existsById(commentId)) {
                        commentRepository.deleteById(commentId);
                        log.info("Deleted Comment: {}", commentId);
                    }
                }
                case JOURNEY -> {
                    // Logic xóa Journey cần cẩn trọng (xóa nhóm là xóa hết data member)
                    // Ở đây tạm thời chỉ hỗ trợ xóa Checkin/Comment
                    log.warn("Auto-delete Journey is not supported yet via Report API.");
                }
            }
        } catch (Exception e) {
            log.error("Failed to delete content type {} id {}", type, targetId, e);
        }
    }

    private void validateTargetId(ReportTargetType type, String id) {
        try {
            boolean exists = switch (type) {
                case USER -> userRepository.existsById(Long.parseLong(id));
                case CHECKIN -> checkinRepository.existsById(UUID.fromString(id));
                case COMMENT -> commentRepository.existsById(UUID.fromString(id));
                case JOURNEY -> journeyRepository.existsById(UUID.fromString(id));
            };

            if (!exists) {
                throw new ResourceNotFoundException("Đối tượng không tồn tại: " + type + " ID=" + id);
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("ID đối tượng không hợp lệ: " + id);
        }
    }
}