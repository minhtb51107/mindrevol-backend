package com.mindrevol.backend.modules.storage.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.backend.modules.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class ImageKitStorageServiceImpl implements FileStorageService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${imagekit.upload-url}")
    private String uploadUrl; // Thường là: https://upload.imagekit.io/api/v1/files/upload

    @Value("${imagekit.private-key}")
    private String privateKey;

    // URL quản lý file (Xóa) của ImageKit
    private static final String MANAGEMENT_API_URL = "https://api.imagekit.io/v1/files/";

    @Override
    public FileUploadResult uploadFile(MultipartFile file, String folder) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                fileName = UUID.randomUUID().toString();
            }
            // Làm sạch tên file
            fileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");

            // Nếu folder null thì lấy mặc định
            String targetFolder = (folder != null && !folder.isEmpty()) ? folder : "mindrevol_uploads";

            return uploadToImageKit(file.getBytes(), fileName, targetFolder);
        } catch (IOException e) {
            log.error("Failed to read file bytes", e);
            throw new RuntimeException("Error reading file content", e);
        }
    }

    // Giữ tương thích cho interface cũ (nếu có)
    @Override
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, "mindrevol_uploads").getUrl();
    }

    @Override
    public String uploadStream(InputStream inputStream, String fileName, String contentType, long size) {
        try {
            return uploadToImageKit(inputStream.readAllBytes(), fileName, "mindrevol_processed").getUrl();
        } catch (IOException e) {
            log.error("Failed to read input stream", e);
            throw new RuntimeException("Error uploading stream", e);
        }
    }

    private FileUploadResult uploadToImageKit(byte[] fileBytes, String fileName, String folder) {
        try {
            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            
            body.add("file", fileResource);
            body.add("fileName", fileName);
            body.add("useUniqueFileName", "true");
            body.add("folder", folder); 

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode root = objectMapper.readTree(response.getBody());
                
                return FileUploadResult.builder()
                        .url(root.path("url").asText())
                        .fileId(root.path("fileId").asText()) 
                        .thumbnailUrl(root.path("thumbnailUrl").asText())
                        .build();
            } else {
                throw new RuntimeException("ImageKit upload failed with status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("ImageKit upload error", e);
            throw new RuntimeException("Failed to upload to ImageKit: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String fileUrl) {
        try {
            return new URL(fileUrl).openStream();
        } catch (IOException e) {
            log.error("Failed to download file: {}", fileUrl, e);
            throw new RuntimeException("Could not download file", e);
        }
    }

    // [REAL IMPLEMENTATION] Xóa file trên ImageKit
    @Override
    public void deleteFile(String fileId) {
        if (fileId == null || fileId.isEmpty()) return;

        try {
            // API Endpoint: DELETE https://api.imagekit.io/v1/files/{fileId}
            String deleteUrl = MANAGEMENT_API_URL + fileId;

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, requestEntity, Void.class);
            
            log.info("✅ Deleted file from ImageKit successfully: {}", fileId);

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("File ID {} not found on ImageKit (maybe already deleted). Ignoring.", fileId);
        } catch (Exception e) {
            log.error("❌ Failed to delete file {} from ImageKit", fileId, e);
            // Không throw exception để tránh làm rollback transaction DB chính nếu đây là tác vụ dọn dẹp
        }
    }

    // Helper tạo Header Auth (Dùng chung cho Upload và Delete)
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        // Basic Auth: base64(privateKey + ":")
        String auth = privateKey + ":"; 
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;
    }
}