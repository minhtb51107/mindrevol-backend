package com.mindrevol.backend.modules.storage.service.impl;

import com.mindrevol.backend.config.MinioConfig;
import com.mindrevol.backend.modules.storage.service.FileStorageService;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        try {
            // Tạo tên file duy nhất: uuid_filename
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            return uploadToMinio(file.getInputStream(), fileName, file.getContentType(), file.getSize());
        } catch (Exception e) {
            throw new RuntimeException("Error getting stream from file", e);
        }
    }

    @Override
    public String uploadStream(InputStream inputStream, String fileName, String contentType, long size) {
        return uploadToMinio(inputStream, fileName, contentType, size);
    }

    private String uploadToMinio(InputStream inputStream, String fileName, String contentType, long size) {
        try {
            String bucketName = minioConfig.getBucketName();
            
            // 1. Đảm bảo bucket tồn tại (Có thể cache check này để tối ưu hơn nữa)
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // 2. Upload lên MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());

            // 3. Trả về URL public
            return String.format("%s/%s/%s", minioConfig.getUrl(), bucketName, fileName);

        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new RuntimeException("Could not upload file: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String fileUrl) {
        try {
            String bucketName = minioConfig.getBucketName();
            String objectName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("Error downloading file from MinIO", e);
            throw new RuntimeException("Could not download file");
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;
        try {
            String bucketName = minioConfig.getBucketName();
            String objectName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error deleting file", e);
        }
    }
}