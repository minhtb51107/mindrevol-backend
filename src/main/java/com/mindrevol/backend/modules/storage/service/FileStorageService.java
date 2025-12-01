package com.mindrevol.backend.modules.storage.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    // Upload ảnh gốc
    String uploadFile(MultipartFile file);
    
    // Upload ảnh nén (Thumbnail) - Hàm mới
    String uploadThumbnail(MultipartFile file);
    
    void deleteFile(String fileUrl);
}