package com.mindrevol.backend.common.service;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

@Service
public class SanitizationService {

    /**
     * Policy nghiêm ngặt: Chỉ giữ lại văn bản thuần, loại bỏ mọi thẻ HTML.
     * Dùng cho: Tên người dùng, Tiêu đề bài viết.
     */
    private final PolicyFactory STRICT_POLICY = new HtmlPolicyBuilder()
            .toFactory();

    /**
     * Policy cho phép Rich Text cơ bản: in đậm, in nghiêng, liên kết.
     * Tự động thêm rel="nofollow" vào link để chống spam SEO.
     * Dùng cho: Bio, Nội dung bài viết, Comment.
     */
    private final PolicyFactory RICH_TEXT_POLICY = new HtmlPolicyBuilder()
            .allowElements("b", "strong", "i", "em")
            .allowUrlProtocols("https")
            .allowElements("a")
            .allowAttributes("href").onElements("a")
            .requireRelNofollowOnLinks()
            .toFactory();

    public String sanitizeStrict(String input) {
        if (input == null) return null;
        return STRICT_POLICY.sanitize(input);
    }

    public String sanitizeRichText(String input) {
        if (input == null) return null;
        return RICH_TEXT_POLICY.sanitize(input);
    }
}