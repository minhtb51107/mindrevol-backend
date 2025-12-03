package com.mindrevol.backend.modules.user.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.service.SanitizationService;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.gamification.repository.UserBadgeRepository;
import com.mindrevol.backend.modules.habit.mapper.HabitMapper;
import com.mindrevol.backend.modules.habit.repository.HabitRepository;
import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.UserDataExport;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.mapper.FriendshipMapper;
import com.mindrevol.backend.modules.user.mapper.UserMapper;
import com.mindrevol.backend.modules.user.repository.FriendshipRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SanitizationService sanitizationService;
    private final CheckinRepository checkinRepository;
    private final CheckinMapper checkinMapper;
    private final HabitRepository habitRepository;
    private final HabitMapper habitMapper;
    private final FriendshipRepository friendshipRepository;
    private final FriendshipMapper friendshipMapper;
    private final UserBadgeRepository userBadgeRepository;

    @Override
    public UserProfileResponse getMyProfile(String currentEmail) {
        User user = getUserByEmail(currentEmail);
        return buildUserProfile(user, user);
    }

    @Override
    public UserProfileResponse getPublicProfile(String handle, String currentUserEmail) {
        User targetUser = userRepository.findByHandle(handle)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng @" + handle + " không tồn tại."));

        User currentUser = null;
        if (currentUserEmail != null && !currentUserEmail.equals("anonymousUser")) {
            currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        }

        return buildUserProfile(targetUser, currentUser);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String currentEmail, UpdateProfileRequest request) {
        User user = getUserByEmail(currentEmail);

        if (request.getFullname() != null) {
            request.setFullname(sanitizationService.sanitizeStrict(request.getFullname()));
        }
        
        if (request.getBio() != null) {
            request.setBio(sanitizationService.sanitizeRichText(request.getBio()));
        }

        if (request.getHandle() != null && !request.getHandle().equals(user.getHandle())) {
            if (userRepository.existsByHandle(request.getHandle())) {
                throw new BadRequestException("Handle @" + request.getHandle() + " đã được sử dụng.");
            }
        }
        
        if (request.getTimezone() != null && !request.getTimezone().isEmpty()) {
            try {
                // Validate timezone hợp lệ
                java.time.ZoneId.of(request.getTimezone());
                user.setTimezone(request.getTimezone());
            } catch (Exception e) {
                log.warn("Invalid timezone sent by user {}: {}", user.getId(), request.getTimezone());
                // Có thể bỏ qua hoặc set về UTC tùy ý
            }
        }

        userMapper.updateUserFromRequest(request, user);
        User updatedUser = userRepository.save(user);
        log.info("User ID {} updated profile.", user.getId());
        return buildUserProfile(updatedUser, user);
    }
    
    @Override
    @Transactional
    public void updateFcmToken(Long userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFcmToken(token);
        userRepository.save(user);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
    }

    // --- HÀM MỚI: XÓA TÀI KHOẢN ---
    @Override
    @Transactional
    public void deleteMyAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Đổi thông tin Unique để giải phóng (tránh lỗi khi đăng ký lại)
        long timestamp = System.currentTimeMillis();
        String suffix = "_deleted_" + timestamp;

        user.setEmail(user.getEmail() + suffix);
        user.setHandle(user.getHandle() + suffix);
        
        // 2. Lưu thông tin rác này xuống DB trước
        userRepository.save(user); 
        
        // 3. Xóa (Soft Delete sẽ được kích hoạt bởi @SQLDelete trong Entity)
        userRepository.deleteById(user.getId());
        
        log.info("User {} deleted account (Soft Delete). Email/Handle freed.", userId);
    }
    // ------------------------------
    
    @Override
    public UserDataExport exportMyData(Long userId) {
        User user = getUserById(userId);

        // 1. Profile
        UserProfileResponse profile = buildUserProfile(user, user);

        // 2. Habits
        var habits = habitRepository.findByUserIdAndArchivedFalse(userId).stream()
                .map(habitMapper::toResponse)
                .collect(Collectors.toList());

        // 3. Checkins (Lấy hết, không phân trang)
        var checkins = checkinRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream() // Cần thêm hàm này ở Repo
                .map(checkinMapper::toResponse)
                .collect(Collectors.toList());

        // 4. Friends
        var friends = friendshipRepository.findAllAcceptedFriendsList(userId).stream() // Đã có hàm này ở phần trước
                .map(f -> friendshipMapper.mapFriend(f, userId))
                .collect(Collectors.toList());

        // 5. Badges
        var badges = userBadgeRepository.findByUserIdOrderByEarnedAtDesc(userId).stream()
                .map(ub -> ub.getBadge().getName())
                .collect(Collectors.toList());

        log.info("User {} exported their data.", userId);

        return UserDataExport.builder()
                .profile(profile)
                .habits(habits)
                .checkins(checkins)
                .friends(friends)
                .badges(badges)
                .build();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + email));
    }

    private UserProfileResponse buildUserProfile(User targetUser, User viewer) {
        UserProfileResponse response = userMapper.toProfileResponse(targetUser);
        response.setFollowerCount(0); 
        response.setFollowingCount(0);
        response.setFollowedByCurrentUser(false);
        return response;
    }
}