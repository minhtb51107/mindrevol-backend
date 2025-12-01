package com.mindrevol.backend.modules.storage.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.storage.service.FileStorageService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.uploadFile(file);
        
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(200)           // <--- Thêm dòng này
                .message("Upload success")
                .data(fileUrl)         // <--- Đổi .result() thành .data()
                .build());
    }
}