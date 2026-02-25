package com.mindrevol.backend.modules.box.repository;

import com.mindrevol.backend.modules.box.entity.Box;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BoxRepository extends JpaRepository<Box, String> {

    // Lấy danh sách Box mà User là thành viên (Sắp xếp theo thời gian tham gia mới nhất)
    @Query("SELECT b FROM Box b JOIN b.members bm WHERE bm.user.id = :userId")
    Page<Box> findBoxesByUserId(@Param("userId") String userId, Pageable pageable);
    
}