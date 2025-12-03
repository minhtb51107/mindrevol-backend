package com.mindrevol.backend.modules.user.job;

import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.entity.JourneyRole;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.storage.service.FileStorageService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCleanupJob {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository journeyParticipantRepository;

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
                // --- BƯỚC MỚI: Xử lý các Journey do User này làm chủ ---
                handleJourneySuccession(user);
                // ------------------------------------------------------

                // 2. Xóa Avatar trên MinIO/Cloud
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    fileStorageService.deleteFile(user.getAvatarUrl());
                    log.info("Deleted avatar for user {}", user.getId());
                }

                // 3. Xóa vĩnh viễn khỏi Database
                userRepository.hardDeleteUser(user.getId());
                log.info("Hard deleted user ID: {}", user.getId());

            } catch (Exception e) {
                log.error("Failed to hard delete user ID: {}", user.getId(), e);
            }
        }

        log.info("User Cleanup Job completed.");
    }

    // Logic chuyển giao quyền lực hoặc giải tán nhóm
    private void handleJourneySuccession(User userToDelete) {
        List<Journey> ownedJourneys = journeyRepository.findByCreatorId(userToDelete.getId());
        
        for (Journey journey : ownedJourneys) {
            log.info("Processing succession for Journey: {}", journey.getId());
            
            // Tìm tất cả thành viên CÒN LẠI của nhóm (trừ người sắp xóa)
            List<JourneyParticipant> remainingParticipants = journeyParticipantRepository.findAllByJourneyId(journey.getId())
                    .stream()
                    .filter(p -> !p.getUser().getId().equals(userToDelete.getId()))
                    .sorted(Comparator.comparing(JourneyParticipant::getJoinedAt)) // Người vào sớm nhất lên làm chủ
                    .toList();

            if (remainingParticipants.isEmpty()) {
                // Nhóm không còn ai khác -> Xóa nhóm luôn
                log.info("Journey {} has no other members. Deleting.", journey.getId());
                journeyRepository.delete(journey);
            } else {
                // Chuyển quyền cho người lâu năm nhất
                JourneyParticipant heir = remainingParticipants.get(0);
                
                // 1. Set Creator mới cho Journey
                journey.setCreator(heir.getUser());
                journeyRepository.save(journey);
                
                // 2. Nâng cấp quyền của người thừa kế lên ADMIN
                heir.setRole(JourneyRole.ADMIN);
                journeyParticipantRepository.save(heir);
                
                log.info("Transferred ownership of Journey {} to User {}", journey.getId(), heir.getUser().getId());
            }
        }
    }
}