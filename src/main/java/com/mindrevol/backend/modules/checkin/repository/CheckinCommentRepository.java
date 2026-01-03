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

// [UUID] Extends String
@Repository
public interface CheckinCommentRepository extends JpaRepository<CheckinComment, String> {
    
    // [UUID] checkinId, excludedUserIds là String
    @Query("SELECT c FROM CheckinComment c " +
           "JOIN FETCH c.user u " +
           "WHERE c.checkin.id = :checkinId " +
           "AND u.id NOT IN :excludedUserIds " +
           "ORDER BY c.createdAt ASC")
    Page<CheckinComment> findByCheckinId(@Param("checkinId") String checkinId, 
                                         @Param("excludedUserIds") Collection<String> excludedUserIds, 
                                         Pageable pageable);

    @Query("SELECT c FROM CheckinComment c JOIN FETCH c.user WHERE c.checkin.id = :checkinId ORDER BY c.createdAt DESC")
    List<CheckinComment> findAllByCheckinId(@Param("checkinId") String checkinId, Pageable pageable);
}