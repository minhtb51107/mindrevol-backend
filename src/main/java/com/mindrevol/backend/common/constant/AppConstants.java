package com.mindrevol.backend.common.constant;

public class AppConstants {

    // --- GIỚI HẠN SỐ LƯỢNG HÀNH TRÌNH ACTIVE (Mỗi người được tham gia bao nhiêu cái cùng lúc) ---
    public static final int MAX_ACTIVE_JOURNEYS_FREE = 5;  // Đủ để tập trung: Học, Gym, Đọc sách
    public static final int MAX_ACTIVE_JOURNEYS_GOLD = 15; // Mở rộng cho người năng động

    // --- GIỚI HẠN THÀNH VIÊN TRONG 1 HÀNH TRÌNH (Dựa trên gói của Chủ Phòng) ---
    public static final int MAX_PARTICIPANTS_FREE = 10; // Nhóm bạn thân, gia đình (Intimate)
    public static final int MAX_PARTICIPANTS_GOLD = 50; // Lớp học, Team công ty, CLB nhỏ

    // --- GIỚI HẠN MEDIA ---
    public static final long MAX_IMAGE_SIZE_MB = 10; // 10MB để hỗ trợ cả Video ngắn
    public static final long MAX_VIDEO_SIZE_BYTES = 10 * 1024 * 1024;
    
 // Thêm các đường dẫn Storage
    public static final String STORAGE_CHECKIN_IMAGES = "checkins/";
    public static final String STORAGE_CHECKIN_VIDEOS = "checkins/videos/";
    
    // Giới hạn thời gian upload ảnh cũ (phút)
    public static final long ALLOWED_PHOTO_AGE_MINUTES = 30;
}