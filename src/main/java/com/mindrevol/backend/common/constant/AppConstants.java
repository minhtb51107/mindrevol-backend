package com.mindrevol.backend.common.constant;

public class AppConstants {
    // --- GIỚI HẠN GÓI FREE (MVP SURVIVAL MODE) ---
    
    // Mỗi người chỉ được tạo tối đa 1 Hành trình Active
    // Lý do: Để họ trân trọng hành trình đó và tập trung. Muốn tạo thêm -> Mua Premium.
    public static final int LIMIT_OWNED_JOURNEYS_FREE = 3;

    // Mỗi nhóm chỉ tối đa 10 thành viên
    // Lý do: Nhóm nhỏ (Cluster) dễ quản lý và tương tác cao hơn. Muốn mở rộng -> Trưởng nhóm phải mua gói.
    public static final int LIMIT_MEMBERS_PER_JOURNEY_FREE = 10;

    // Giới hạn dung lượng ảnh upload (MB) để tiết kiệm ổ cứng MinIO
    public static final long MAX_IMAGE_SIZE_MB = 5; 
}