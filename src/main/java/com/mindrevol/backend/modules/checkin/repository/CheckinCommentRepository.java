package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.CheckinComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CheckinCommentRepository extends JpaRepository<CheckinComment, Long> {
    
    // Lấy comment của 1 bài checkin, sắp xếp cũ nhất trước (để đọc theo dòng thời gian)
    @Query("SELECT c FROM CheckinComment c JOIN FETCH c.user WHERE c.checkin.id = :checkinId ORDER BY c.createdAt ASC")
    Page<CheckinComment> findByCheckinId(@Param("checkinId") UUID checkinId, Pageable pageable);
}