package com.mindrevol.backend.modules.user.repository;

import com.mindrevol.backend.modules.user.entity.Friendship;
import com.mindrevol.backend.modules.user.entity.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// [UUID] JpaRepository<Friendship, String>
@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, String> {

    // Kiểm tra xem ĐÃ TỪNG GỬI yêu cầu (dù Pending hay Accepted) giữa 2 người chưa (tránh spam gửi lại)
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
           "WHERE (f.requester.id = :u1 AND f.addressee.id = :u2) " +
           "OR (f.requester.id = :u2 AND f.addressee.id = :u1)")
    boolean existsByUsers(@Param("u1") String userId1, @Param("u2") String userId2);

    // Kiểm tra xem 2 người CÓ PHẢI LÀ BẠN BÈ HIỆN TẠI (status = ACCEPTED) hay không
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
           "WHERE ((f.requester.id = :userId1 AND f.addressee.id = :userId2) " +
           "OR (f.requester.id = :userId2 AND f.addressee.id = :userId1)) " +
           "AND f.status = 'ACCEPTED'")
    boolean isFriend(@Param("userId1") String userId1, @Param("userId2") String userId2);

    // Lấy đối tượng Friendship giữa 2 người (để thao tác Accept/Reject/Delete)
    @Query("SELECT f FROM Friendship f " +
           "WHERE (f.requester.id = :u1 AND f.addressee.id = :u2) " +
           "OR (f.requester.id = :u2 AND f.addressee.id = :u1)")
    Optional<Friendship> findByUsers(@Param("u1") String userId1, @Param("u2") String userId2);

    // Phân trang danh sách BẠN BÈ của một người (trạng thái ACCEPTED)
    @Query(value = "SELECT f FROM Friendship f " +
                   "JOIN FETCH f.requester JOIN FETCH f.addressee " +
                   "WHERE (f.requester.id = :userId OR f.addressee.id = :userId) " +
                   "AND f.status = 'ACCEPTED'",
           countQuery = "SELECT COUNT(f) FROM Friendship f " +
                        "WHERE (f.requester.id = :userId OR f.addressee.id = :userId) " +
                        "AND f.status = 'ACCEPTED'")
    Page<Friendship> findAllAcceptedFriends(@Param("userId") String userId, Pageable pageable);

    // Trả về full danh sách bạn bè (Không phân trang, dùng cho các logic tính toán nội bộ)
    @Query("SELECT f FROM Friendship f " +
           "JOIN FETCH f.requester JOIN FETCH f.addressee " +
           "WHERE (f.requester.id = :userId OR f.addressee.id = :userId) AND f.status = 'ACCEPTED'")
    List<Friendship> findAllAcceptedFriendsList(@Param("userId") String authorId);

    // Lấy danh sách LỜI MỜI GỬI ĐẾN (Incoming requests) chờ được phản hồi
    @Query(value = "SELECT f FROM Friendship f JOIN FETCH f.requester " +
                   "WHERE f.addressee.id = :userId AND f.status = :status",
           countQuery = "SELECT COUNT(f) FROM Friendship f WHERE f.addressee.id = :userId AND f.status = :status")
    Page<Friendship> findIncomingRequests(@Param("userId") String userId, @Param("status") FriendshipStatus status, Pageable pageable);

    // Lấy danh sách LỜI MỜI ĐÃ GỬI ĐI (Outgoing requests) đang chờ bên kia chấp nhận
    @Query(value = "SELECT f FROM Friendship f JOIN FETCH f.addressee " +
                   "WHERE f.requester.id = :userId AND f.status = 'PENDING'",
           countQuery = "SELECT COUNT(f) FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'PENDING'")
    Page<Friendship> findOutgoingRequests(@Param("userId") String userId, Pageable pageable);

    // Xóa mối quan hệ bằng mã ID tương ứng
    void deleteByRequesterIdAndAddresseeId(String blockedId, String currentUserId);
    
    // Đếm tổng số lượng bạn bè hiện có của một người dùng
    @Query("SELECT COUNT(f) FROM Friendship f " +
            "WHERE (f.requester.id = :userId OR f.addressee.id = :userId) " +
            "AND f.status = 'ACCEPTED'")
     long countByUserIdAndStatusAccepted(@Param("userId") String userId);
}