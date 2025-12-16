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

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
           "WHERE (f.requester.id = :u1 AND f.addressee.id = :u2) " +
           "OR (f.requester.id = :u2 AND f.addressee.id = :u1)")
    boolean existsByUsers(@Param("u1") Long userId1, @Param("u2") Long userId2);

    @Query("SELECT f FROM Friendship f " +
           "WHERE (f.requester.id = :u1 AND f.addressee.id = :u2) " +
           "OR (f.requester.id = :u2 AND f.addressee.id = :u1)")
    Optional<Friendship> findByUsers(@Param("u1") Long userId1, @Param("u2") Long userId2);

    // --- SỬA LỖI TẠI ĐÂY ---
    // Thêm JOIN FETCH để lấy luôn thông tin User (requester và addressee)
    @Query(value = "SELECT f FROM Friendship f " +
                   "JOIN FETCH f.requester JOIN FETCH f.addressee " +
                   "WHERE (f.requester.id = :userId OR f.addressee.id = :userId) " +
                   "AND f.status = 'ACCEPTED'",
           countQuery = "SELECT COUNT(f) FROM Friendship f " +
                        "WHERE (f.requester.id = :userId OR f.addressee.id = :userId) " +
                        "AND f.status = 'ACCEPTED'")
    Page<Friendship> findAllAcceptedFriends(@Param("userId") Long userId, Pageable pageable);
    // -----------------------

    // Hàm lấy List (Dùng cho Export/Fanout - Đã có JOIN FETCH)
    @Query("SELECT f FROM Friendship f " +
           "JOIN FETCH f.requester JOIN FETCH f.addressee " +
           "WHERE (f.requester.id = :userId OR f.addressee.id = :userId) AND f.status = 'ACCEPTED'")
    List<Friendship> findAllAcceptedFriendsList(@Param("userId") Long userId);

    // Hàm lấy lời mời nhận được (Đã có JOIN FETCH)
    @Query(value = "SELECT f FROM Friendship f JOIN FETCH f.requester " +
                   "WHERE f.addressee.id = :userId AND f.status = :status",
           countQuery = "SELECT COUNT(f) FROM Friendship f WHERE f.addressee.id = :userId AND f.status = :status")
    Page<Friendship> findIncomingRequests(@Param("userId") Long userId, @Param("status") FriendshipStatus status, Pageable pageable);

    // Hàm lấy lời mời đã gửi (Đã có JOIN FETCH)
    @Query(value = "SELECT f FROM Friendship f JOIN FETCH f.addressee " +
                   "WHERE f.requester.id = :userId AND f.status = 'PENDING'",
           countQuery = "SELECT COUNT(f) FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'PENDING'")
    Page<Friendship> findOutgoingRequests(@Param("userId") Long userId, Pageable pageable);
}