package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.Journey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JourneyRepository extends JpaRepository<Journey, Long> {
    
    Optional<Journey> findByInviteCode(String inviteCode);
    
    boolean existsByInviteCode(String inviteCode);

    List<Journey> findByCreatorId(Long creatorId);

    @Query("SELECT COUNT(j) FROM JourneyParticipant jp JOIN jp.journey j WHERE jp.user.id = :userId AND jp.role = 'OWNER' AND j.status = 'ONGOING'")
    long countActiveOwnedJourneys(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Journey j SET j.status = 'COMPLETED' WHERE j.status = 'ONGOING' AND j.endDate < :today")
    int updateExpiredJourneysStatus(@Param("today") LocalDate today);

    // [FIX] Thêm @Query để tìm Journey đã hoàn thành mà user có tham gia
    @Query("SELECT j FROM Journey j " +
           "JOIN JourneyParticipant jp ON j.id = jp.journey.id " +
           "WHERE jp.user.id = :userId " +
           "AND j.status = 'COMPLETED'")
    List<Journey> findCompletedJourneysByUserId(@Param("userId") Long userId);
}