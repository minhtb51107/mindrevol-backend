package com.mindrevol.backend.modules.checkin.service;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SavedCheckinService {
    // Hàm toggle: Trả về true nếu vừa chuyển sang trạng thái LƯU, false nếu BỎ LƯU
    boolean toggleSaveCheckin(String userId, String checkinId);
    
    // Lấy danh sách bài đã lưu của người dùng
    Page<CheckinResponse> getMySavedCheckins(String userId, Pageable pageable);
}