package com.mindrevol.backend.modules.user.repository;

import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

// [UUID] JpaRepository<User, String>
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByHandle(String handle);
    boolean existsByEmail(String email);
    boolean existsByHandle(String handle);

    @Query(value = "SELECT * FROM users WHERE deleted_at < :cutoffDate", nativeQuery = true)
    List<User> findUsersReadyForHardDelete(@Param("cutoffDate") OffsetDateTime cutoffDate);

    // [UUID] userId là String
    @Modifying
    @Query(value = "DELETE FROM users WHERE id = :userId", nativeQuery = true)
    void hardDeleteUser(@Param("userId") String userId);
    
    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.fullname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.handle) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND u.deletedAt IS NULL")
     List<User> searchUsers(@Param("query") String query);
    
    // [UUID] userId là String
    @Modifying
    @Query("UPDATE User u SET u.points = u.points + :amount WHERE u.id = :userId")
    void incrementPoints(@Param("userId") String userId, @Param("amount") int amount);

    // [UUID] userId là String
    @Modifying
    @Query("UPDATE User u SET u.points = u.points - :amount WHERE u.id = :userId AND u.points >= :amount")
    int decrementPoints(@Param("userId") String userId, @Param("amount") int amount);
}