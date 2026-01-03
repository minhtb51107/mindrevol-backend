package com.mindrevol.backend.modules.user.repository;

import com.mindrevol.backend.modules.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

// [UUID] JpaRepository<UserBlock, String>
@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, String> {
    
    // [UUID] String
    boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);
    
    Optional<UserBlock> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

    // [UUID] Trả về Set<String>
    @Query(value = "SELECT ub.blocked_id FROM user_blocks ub WHERE ub.blocker_id = :userId " +
                   "UNION " +
                   "SELECT ub.blocker_id FROM user_blocks ub WHERE ub.blocked_id = :userId", 
           nativeQuery = true)
    Set<String> findAllBlockedUserIdsInteraction(@Param("userId") String userId);
    
    List<UserBlock> findAllByBlockerId(String blockerId);
}