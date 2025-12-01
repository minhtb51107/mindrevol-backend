package com.mindrevol.backend.modules.checkin.entity;

public enum ReactionType {
    HEART,  // Yêu thích
    CLAP,   // Vỗ tay (Chúc mừng)
    HUG,    // Ôm (An ủi - Dùng khi status = FAILED)
    FIRE,   // Cháy quá (Dùng khi status = COMEBACK)
    VOICE   // Ghi âm lời động viên (Mới)
}