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
    
    // Tìm tài khoản bằng Email
    Optional<User> findByEmail(String email);
    
    // Tìm tài khoản bằng Handle (username duy nhất kiểu @username)
    Optional<User> findByHandle(String handle);
    
    // Kiểm tra trùng lặp email/handle khi đăng ký
    boolean existsByEmail(String email);
    boolean existsByHandle(String handle);

    // Job tuân thủ luật bảo mật dữ liệu (GDPR):
    // Lấy những user đã "xóa mềm" tài khoản trước mốc thời gian (cutoffDate) để chuẩn bị xóa vĩnh viễn (Hard delete).
    @Query(value = "SELECT * FROM users WHERE deleted_at < :cutoffDate", nativeQuery = true)
    List<User> findUsersReadyForHardDelete(@Param("cutoffDate") OffsetDateTime cutoffDate);

    // Thực thi XÓA VĨNH VIỄN một user khỏi Database (bỏ qua filter xóa mềm)
    @Modifying
    @Query(value = "DELETE FROM users WHERE id = :userId", nativeQuery = true)
    void hardDeleteUser(@Param("userId") String userId);
    
    // Thanh tìm kiếm user (Tìm bằng tên, handle hoặc email) - Bỏ qua những user đã xóa tài khoản
    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.fullname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.handle) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND u.deletedAt IS NULL")
     List<User> searchUsers(@Param("query") String query);
    
    // Atomic Update (Update trực tiếp dưới CSDL) để CỘNG ĐIỂM (points) thưởng an toàn tránh Race Condition
    @Modifying
    @Query("UPDATE User u SET u.points = u.points + :amount WHERE u.id = :userId")
    void incrementPoints(@Param("userId") String userId, @Param("amount") int amount);

    // Atomic Update để TRỪ ĐIỂM (có điều kiện điểm hiện tại phải >= số lượng trừ)
    @Modifying
    @Query("UPDATE User u SET u.points = u.points - :amount WHERE u.id = :userId AND u.points >= :amount")
    int decrementPoints(@Param("userId") String userId, @Param("amount") int amount);
    
    // Job định kỳ: RESET chuỗi đăng nhập/checkin (Streak) về 0 nếu người dùng quên không check-in > 1 ngày.
    @Modifying
    @Query(value = "UPDATE users SET current_streak = 0 WHERE current_streak > 0 AND (last_checkin_at IS NULL OR last_checkin_at < CURRENT_DATE - INTERVAL '1 day')", nativeQuery = true)
    int resetBrokenStreaks();
}