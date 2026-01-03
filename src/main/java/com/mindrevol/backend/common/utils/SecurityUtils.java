package com.mindrevol.backend.common.utils;

import com.mindrevol.backend.common.exception.BadRequestException; // Hoặc UnauthorizedException nếu bạn có
import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    // Hàm này sẽ trả về ID của người đang đăng nhập
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new BadRequestException("Người dùng chưa đăng nhập");
        }

        Object principal = authentication.getPrincipal();

        // Vì trong UserDetailsServiceImpl bạn thường trả về chính Entity User
        if (principal instanceof User) {
            return ((User) principal).getId();
        }

        throw new BadRequestException("Không xác định được danh tính người dùng");
    }
    
    // Hàm lấy toàn bộ User Entity (nếu cần)
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new BadRequestException("Người dùng chưa đăng nhập");
    }
}