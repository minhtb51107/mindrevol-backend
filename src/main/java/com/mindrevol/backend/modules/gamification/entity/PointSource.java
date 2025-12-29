package com.mindrevol.backend.modules.gamification.entity;

public enum PointSource {
    CHECKIN,        // Cộng điểm khi check-in
    SHOP_PURCHASE,  // Trừ điểm khi mua hàng
    ADMIN_ADJUST,   // Admin tặng/trừ
    BONUS,          // Thưởng thêm
    TOPUP           // [MỚI] Nạp tiền
}