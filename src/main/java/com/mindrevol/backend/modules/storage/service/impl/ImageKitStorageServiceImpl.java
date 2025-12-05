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
@Primary // Đánh dấu đây là Service lưu trữ chính
@RequiredArgsConstructor
@Slf4j
public class ImageKitStorageServiceImpl implements FileStorageService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${imagekit.upload-url}")
    private String uploadUrl;

    @Value("${imagekit.private-key}")
    private String privateKey;

    @Override
    public String uploadFile(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                fileName = UUID.randomUUID().toString();
            }
            // Loại bỏ khoảng trắng để URL đẹp hơn
            fileName = fileName.replaceAll("\\s+", "_");

            return uploadToImageKit(file.getBytes(), fileName);
        } catch (IOException e) {
            log.error("Failed to read file bytes", e);
            throw new RuntimeException("Error reading file content", e);
        }
    }

    @Override
    public String uploadStream(InputStream inputStream, String fileName, String contentType, long size) {
        try {
            return uploadToImageKit(inputStream.readAllBytes(), fileName);
        } catch (IOException e) {
            log.error("Failed to read input stream", e);
            throw new RuntimeException("Error uploading stream", e);
        }
    }

    private String uploadToImageKit(byte[] fileBytes, String fileName) {
        try {
            // 1. Tạo Header Auth (Basic Auth với Private Key)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            String auth = privateKey + ":"; // Password để trống
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            // 2. Tạo Body (File + Các tham số)
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Đóng gói byte[] thành Resource để RestTemplate hiểu là file upload
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            
            body.add("file", fileResource);
            body.add("fileName", fileName);
            body.add("useUniqueFileName", "true"); // Để ImageKit tự thêm ký tự ngẫu nhiên tránh trùng
            body.add("folder", "mindrevol_uploads"); // Gom vào 1 thư mục cho gọn

            // 3. Gửi Request
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                // 4. Parse kết quả để lấy URL
                JsonNode root = objectMapper.readTree(response.getBody());
                String secureUrl = root.path("url").asText();
                log.info("Uploaded to ImageKit: {}", secureUrl);
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
        // Với MVP và gói Free dung lượng lớn, ta tạm bỏ qua việc xóa trên Cloud để giảm độ phức tạp.
        // Chỉ cần xóa link trong DB là User không thấy nữa.
        log.warn("Skipping delete file on ImageKit (MVP mode): {}", fileUrl);
    }
}