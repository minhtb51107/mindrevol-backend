package com.mindrevol.backend.modules.user.repository;

import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime; // Dùng OffsetDateTime cho khớp với BaseEntity
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByHandle(String handle);
    boolean existsByEmail(String email);
    boolean existsByHandle(String handle);

    // --- MỚI: TÌM USER ĐÃ XÓA MỀM QUÁ 30 NGÀY ---
    // Phải dùng nativeQuery vì Hibernate @Where sẽ tự động lọc bỏ user đã xóa
    @Query(value = "SELECT * FROM users WHERE deleted_at < :cutoffDate", nativeQuery = true)
    List<User> findUsersReadyForHardDelete(@Param("cutoffDate") OffsetDateTime cutoffDate);

    // --- MỚI: XÓA VĨNH VIỄN (HARD DELETE) ---
    @Modifying
    @Query(value = "DELETE FROM users WHERE id = :userId", nativeQuery = true)
    void hardDeleteUser(@Param("userId") Long userId);
    
    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.fullname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.handle) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND u.deletedAt IS NULL")
     List<User> searchUsers(@Param("query") String query);
}