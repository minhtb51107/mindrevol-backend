package com.mindrevol.backend.modules.storage.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface FileStorageService {
    // 1. Upload ảnh gốc (Giữ nguyên cho CheckinService dùng - mặc định folder chung)
    String uploadFile(MultipartFile file);
    
    // 2. [MỚI] Upload ảnh có chỉ định folder (Dùng cho User Profile: "avatars/...")
    String uploadFile(MultipartFile file, String folder);
    
    // Upload ảnh từ luồng dữ liệu (Dùng cho Worker)
    String uploadStream(InputStream inputStream, String fileName, String contentType, long size);
    
    // Tải ảnh về
    InputStream downloadFile(String fileUrl);
    
    void deleteFile(String fileUrl);
}