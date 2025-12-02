package com.mindrevol.backend.modules.journey.entity;

public enum InteractionType {
    PRIVATE_REPLY,   // Kiểu Locket: Reply sẽ nhảy vào tin nhắn riêng (1-1)
    GROUP_DISCUSS,   // Kiểu Facebook: Reply sẽ hiện dưới bài post cho cả nhóm thấy
    RESTRICTED       // Chỉ thả Reaction, không cho nhắn (Dành cho các channel thông báo 1 chiều)
}