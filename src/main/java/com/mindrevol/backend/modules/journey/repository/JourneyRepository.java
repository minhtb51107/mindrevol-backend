package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.Journey;
import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JourneyRepository extends JpaRepository<Journey, String> {
    
    Optional<Journey> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);
    List<Journey> findByCreatorId(String creatorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Journey j WHERE j.id = :id")
    Optional<Journey> findByIdWithLock(@Param("id") String id);

    @Query("SELECT COUNT(j) FROM Journey j WHERE j.creator.id = :userId AND j.status = 'ONGOING'")
    long countActiveOwnedJourneys(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE Journey j SET j.status = 'COMPLETED' WHERE j.status = 'ONGOING' AND j.endDate < :today")
    int updateExpiredJourneysStatus(@Param("today") LocalDate today);

    @Query("SELECT DISTINCT j FROM Journey j " +
           "LEFT JOIN JourneyParticipant targetJp ON j.id = targetJp.journey.id " +
           "LEFT JOIN BoxMember bm ON j.box.id = bm.box.id " +
           "WHERE j.status <> 'ARCHIVED' " +
           "AND (targetJp.user.id = :userId OR bm.user.id = :userId) " +
           "ORDER BY j.createdAt DESC")
    List<Journey> findPublicJourneysByUserId(@Param("userId") String userId);

    @Query("SELECT DISTINCT j FROM Journey j " +
           "JOIN JourneyParticipant targetJp ON j.id = targetJp.journey.id " +
           "WHERE j.status <> 'ARCHIVED' " +
           "AND targetJp.user.id = :userId " +
           "AND targetJp.isProfileVisible = false " +
           "ORDER BY j.createdAt DESC")
    List<Journey> findPrivateJourneysByUserId(@Param("userId") String userId);

    // Cập nhật: Lấy hành trình của Guest HOẶC của BoxMember
    @Query("SELECT DISTINCT j FROM Journey j " +
           "LEFT JOIN FETCH j.participants p " +
           "LEFT JOIN FETCH p.user u " +
           "WHERE j.status <> 'ARCHIVED' AND (j.endDate IS NULL OR j.endDate >= :today) " +
           "AND (j.id IN (SELECT jp.journey.id FROM JourneyParticipant jp WHERE jp.user.id = :userId) " +
           "OR j.box.id IN (SELECT bm.box.id FROM BoxMember bm WHERE bm.user.id = :userId)) " +
           "ORDER BY j.createdAt DESC")
    List<Journey> findActiveJourneysByUserIdWithMembers(@Param("userId") String userId, @Param("today") LocalDate today);
    
    @Query("SELECT j FROM Journey j " +
           "LEFT JOIN JourneyParticipant jp ON j.id = jp.journey.id " +
           "LEFT JOIN BoxMember bm ON j.box.id = bm.box.id " +
           "WHERE (jp.user.id = :userId OR bm.user.id = :userId) AND j.endDate < :today " +
           "ORDER BY j.endDate DESC")
    List<Journey> findCompletedJourneysByUserId(@Param("userId") String userId, @Param("today") LocalDate today);
    
    @Query("SELECT j FROM Journey j WHERE " +
           "(j.box IS NULL AND EXISTS (SELECT 1 FROM JourneyParticipant jp WHERE jp.journey = j AND jp.user.id = :userId)) " +
           "OR (j.box IS NOT NULL AND EXISTS (SELECT 1 FROM BoxMember bm WHERE bm.box.id = j.box.id AND bm.user.id = :userId))")
    Page<Journey> findVisibleJourneysForUser(@Param("userId") String userId, Pageable pageable);
    
    @Query("SELECT j FROM Journey j WHERE j.box.id = :boxId ORDER BY j.createdAt DESC")
    Page<Journey> findJourneysByBoxId(@Param("boxId") String boxId, Pageable pageable);

    @Query("SELECT j FROM Journey j WHERE " +
           "j.id IN (SELECT jp.journey.id FROM JourneyParticipant jp WHERE jp.user.id = :userId) " +
           "OR j.box.id IN (SELECT bm.box.id FROM BoxMember bm WHERE bm.user.id = :userId)")
    List<Journey> findAllJourneysForUser(@Param("userId") String userId);
}