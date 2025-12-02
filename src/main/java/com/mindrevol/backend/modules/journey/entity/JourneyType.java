package com.mindrevol.backend.modules.journey.entity;

public enum JourneyType {
    HABIT,      // Thói quen: Cần kỷ luật, reset streak, nhắc nhở
    ROADMAP,    // Lộ trình: Có task rõ ràng, kỷ luật
    MEMORIES,   // Kỷ niệm: Du lịch, Couple, Nhóm bạn -> Vui là chính
    PROJECT,    // Dự án: Làm việc nhóm -> Tập trung vào Task, không quan tâm Streak
    CHALLENGE   // Thử thách: Có thể dùng cho game hoặc thi đua ngắn hạn
}