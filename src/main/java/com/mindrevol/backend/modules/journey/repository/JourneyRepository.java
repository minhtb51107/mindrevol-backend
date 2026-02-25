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

    @Query("SELECT COUNT(j) FROM JourneyParticipant jp JOIN jp.journey j WHERE jp.user.id = :userId AND jp.role = 'OWNER' AND j.status = 'ONGOING'")
    long countActiveOwnedJourneys(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE Journey j SET j.status = 'COMPLETED' WHERE j.status = 'ONGOING' AND j.endDate < :today")
    int updateExpiredJourneysStatus(@Param("today") LocalDate today);

    // [CŨ - Giữ lại nếu cần cho logic khác]
    @Query("SELECT j FROM Journey j " +
           "JOIN JourneyParticipant jp ON j.id = jp.journey.id " +
           "WHERE jp.user.id = :userId " +
           "AND (j.endDate IS NULL OR j.endDate >= :today) " +
           "AND j.status <> 'ARCHIVED' " +
           "ORDER BY j.createdAt DESC")
    List<Journey> findActiveJourneysByUserId(@Param("userId") String userId, @Param("today") LocalDate today);

    // [MỚI - TỐI ƯU HÓA] Query này Fetch luôn Participants và User để lấy Avatar không bị N+1
    // Sử dụng DISTINCT để tránh duplicate do Join
    @Query("SELECT DISTINCT j FROM Journey j " +
           "LEFT JOIN FETCH j.participants p " +
           "LEFT JOIN FETCH p.user u " +
           "WHERE j.status <> 'ARCHIVED' " +
           "AND (j.endDate IS NULL OR j.endDate >= :today) " +
           "AND j.id IN (SELECT jp.journey.id FROM JourneyParticipant jp WHERE jp.user.id = :userId) " +
           "ORDER BY j.createdAt DESC")
    List<Journey> findActiveJourneysByUserIdWithMembers(@Param("userId") String userId, @Param("today") LocalDate today);

    // [FIX QUERY COMPLETED] Lấy hành trình đã hết hạn (endDate < today)
    @Query("SELECT j FROM Journey j " +
           "JOIN JourneyParticipant jp ON j.id = jp.journey.id " +
           "WHERE jp.user.id = :userId " +
           "AND j.endDate < :today " +
           "ORDER BY j.endDate DESC")
    List<Journey> findCompletedJourneysByUserId(@Param("userId") String userId, @Param("today") LocalDate today);
    
    @Query("SELECT j FROM Journey j WHERE " +
           "(j.box IS NULL AND EXISTS (SELECT 1 FROM JourneyParticipant jp WHERE jp.journey = j AND jp.user.id = :userId)) " +
           "OR " +
           "(j.box IS NOT NULL AND EXISTS (SELECT 1 FROM BoxMember bm WHERE bm.box.id = j.box.id AND bm.user.id = :userId))")
    Page<Journey> findVisibleJourneysForUser(@Param("userId") String userId, Pageable pageable);
    
    @Query("SELECT j FROM Journey j WHERE j.box.id = :boxId ORDER BY j.createdAt DESC")
    Page<Journey> findJourneysByBoxId(@Param("boxId") String boxId, Pageable pageable);
}