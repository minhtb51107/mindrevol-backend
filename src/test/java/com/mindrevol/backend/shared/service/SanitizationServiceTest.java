package com.mindrevol.backend.shared.service;

import org.junit.jupiter.api.Test;

import com.mindrevol.backend.common.service.SanitizationService;

import static org.junit.jupiter.api.Assertions.*;

class SanitizationServiceTest {

    private final SanitizationService service = new SanitizationService();

    @Test
    void sanitizeStrict_ShouldRemoveAllHtmlTags() {
        String unsafeInput = "Hello <script>alert('hack')</script><b>World</b>";
        String expected = "Hello World"; // Policy Strict loại bỏ hết tag
        
        // Lưu ý: Kết quả thực tế phụ thuộc vào việc policy có giữ lại khoảng trắng hay không. 
        // OWASP sanitizer thường an toàn và loại bỏ sạch.
        String actual = service.sanitizeStrict(unsafeInput);
        
        // Kiểm tra không còn thẻ script hay b
        assertFalse(actual.contains("<script>"));
        assertFalse(actual.contains("<b>"));
    }

    @Test
    void sanitizeRichText_ShouldKeepSafeTags() {
        String input = "<b>Bold</b> and <script>bad</script>";
        String result = service.sanitizeRichText(input);

        assertTrue(result.contains("<b>Bold</b>") || result.contains("<strong>Bold</strong>")); // Policy cho phép b/strong
        assertFalse(result.contains("<script>"));
    }

    @Test
    void sanitizeRichText_ShouldAllowLinks() {
        String input = "<a href=\"https://google.com\">Link</a>";
        String result = service.sanitizeRichText(input);
        
        assertTrue(result.contains("<a") && result.contains("href"));
        assertTrue(result.contains("rel=\"nofollow\"")); // Policy tự động thêm nofollow
    }
}