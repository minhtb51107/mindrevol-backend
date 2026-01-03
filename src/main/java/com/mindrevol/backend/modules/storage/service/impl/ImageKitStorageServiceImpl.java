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
    private String uploadUrl;

    @Value("${imagekit.private-key}")
    private String privateKey;

    // --- 1. Hàm cũ (Giữ nguyên để không lỗi code cũ) ---
    @Override
    public String uploadFile(MultipartFile file) {
        // Gọi hàm mới với folder mặc định
        return uploadFile(file, "mindrevol_uploads");
    }

    // --- 2. Hàm mới (Cho phép tùy chỉnh folder) ---
    @Override
    public String uploadFile(MultipartFile file, String folder) {
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

    @Override
    public String uploadStream(InputStream inputStream, String fileName, String contentType, long size) {
        try {
            return uploadToImageKit(inputStream.readAllBytes(), fileName, "mindrevol_processed");
        } catch (IOException e) {
            log.error("Failed to read input stream", e);
            throw new RuntimeException("Error uploading stream", e);
        }
    }

    // [HELPER] Hàm upload thực tế
    private String uploadToImageKit(byte[] fileBytes, String fileName, String folder) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            String auth = privateKey + ":"; 
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

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
            body.add("folder", folder); // Folder động

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String secureUrl = root.path("url").asText();
                log.info("Uploaded to ImageKit [{}]: {}", folder, secureUrl);
                return secureUrl;
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

    @Override
    public void deleteFile(String fileUrl) {
        log.warn("Skipping delete file on ImageKit (MVP mode): {}", fileUrl);
    }
}