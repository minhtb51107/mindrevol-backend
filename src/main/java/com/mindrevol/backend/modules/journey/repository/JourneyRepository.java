package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.Journey;
import jakarta.persistence.LockModeType;
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

    // [FIX CONCURRENCY] Thêm hàm tìm kiếm có khóa (Lock) để chặn xung đột khi Join
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Journey j WHERE j.id = :id")
    Optional<Journey> findByIdWithLock(@Param("id") String id);

    @Query("SELECT COUNT(j) FROM JourneyParticipant jp JOIN jp.journey j WHERE jp.user.id = :userId AND jp.role = 'OWNER' AND j.status = 'ONGOING'")
    long countActiveOwnedJourneys(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE Journey j SET j.status = 'COMPLETED' WHERE j.status = 'ONGOING' AND j.endDate < :today")
    int updateExpiredJourneysStatus(@Param("today") LocalDate today);

    @Query("SELECT j FROM Journey j " +
           "JOIN JourneyParticipant jp ON j.id = jp.journey.id " +
           "WHERE jp.user.id = :userId " +
           "AND j.status = 'COMPLETED'")
    List<Journey> findCompletedJourneysByUserId(@Param("userId") String userId);
}