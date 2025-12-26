package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.CheckinComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface CheckinCommentRepository extends JpaRepository<CheckinComment, UUID> {
    
    // [CẬP NHẬT] Thay vì sub-query user block, ta truyền danh sách excludedUserIds vào
    // user.id NOT IN (...) sẽ loại bỏ comment của người bị chặn
    @Query("SELECT c FROM CheckinComment c " +
           "JOIN FETCH c.user u " +
           "WHERE c.checkin.id = :checkinId " +
           "AND u.id NOT IN :excludedUserIds " +
           "ORDER BY c.createdAt ASC")
    Page<CheckinComment> findByCheckinId(@Param("checkinId") UUID checkinId, 
                                         @Param("excludedUserIds") Collection<Long> excludedUserIds, 
                                         Pageable pageable);

    // Dùng cho modal activity (lấy comment mới nhất)
    @Query("SELECT c FROM CheckinComment c JOIN FETCH c.user WHERE c.checkin.id = :checkinId ORDER BY c.createdAt DESC")
    List<CheckinComment> findAllByCheckinId(@Param("checkinId") UUID checkinId, Pageable pageable);
}