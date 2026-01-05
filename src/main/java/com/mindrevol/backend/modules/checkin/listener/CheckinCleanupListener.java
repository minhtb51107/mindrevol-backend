package com.mindrevol.backend.modules.checkin.listener;

import com.mindrevol.backend.modules.checkin.event.CheckinDeletedEvent;
import com.mindrevol.backend.modules.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckinCleanupListener {

    private final FileStorageService fileStorageService;

    /**
     * Xử lý việc xóa file trên Cloud.
     * @Async: Chạy trên một luồng riêng biệt để không làm user phải chờ.
     * phase = AFTER_COMMIT: Chỉ chạy khi DB đã xóa record thành công.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCheckinDeletedEvent(CheckinDeletedEvent event) {
        log.info("Transaction committed. Deleting file from Cloud: {}", event.getFileId());
        try {
            fileStorageService.deleteFile(event.getFileId());
        } catch (Exception e) {
            log.error("Failed to delete file {} from Cloud. Manual cleanup required.", event.getFileId(), e);
            // Trong hệ thống Enterprise, chỗ này sẽ đẩy vào "Dead Letter Queue" để retry sau.
        }
    }
}