package com.mindrevol.backend.modules.store.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.store.entity.StoreItem;
import com.mindrevol.backend.modules.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // Lấy danh sách hàng hóa để hiển thị lên App
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<StoreItem>>> getItems() {
        return ResponseEntity.ok(ApiResponse.success(storeService.getActiveItems()));
    }

    // Mua hàng (Client gửi itemCode, ví dụ "FREEZE_1")
    @PostMapping("/buy/{itemCode}")
    public ResponseEntity<ApiResponse<Void>> buyItem(@PathVariable String itemCode) {
        Long userId = SecurityUtils.getCurrentUserId();
        storeService.buyItem(userId, itemCode);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}