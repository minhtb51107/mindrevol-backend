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
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.mapper.FriendshipMapper;
import com.mindrevol.backend.modules.user.mapper.UserMapper;
import com.mindrevol.backend.modules.user.repository.FriendshipRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    // ... (Các hàm getMyProfile, getPublicProfile, updateProfile, updateFcmToken, getUserById, deleteMyAccount giữ nguyên) ...
    // Bạn có thể copy lại từ file cũ, tôi chỉ viết lại hàm exportMyData bị lỗi dưới đây

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
        if (request.getFullname() != null) request.setFullname(sanitizationService.sanitizeStrict(request.getFullname()));
        if (request.getBio() != null) request.setBio(sanitizationService.sanitizeRichText(request.getBio()));
        if (request.getHandle() != null && !request.getHandle().equals(user.getHandle())) {
            if (userRepository.existsByHandle(request.getHandle())) throw new BadRequestException("Handle @" + request.getHandle() + " đã được sử dụng.");
        }
        if (request.getTimezone() != null && !request.getTimezone().isEmpty()) {
            try { java.time.ZoneId.of(request.getTimezone()); user.setTimezone(request.getTimezone()); } catch (Exception e) {}
        }
        userMapper.updateUserFromRequest(request, user);
        return buildUserProfile(userRepository.save(user), user);
    }

    @Override
    @Transactional
    public void updateFcmToken(Long userId, String token) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFcmToken(token);
        userRepository.save(user);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public void deleteMyAccount(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        long timestamp = System.currentTimeMillis();
        user.setEmail(user.getEmail() + "_deleted_" + timestamp);
        user.setHandle(user.getHandle() + "_deleted_" + timestamp);
        userRepository.save(user);
        userRepository.deleteById(user.getId());
    }

    // --- SỬA LỖI TẠI ĐÂY ---
    @Override
    public UserDataExport exportMyData(Long userId) {
        User user = getUserById(userId);

        var habits = habitRepository.findByUserIdAndArchivedFalse(userId).stream()
                .map(habitMapper::toResponse).collect(Collectors.toList());

        var checkins = checkinRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(checkinMapper::toResponse).collect(Collectors.toList());

        // FIX: Đổi mapFriend() thành toResponse() để trả về FriendshipResponse
        var friends = friendshipRepository.findAllAcceptedFriendsList(userId).stream()
                .map(f -> friendshipMapper.toResponse(f, userId)) 
                .collect(Collectors.toList());

        var badges = userBadgeRepository.findByUserIdOrderByEarnedAtDesc(userId).stream()
                .map(ub -> ub.getBadge().getName()).collect(Collectors.toList());

        return UserDataExport.builder()
                .profile(buildUserProfile(user, user))
                .habits(habits)
                .checkins(checkins)
                .friends(friends) // Giờ kiểu dữ liệu đã khớp
                .badges(badges)
                .build();
    }

    @Override
    public List<UserSummaryResponse> searchUsers(String query, Long currentUserId) {
        String cleanedQuery = query.startsWith("@") ? query.substring(1) : query;
        List<User> users = userRepository.searchUsers(cleanedQuery);
        return users.stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .map(u -> UserSummaryResponse.builder()
                        .id(u.getId())
                        .fullname(u.getFullname())
                        .handle(u.getHandle())
                        .avatarUrl(u.getAvatarUrl())
                        .friendshipStatus("NONE")
                        .build())
                .collect(Collectors.toList());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserProfileResponse buildUserProfile(User targetUser, User viewer) {
        UserProfileResponse response = userMapper.toProfileResponse(targetUser);
        response.setFollowerCount(0);
        response.setFollowingCount(0);
        response.setFollowedByCurrentUser(false);
        return response;
    }
}