package com.mindrevol.backend.modules.report.service;

import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.report.dto.CreateReportRequest;
import com.mindrevol.backend.modules.report.entity.Report;
import com.mindrevol.backend.modules.report.entity.ReportStatus;
import com.mindrevol.backend.modules.report.repository.ReportRepository;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createReport(Long reporterId, CreateReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // TODO: Có thể thêm logic kiểm tra targetId có tồn tại thật không (Checkin/User...)
        // Nhưng để giảm phụ thuộc giữa các module (Loose coupling), tạm thời ta tin tưởng client gửi đúng ID.

        Report report = Report.builder()
                .reporter(reporter)
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        reportRepository.save(report);
        
        log.info("User {} reported {} ({}) for reason {}", 
                reporterId, request.getTargetType(), request.getTargetId(), request.getReason());
                
        // Mở rộng: Nếu 1 bài viết bị report > 10 lần -> Tự động ẩn bài đó (Soft Hide)
    }
}