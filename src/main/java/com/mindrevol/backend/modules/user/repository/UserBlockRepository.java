package com.mindrevol.backend.modules.user.repository;

import com.mindrevol.backend.modules.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    
    // ĐÚNG: Dùng 'BlockerId' và 'BlockedId' khớp với entity
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    
    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    // Lấy danh sách ID để filter feed
    @Query(value = "SELECT ub.blocked_id FROM user_blocks ub WHERE ub.blocker_id = :userId " +
                   "UNION " +
                   "SELECT ub.blocker_id FROM user_blocks ub WHERE ub.blocked_id = :userId", 
           nativeQuery = true)
    Set<Long> findAllBlockedUserIdsInteraction(@Param("userId") Long userId);
    
    List<UserBlock> findAllByBlockerId(Long blockerId);
}