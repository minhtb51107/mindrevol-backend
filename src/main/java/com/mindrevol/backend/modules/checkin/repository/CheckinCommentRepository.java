package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.CheckinComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// QUAN TRỌNG: Đổi JpaRepository<..., Long> thành <..., UUID>
@Repository
public interface CheckinCommentRepository extends JpaRepository<CheckinComment, UUID> {
    
    // 1. Lấy comment phân trang cho phần hiển thị dưới bài viết (Tránh người bị block)
    @Query("SELECT c FROM CheckinComment c " +
           "JOIN FETCH c.user u " +
           "WHERE c.checkin.id = :checkinId " +
           "AND u.id NOT IN (SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker.id = :currentUserId) " +
           "AND u.id NOT IN (SELECT ub.blocker.id FROM UserBlock ub WHERE ub.blocked.id = :currentUserId) " +
           "ORDER BY c.createdAt ASC")
    Page<CheckinComment> findByCheckinId(@Param("checkinId") UUID checkinId, 
                                         @Param("currentUserId") Long currentUserId, 
                                         Pageable pageable);

    // 2. [MỚI] Lấy danh sách comment để trộn vào Activity Modal (Không cần phân trang phức tạp, lấy mới nhất)
    // Dùng cho ReactionServiceImpl.getReactions
    @Query("SELECT c FROM CheckinComment c JOIN FETCH c.user WHERE c.checkin.id = :checkinId ORDER BY c.createdAt DESC")
    List<CheckinComment> findAllByCheckinId(@Param("checkinId") UUID checkinId, Pageable pageable);
}