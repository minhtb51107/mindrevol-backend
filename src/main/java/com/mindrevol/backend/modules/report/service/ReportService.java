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

// [FIX] Đã xóa import java.util.UUID

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
                .targetId(request.getTargetId()) // Vẫn lưu là String trong DB, nhưng nội dung là số Long
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

    // --- ADMIN: XỬ LÝ VI PHẠM ---
    @Transactional
    public void resolveReport(Long reportId, Long handlerId, ResolveReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BadRequestException("Báo cáo này đã được xử lý trước đó.");
        }

        User handler = userRepository.findById(handlerId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        // 1. Thực hiện hành động trừng phạt
        if (request.getStatus() == ReportStatus.RESOLVED) {
            if (request.isBanUser()) {
                banTargetUser(report.getTargetType(), report.getTargetId());
            }
            if (request.isDeleteContent()) {
                deleteTargetContent(report.getTargetType(), report.getTargetId());
            }
        }

        // 2. Cập nhật trạng thái
        report.setStatus(request.getStatus());
        report.setAdminNote(request.getAdminNote());
        report.setHandler(handler);
        
        reportRepository.save(report);
        log.info("Admin {} resolved report {} with status {}", handlerId, reportId, request.getStatus());
    }

    // --- HELPER METHODS ---

    private void banTargetUser(ReportTargetType type, String targetId) {
        try {
            Long id = Long.parseLong(targetId); // [FIX] Parse Long
            
            switch (type) {
                case USER -> banUserById(id);
                case CHECKIN -> checkinRepository.findById(id)
                        .ifPresent(c -> banUserById(c.getUser().getId()));
                case COMMENT -> commentRepository.findById(id)
                        .ifPresent(c -> banUserById(c.getUser().getId()));
                // Case JOURNEY: Tùy logic, có thể ban creator
                case JOURNEY -> journeyRepository.findById(id)
                        .ifPresent(j -> banUserById(j.getCreator().getId()));
            }
        } catch (NumberFormatException e) {
            log.error("Invalid target ID format for ban: {}", targetId);
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
            Long id = Long.parseLong(targetId); // [FIX] Parse Long

            switch (type) {
                case CHECKIN -> {
                    if (checkinRepository.existsById(id)) {
                        checkinRepository.deleteById(id);
                        log.info("Deleted Checkin: {}", id);
                    }
                }
                case COMMENT -> {
                    if (commentRepository.existsById(id)) {
                        commentRepository.deleteById(id);
                        log.info("Deleted Comment: {}", id);
                    }
                }
                case JOURNEY -> {
                    // Cẩn trọng khi xóa Journey
                    log.warn("Auto-delete Journey is not supported yet via Report API.");
                }
                default -> log.warn("Unknown target type for delete: {}", type);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid target ID format for delete: {}", targetId);
        } catch (Exception e) {
            log.error("Failed to delete content type {} id {}", type, targetId, e);
        }
    }

    private void validateTargetId(ReportTargetType type, String idStr) {
        try {
            Long id = Long.parseLong(idStr); // [FIX] Parse Long
            
            boolean exists = switch (type) {
                case USER -> userRepository.existsById(id);
                case CHECKIN -> checkinRepository.existsById(id);
                case COMMENT -> commentRepository.existsById(id);
                case JOURNEY -> journeyRepository.existsById(id);
            };

            if (!exists) {
                throw new ResourceNotFoundException("Đối tượng không tồn tại: " + type + " ID=" + id);
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException("ID đối tượng không hợp lệ (Phải là số): " + idStr);
        }
    }
}