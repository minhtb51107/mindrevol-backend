package com.mindrevol.backend.modules.report.repository;

import com.mindrevol.backend.modules.report.entity.Report;
import com.mindrevol.backend.modules.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// [UUID] JpaRepository<Report, String>
@Repository
public interface ReportRepository extends JpaRepository<Report, String> {
    Page<Report> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);
}