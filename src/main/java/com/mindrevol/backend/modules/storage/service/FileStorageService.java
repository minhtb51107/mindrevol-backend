package com.mindrevol.backend.modules.storage.service;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface FileStorageService {
    
    // Upload ảnh trả về kết quả đầy đủ (URL + ID)
    FileUploadResult uploadFile(MultipartFile file, String folder);
    
    // Giữ lại hàm cũ (Overload) để tương thích ngược nếu cần
    default String uploadFile(MultipartFile file) {
        return uploadFile(file, "mindrevol_uploads").getUrl();
    }
    
    String uploadStream(InputStream inputStream, String fileName, String contentType, long size);
    
    InputStream downloadFile(String fileUrl);
    
    void deleteFile(String fileId);

    @Data
    @Builder
    class FileUploadResult {
        private String url;
        private String fileId;
        private String thumbnailUrl;
    }
}