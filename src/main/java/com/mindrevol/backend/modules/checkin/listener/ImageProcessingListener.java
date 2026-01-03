package com.mindrevol.backend.modules.checkin.listener;

import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageProcessingListener {

    private final CheckinRepository checkinRepository;
    private final FileStorageService fileStorageService;

    @Async("imageTaskExecutor") 
    @EventListener
    @Transactional
    public void handleImageProcessing(CheckinSuccessEvent event) {
        // event.getCheckinId() bây giờ là String -> Hết lỗi findById
        log.info("Bắt đầu xử lý ảnh cho Checkin ID: {}", event.getCheckinId());

        try {
            Checkin checkin = checkinRepository.findById(event.getCheckinId()).orElse(null);
            
            if (checkin == null || checkin.getImageUrl() == null || checkin.getImageUrl().isEmpty()) {
                return;
            }

            // Logic xử lý ảnh giữ nguyên
            InputStream originalImageStream = fileStorageService.downloadFile(checkin.getImageUrl());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Thumbnails.of(originalImageStream)
                    .size(400, 400)
                    .outputQuality(0.8)
                    .toOutputStream(os);
            
            byte[] thumbnailData = os.toByteArray();
            InputStream thumbnailStream = new ByteArrayInputStream(thumbnailData);
            
            String thumbName = UUID.randomUUID() + "_thumb.jpg";

            String thumbnailUrl = fileStorageService.uploadStream(
                    thumbnailStream, 
                    thumbName, 
                    "image/jpeg", 
                    thumbnailData.length
            );

            checkin.setThumbnailUrl(thumbnailUrl);
            checkinRepository.save(checkin);

            log.info("Đã tạo Thumbnail thành công: {}", thumbnailUrl);

        } catch (Exception e) {
            log.error("Lỗi xử lý ảnh cho Checkin {}: {}", event.getCheckinId(), e.getMessage());
        }
    }
}