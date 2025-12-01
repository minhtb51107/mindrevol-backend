package com.mindrevol.backend.modules.user.repository;

import com.mindrevol.backend.modules.user.entity.Friendship;
import com.mindrevol.backend.modules.user.entity.FriendshipStatus;
import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // 1. Kiểm tra xem giữa 2 người đã tồn tại mối quan hệ nào chưa (bất kể ai gửi)
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
           "FROM Friendship f " +
           "WHERE (f.requester.id = :userId1 AND f.addressee.id = :userId2) " +
           "OR (f.requester.id = :userId2 AND f.addressee.id = :userId1)")
    boolean existsByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // 2. Lấy mối quan hệ cụ thể giữa 2 người
    @Query("SELECT f FROM Friendship f " +
           "WHERE (f.requester.id = :userId1 AND f.addressee.id = :userId2) " +
           "OR (f.requester.id = :userId2 AND f.addressee.id = :userId1)")
    Optional<Friendship> findByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // 3. Lấy danh sách bạn bè (Status = ACCEPTED)
    // Logic: Lấy record mà user là requester HOẶC user là addressee, VÀ status là ACCEPTED
    @Query("SELECT f FROM Friendship f " +
           "WHERE (f.requester.id = :userId OR f.addressee.id = :userId) " +
           "AND f.status = 'ACCEPTED'")
    Page<Friendship> findAllAcceptedFriends(@Param("userId") Long userId, Pageable pageable);
    
    // 3.1 Lấy danh sách bạn bè dạng List (không phân trang - dùng cho dropdown mời bạn)
    @Query("SELECT f FROM Friendship f " +
            "WHERE (f.requester.id = :userId OR f.addressee.id = :userId) " +
            "AND f.status = 'ACCEPTED'")
    List<Friendship> findAllAcceptedFriendsList(@Param("userId") Long userId);

    // 4. Lấy danh sách lời mời kết bạn ĐANG CHỜ TÔI DUYỆT (Tôi là addressee)
    @Query("SELECT f FROM Friendship f " +
           "WHERE f.addressee.id = :userId AND f.status = 'PENDING'")
    Page<Friendship> findIncomingRequests(@Param("userId") Long userId, Pageable pageable);

    // 5. Lấy danh sách lời mời TÔI ĐÃ GỬI đi (Tôi là requester)
    @Query("SELECT f FROM Friendship f " +
           "WHERE f.requester.id = :userId AND f.status = 'PENDING'")
    Page<Friendship> findOutgoingRequests(@Param("userId") Long userId, Pageable pageable);
}