package com.mindrevol.backend.modules.box.repository;

import com.mindrevol.backend.modules.box.entity.BoxMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoxMemberRepository extends JpaRepository<BoxMember, String> {

    // Kiểm tra xem user có nằm trong box này không
    boolean existsByBoxIdAndUserId(String boxId, String userId);

    // Lấy thông tin member của user trong box
    Optional<BoxMember> findByBoxIdAndUserId(String boxId, String userId);
    
    // Đếm số lượng thành viên trong Box
    long countByBoxId(String boxId);
    
    // [ĐÃ THÊM MỚI]: Lấy danh sách thành viên trong Box có phân trang
    Page<BoxMember> findByBoxId(String boxId, Pageable pageable);
    
    // Lấy tất cả user_id trong một box (Phục vụ cho việc cấp quyền xem Journey ở bước sau)
    @Query("SELECT bm.user.id FROM BoxMember bm WHERE bm.box.id = :boxId")
    List<String> findAllUserIdsByBoxId(@Param("boxId") String boxId);
}