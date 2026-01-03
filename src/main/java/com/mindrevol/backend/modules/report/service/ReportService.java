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

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CheckinRepository checkinRepository;
    private final CheckinCommentRepository commentRepository;
    private final JourneyRepository journeyRepository;

    @Transactional
    public void createReport(String currentUserId, CreateReportRequest request) { // [UUID] String
        User reporter = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateTargetId(request.getTargetType(), request.getTargetId());

        Report report = Report.builder()
                .reporter(reporter)
                .targetId(request.getTargetId()) // [UUID] String
                .targetType(request.getTargetType())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        reportRepository.save(report);
        log.info("User {} reported {} ({})", currentUserId, request.getTargetType(), request.getTargetId());
    }

    public Page<Report> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtAsc(ReportStatus.PENDING, pageable);
    }

    @Transactional
    public void resolveReport(String reportId, String adminId, ResolveReportRequest request) { // [UUID] String
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BadRequestException("Báo cáo này đã được xử lý trước đó.");
        }

        User handler = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (request.getStatus() == ReportStatus.RESOLVED) {
            if (request.isBanUser()) {
                banTargetUser(report.getTargetType(), report.getTargetId());
            }
            if (request.isDeleteContent()) {
                deleteTargetContent(report.getTargetType(), report.getTargetId());
            }
        }

        report.setStatus(request.getStatus());
        report.setAdminNote(request.getAdminNote());
        report.setHandler(handler);
        
        reportRepository.save(report);
        log.info("Admin {} resolved report {} with status {}", adminId, reportId, request.getStatus());
    }

    private void banTargetUser(ReportTargetType type, String targetId) { // [UUID] String
        // [FIX] Không parse Long nữa, dùng trực tiếp String
        switch (type) {
            case USER -> banUserById(targetId);
            case CHECKIN -> checkinRepository.findById(targetId)
                    .ifPresent(c -> banUserById(c.getUser().getId()));
            case COMMENT -> commentRepository.findById(targetId)
                    .ifPresent(c -> banUserById(c.getUser().getId()));
            case JOURNEY -> journeyRepository.findById(targetId)
                    .ifPresent(j -> banUserById(j.getCreator().getId()));
        }
    }

    private void banUserById(String userId) { // [UUID] String
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.BANNED);
            userRepository.save(user);
            log.info("BANNED User ID: {}", userId);
        });
    }

    private void deleteTargetContent(ReportTargetType type, String targetId) { // [UUID] String
        try {
            switch (type) {
                case CHECKIN -> {
                    if (checkinRepository.existsById(targetId)) {
                        checkinRepository.deleteById(targetId);
                        log.info("Deleted Checkin: {}", targetId);
                    }
                }
                case COMMENT -> {
                    if (commentRepository.existsById(targetId)) {
                        commentRepository.deleteById(targetId);
                        log.info("Deleted Comment: {}", targetId);
                    }
                }
                case JOURNEY -> {
                    log.warn("Auto-delete Journey is not supported yet via Report API.");
                }
                default -> log.warn("Unknown target type for delete: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to delete content type {} id {}", type, targetId, e);
        }
    }

    private void validateTargetId(ReportTargetType type, String targetId) { // [UUID] String
        boolean exists = switch (type) {
            case USER -> userRepository.existsById(targetId);
            case CHECKIN -> checkinRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
            case JOURNEY -> journeyRepository.existsById(targetId);
        };

        if (!exists) {
            throw new ResourceNotFoundException("Đối tượng không tồn tại: " + type + " ID=" + targetId);
        }
    }
}