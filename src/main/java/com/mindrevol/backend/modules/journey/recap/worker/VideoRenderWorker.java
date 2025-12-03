package com.mindrevol.backend.modules.journey.recap.worker;

import com.mindrevol.backend.modules.journey.recap.dto.VideoTask;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import com.mindrevol.backend.modules.storage.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoRenderWorker {

    private final RedissonClient redissonClient;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    private static final String VIDEO_QUEUE_NAME = "video_render_queue";
    
    // Video render rất nặng CPU, chỉ cho phép 1 luồng chạy tại 1 thời điểm để không sập server
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void startWorker() {
        executorService.submit(this::processQueue);
    }

    @PreDestroy
    public void stopWorker() {
        log.info("Stopping Video Worker...");
        executorService.shutdownNow();
    }

    private void processQueue() {
        RBlockingQueue<VideoTask> queue = redissonClient.getBlockingQueue(VIDEO_QUEUE_NAME);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                VideoTask task = queue.take();
                handleVideoTask(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing video task", e);
            }
        }
    }

    private void handleVideoTask(VideoTask task) {
        Path tempDir = null;
        try {
            log.info("Start rendering video for user {}", task.getUserId());
            
            // 1. Tạo thư mục tạm
            tempDir = Files.createTempDirectory("recap_" + UUID.randomUUID());
            
            // 2. Tải tất cả ảnh về
            List<String> imagePaths = new ArrayList<>();
            for (int i = 0; i < task.getImageUrls().size(); i++) {
                String url = task.getImageUrls().get(i);
                try (InputStream in = fileStorageService.downloadFile(url)) {
                    Path imgPath = tempDir.resolve(String.format("img_%03d.jpg", i));
                    Files.copy(in, imgPath, StandardCopyOption.REPLACE_EXISTING);
                    imagePaths.add(imgPath.toString());
                }
            }

            if (imagePaths.isEmpty()) {
                throw new RuntimeException("No images to render");
            }

            // 3. Gọi FFmpeg để ghép video
            // Lệnh: ffmpeg -framerate 1/2 -i img_%03d.jpg -c:v libx264 -r 30 -pix_fmt yuv420p output.mp4
            // (Mỗi ảnh hiện 2 giây)
            String outputPath = tempDir.resolve("output.mp4").toString();
            
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y", // Overwrite output
                    "-framerate", "0.5", // 1 ảnh = 2 giây
                    "-i", tempDir.resolve("img_%03d.jpg").toString(),
                    "-c:v", "libx264",
                    "-r", "30",
                    "-pix_fmt", "yuv420p",
                    outputPath
            );
            
            // Redirect log FFmpeg ra console để debug
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
            }

            // 4. Upload Video lên MinIO
            File videoFile = new File(outputPath);
            String videoUrl;
            try (FileInputStream fis = new FileInputStream(videoFile)) {
                videoUrl = fileStorageService.uploadStream(
                        fis, 
                        "recap_" + System.currentTimeMillis() + ".mp4", 
                        "video/mp4", 
                        videoFile.length()
                );
            }

            // 5. Gửi thông báo cho User
            notificationService.sendAndSaveNotification(
                    task.getUserId(),
                    null, // System sender
                    NotificationType.SYSTEM,
                    "Video Recap đã sẵn sàng! 🎬",
                    "Video tổng kết hành trình của bạn đã được tạo thành công.",
                    task.getJourneyId().toString(),
                    videoUrl // Bấm vào notification sẽ mở video này
            );
            
            log.info("Video rendered and uploaded: {}", videoUrl);

        } catch (Exception e) {
            log.error("Failed to render video for user {}", task.getUserId(), e);
            // Có thể retry nếu cần
        } finally {
            // 6. Dọn dẹp thư mục tạm
            if (tempDir != null) {
                deleteDirectoryRecursively(tempDir.toFile());
            }
        }
    }

    private void deleteDirectoryRecursively(File file) {
        if (file.isDirectory()) {
            for (File c : file.listFiles()) deleteDirectoryRecursively(c);
        }
        file.delete();
    }
}