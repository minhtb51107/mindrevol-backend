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

// [FIX] Extends Long
@Repository
public interface CheckinCommentRepository extends JpaRepository<CheckinComment, Long> {
    
    // [FIX] UUID checkinId -> Long checkinId
    @Query("SELECT c FROM CheckinComment c " +
           "JOIN FETCH c.user u " +
           "WHERE c.checkin.id = :checkinId " +
           "AND u.id NOT IN :excludedUserIds " +
           "ORDER BY c.createdAt ASC")
    Page<CheckinComment> findByCheckinId(@Param("checkinId") Long checkinId, 
                                         @Param("excludedUserIds") Collection<Long> excludedUserIds, 
                                         Pageable pageable);

    @Query("SELECT c FROM CheckinComment c JOIN FETCH c.user WHERE c.checkin.id = :checkinId ORDER BY c.createdAt DESC")
    List<CheckinComment> findAllByCheckinId(@Param("checkinId") Long checkinId, Pageable pageable);
}