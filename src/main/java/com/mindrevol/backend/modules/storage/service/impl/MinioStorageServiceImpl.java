package com.mindrevol.backend.modules.storage.service.impl;

import com.mindrevol.backend.config.MinioConfig;
import com.mindrevol.backend.modules.storage.service.FileStorageService;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public String uploadFile(MultipartFile file) {
        return uploadToMinio(file, false);
    }

    @Override
    public String uploadThumbnail(MultipartFile file) {
        return uploadToMinio(file, true);
    }

    private String uploadToMinio(MultipartFile file, boolean isThumbnail) {
        try {
            String bucketName = minioConfig.getBucketName();
            
            // 1. Đảm bảo bucket tồn tại
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket '{}' created successfully", bucketName);
            }

            // 2. Tạo tên file duy nhất
            // Format: uuid_thumb.jpg hoặc uuid_filename.png
            String fileName = UUID.randomUUID() + (isThumbnail ? "_thumb.jpg" : "_" + file.getOriginalFilename());
            
            InputStream inputStream;
            long size;
            String contentType;

            if (isThumbnail) {
                // Logic NÉN ẢNH: Resize về 400x400, giảm chất lượng còn 80%, convert sang JPG
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Thumbnails.of(file.getInputStream())
                        .size(400, 400)
                        .outputQuality(0.8)
                        .toOutputStream(os);
                byte[] data = os.toByteArray();
                inputStream = new ByteArrayInputStream(data);
                size = data.length;
                contentType = "image/jpeg"; 
            } else {
                // Ảnh gốc
                inputStream = file.getInputStream();
                size = file.getSize();
                contentType = file.getContentType();
            }

            // 3. Upload lên MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());

            // 4. Trả về URL public
            // Lưu ý: minioConfig.getUrl() cần có dạng "http://localhost:9000" (không có dấu / ở cuối)
            return String.format("%s/%s/%s", minioConfig.getUrl(), bucketName, fileName);

        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new RuntimeException("Could not upload file: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;

        try {
            String bucketName = minioConfig.getBucketName();
            
            // Logic parse tên file từ URL
            // URL dạng: http://localhost:9000/bucket-name/ten-file.jpg
            // Ta cần lấy phần cuối cùng: "ten-file.jpg"
            String objectName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            
            log.info("Deleted file from MinIO: {}", objectName);

        } catch (Exception e) {
            // Chỉ log lỗi, không throw exception để tránh làm crash luồng chính (ví dụ khi xóa user)
            log.error("Error deleting file from MinIO: URL={}", fileUrl, e);
        }
    }
}