package com.mindrevol.backend.modules.checkin.service.impl;

import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.SavedCheckin;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.checkin.repository.SavedCheckinRepository;
import com.mindrevol.backend.modules.checkin.service.SavedCheckinService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate; // [THÊM MỚI]
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set; // [THÊM MỚI]

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedCheckinServiceImpl implements SavedCheckinService {

    private final SavedCheckinRepository savedCheckinRepository;
    private final CheckinRepository checkinRepository;
    private final UserRepository userRepository;
    private final CheckinMapper checkinMapper;
    
    // [THÊM MỚI] Inject RedisTemplate để điều khiển Cache
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public boolean toggleSaveCheckin(String userId, String checkinId) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài đăng không tồn tại"));

        boolean isSavedNow;

        if (savedCheckinRepository.existsByUserIdAndCheckinId(userId, checkinId)) {
            // Đã lưu -> Bấm vào thì bỏ lưu
            savedCheckinRepository.deleteByUserIdAndCheckinId(userId, checkinId);
            log.info("User {} unsaved checkin {}", userId, checkinId);
            isSavedNow = false;
        } else {
            // Chưa lưu -> Bấm vào thì lưu
            User user = userRepository.getReferenceById(userId);
            SavedCheckin savedCheckin = SavedCheckin.builder()
                    .user(user)
                    .checkin(checkin)
                    .build();
            savedCheckinRepository.save(savedCheckin);
            log.info("User {} saved checkin {}", userId, checkinId);
            isSavedNow = true;
        }

        // [THÊM MỚI] Xóa Cache Feed để lần lấy feed tiếp theo cập nhật trạng thái mới nhất
        clearUserFeedCache(userId);

        return isSavedNow;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CheckinResponse> getMySavedCheckins(String userId, Pageable pageable) {
        return savedCheckinRepository.findSavedCheckinsByUserId(userId, pageable)
                .map(checkin -> {
                    CheckinResponse response = checkinMapper.toResponse(checkin);
                    response.setSaved(true); 
                    return response;
                }); 
    }

    // [THÊM MỚI] Hàm xóa cache bảng tin của user
    private void clearUserFeedCache(String userId) {
        try {
            // Cú pháp tìm key cache khớp với cấu trúc trong FeedServiceImpl
            String pattern = "feed:unified:" + userId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.error("Lỗi xóa cache feed khi lưu bài viết: {}", e.getMessage());
        }
    }
}