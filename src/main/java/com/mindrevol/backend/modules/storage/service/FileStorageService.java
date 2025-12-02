package com.mindrevol.backend.modules.storage.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface FileStorageService {
    // Upload ảnh gốc
    String uploadFile(MultipartFile file);
    
    // Upload ảnh từ luồng dữ liệu (Dùng cho Worker sau khi nén xong)
    String uploadStream(InputStream inputStream, String fileName, String contentType, long size);
    
    // Tải ảnh về (để Worker xử lý)
    InputStream downloadFile(String fileUrl);
    
    void deleteFile(String fileUrl);
}