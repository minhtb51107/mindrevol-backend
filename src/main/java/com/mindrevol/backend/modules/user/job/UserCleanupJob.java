package com.mindrevol.backend.modules.user.job;

import com.mindrevol.backend.modules.storage.service.FileStorageService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCleanupJob {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    // Chạy lúc 4:00 AM mỗi ngày
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void cleanupDeletedUsers() {
        log.info("Starting User Cleanup Job...");

        // 1. Tìm các user đã xóa mềm quá 30 ngày
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(30);
        List<User> usersToDelete = userRepository.findUsersReadyForHardDelete(cutoffDate);

        log.info("Found {} users ready for hard delete.", usersToDelete.size());

        for (User user : usersToDelete) {
            try {
                // 2. Xóa Avatar trên MinIO/Cloud (Dọn dẹp tài nguyên)
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    fileStorageService.deleteFile(user.getAvatarUrl());
                    log.info("Deleted avatar for user {}", user.getId());
                }

                // 3. Xóa vĩnh viễn khỏi Database
                // Nhờ Migration 027, các dữ liệu con (checkin, comment...) sẽ tự động xóa theo
                userRepository.hardDeleteUser(user.getId());
                
                log.info("Hard deleted user ID: {}", user.getId());

            } catch (Exception e) {
                log.error("Failed to hard delete user ID: {}", user.getId(), e);
            }
        }

        log.info("User Cleanup Job completed.");
    }
}