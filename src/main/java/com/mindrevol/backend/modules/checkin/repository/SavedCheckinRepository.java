package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.SavedCheckin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedCheckinRepository extends JpaRepository<SavedCheckin, String> {
    
    // Kiểm tra xem user đã lưu bài này chưa
    boolean existsByUserIdAndCheckinId(String userId, String checkinId);
    
    // Xóa bài lưu
    void deleteByUserIdAndCheckinId(String userId, String checkinId);

    // Lấy danh sách các Checkin mà user đã lưu, sắp xếp theo thời gian lưu mới nhất
    @Query("SELECT sc.checkin FROM SavedCheckin sc WHERE sc.user.id = :userId ORDER BY sc.createdAt DESC")
    Page<Checkin> findSavedCheckinsByUserId(@Param("userId") String userId, Pageable pageable);
}