package com.mindrevol.backend.modules.system.service;

import com.mindrevol.backend.modules.system.dto.CreateFeedbackRequest;
import com.mindrevol.backend.modules.system.entity.AppConfig;
import com.mindrevol.backend.modules.system.entity.FeedbackType;
import com.mindrevol.backend.modules.system.entity.UserFeedback;
import com.mindrevol.backend.modules.system.repository.AppConfigRepository;
import com.mindrevol.backend.modules.system.repository.UserFeedbackRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemService {

    private final AppConfigRepository appConfigRepository;
    private final UserFeedbackRepository userFeedbackRepository;
    private final UserRepository userRepository;

    // Lấy toàn bộ cấu hình public để App hiển thị (Link MXH, Policy...)
    public Map<String, String> getPublicConfigs() {
        return appConfigRepository.findAll().stream()
                .collect(Collectors.toMap(AppConfig::getKey, AppConfig::getValue));
    }

    @Transactional
    public void submitFeedback(CreateFeedbackRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId).orElse(null);

        UserFeedback feedback = UserFeedback.builder()
                .user(user)
                .type(FeedbackType.valueOf(request.getType()))
                .content(request.getContent())
                .appVersion(request.getAppVersion())
                .deviceName(request.getDeviceName())
                .screenshotUrl(request.getScreenshotUrl())
                .isResolved(false)
                .build();
        
        userFeedbackRepository.save(feedback);
        // TODO: Có thể bắn thông báo về Slack/Email cho Admin tại đây
    }
}