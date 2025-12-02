package com.mindrevol.backend.modules.report.service;

import com.mindrevol.backend.common.exception.BadRequestException; // Import
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.report.dto.CreateReportRequest;
import com.mindrevol.backend.modules.report.entity.Report;
import com.mindrevol.backend.modules.report.entity.ReportStatus;
import com.mindrevol.backend.modules.report.repository.ReportRepository;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
// Import thêm các Repo cần validate
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.checkin.repository.CheckinCommentRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    // --- INJECT THÊM ---
    private final CheckinRepository checkinRepository;
    private final CheckinCommentRepository commentRepository;
    private final JourneyRepository journeyRepository;

    @Transactional
    public void createReport(Long reporterId, CreateReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // --- LOGIC MỚI: VALIDATE TARGET ID ---
        validateTargetId(request.getTargetType(), request.getTargetId());
        // -------------------------------------

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

    private void validateTargetId(com.mindrevol.backend.modules.report.entity.ReportTargetType type, String id) {
        try {
            boolean exists = switch (type) {
                case USER -> userRepository.existsById(Long.parseLong(id));
                case CHECKIN -> checkinRepository.existsById(UUID.fromString(id));
                case COMMENT -> commentRepository.existsById(Long.parseLong(id));
                case JOURNEY -> journeyRepository.existsById(UUID.fromString(id));
            };

            if (!exists) {
                throw new ResourceNotFoundException("Đối tượng báo cáo không tồn tại: " + type + " ID=" + id);
            }
        } catch (IllegalArgumentException e) {
            // Lỗi format ID (ví dụ gửi String lung tung vào chỗ cần UUID/Long)
            throw new BadRequestException("ID đối tượng không hợp lệ: " + id);
        }
    }
}