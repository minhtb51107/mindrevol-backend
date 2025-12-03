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
	@Query("SELECT c FROM CheckinComment c " +
	           "JOIN FETCH c.user u " +
	           "WHERE c.checkin.id = :checkinId " +
	           // --- LOGIC BLOCK ---
	           "AND u.id NOT IN (SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker.id = :currentUserId) " +
	           "AND u.id NOT IN (SELECT ub.blocker.id FROM UserBlock ub WHERE ub.blocked.id = :currentUserId) " +
	           // -------------------
	           "ORDER BY c.createdAt ASC")
	    Page<CheckinComment> findByCheckinId(@Param("checkinId") UUID checkinId, 
	                                         @Param("currentUserId") Long currentUserId, // <--- Thêm param này
	                                         Pageable pageable);
}