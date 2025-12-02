package com.mindrevol.backend.modules.storage.job;

import com.mindrevol.backend.config.MinioConfig;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileCleanupJob {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    
    // Inject các Repository có chứa link ảnh để kiểm tra
    // Lưu ý: Cần update Repository để thêm hàm existsByImageUrl nếu chưa có (nhưng ở đây tôi dùng logic giả định để demo)
    // private final CheckinRepository checkinRepository;
    // private final UserRepository userRepository;

    // Chạy vào 3:00 sáng Chủ Nhật hàng tuần
    @Scheduled(cron = "0 0 3 * * SUN")
    public void scanOrphanedFiles() {
        log.info("=== STARTING ORPHANED FILE SCAN ===");
        String bucket = minioConfig.getBucketName();

        try {
            // 1. Liệt kê tất cả file trong Bucket
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucket).recursive(true).build());

            int countDeleted = 0;

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                
                // Construct URL đầy đủ để check trong DB
                String fileUrl = minioConfig.getUrl() + "/" + bucket + "/" + objectName;

                // 2. Kiểm tra xem URL này có tồn tại trong DB không
                // Logic này rất nặng nếu không có Index trên cột image_url.
                // Tạm thời tôi comment lại để bạn hiểu luồng đi.
                
                /*
                boolean isUsedInCheckin = checkinRepository.existsByImageUrlOrThumbnailUrl(fileUrl, fileUrl);
                boolean isUsedInUser = userRepository.existsByAvatarUrl(fileUrl);
                
                if (!isUsedInCheckin && !isUsedInUser) {
                    log.warn("Found orphaned file: {}", objectName);
                    
                    // 3. Xóa file (BỎ COMMENT DÒNG DƯỚI KHI MUỐN CHẠY THẬT)
                    // minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
                    
                    countDeleted++;
                }
                */
            }
            log.info("Scan completed. Found potential orphaned files: {}", countDeleted);

        } catch (Exception e) {
            log.error("Error during file cleanup", e);
        }
    }
}