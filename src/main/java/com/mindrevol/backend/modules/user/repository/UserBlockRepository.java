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
    
    // Kiểm tra nhanh xem A có đang chặn B không
    boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);
    
    // Lấy record Block cụ thể để có thể Unblock (xóa record)
    Optional<UserBlock> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

    // RẤT QUAN TRỌNG CHO BẢO MẬT HIỂN THỊ:
    // Lấy một tập hợp (Set) ID bao gồm: Những người mình đang chặn GỘP VỚI những người đang chặn mình.
    // Kết quả dùng để lọc Feed/Tin nhắn/Tìm kiếm (để 2 bên hoàn toàn "tàng hình" với nhau).
    @Query(value = "SELECT ub.blocked_id FROM user_blocks ub WHERE ub.blocker_id = :userId " +
                   "UNION " +
                   "SELECT ub.blocker_id FROM user_blocks ub WHERE ub.blocked_id = :userId", 
           nativeQuery = true)
    Set<String> findAllBlockedUserIdsInteraction(@Param("userId") String userId);
    
    // Lấy danh sách (Block List) hiển thị trong phần Cài đặt của người dùng
    List<UserBlock> findAllByBlockerId(String blockerId);
}