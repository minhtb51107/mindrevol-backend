package com.mindrevol.backend.modules.user.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.service.SanitizationService;
import com.mindrevol.backend.modules.auth.entity.SocialAccount;
import com.mindrevol.backend.modules.auth.repository.SocialAccountRepository;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.gamification.repository.UserBadgeRepository;
import com.mindrevol.backend.modules.habit.mapper.HabitMapper;
import com.mindrevol.backend.modules.habit.repository.HabitRepository;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.mapper.JourneyMapper;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.user.dto.request.UpdateNotificationSettingsRequest;
import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.LinkedAccountResponse;
import com.mindrevol.backend.modules.user.dto.response.UserDataExport;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserSettings;
import com.mindrevol.backend.modules.user.mapper.FriendshipMapper;
import com.mindrevol.backend.modules.user.mapper.UserMapper;
import com.mindrevol.backend.modules.user.repository.FriendshipRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.repository.UserSettingsRepository;
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
    private final JourneyRepository journeyRepository;
    private final JourneyMapper journeyMapper;
    
    // --- Inject thêm các Repository mới ---
    private final UserSettingsRepository userSettingsRepository;
    private final SocialAccountRepository socialAccountRepository;

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
        
        // Sanitize input
        if (request.getFullname() != null) request.setFullname(sanitizationService.sanitizeStrict(request.getFullname()));
        if (request.getBio() != null) request.setBio(sanitizationService.sanitizeRichText(request.getBio()));
        
        // Validate Handle
        if (request.getHandle() != null && !request.getHandle().equals(user.getHandle())) {
            if (userRepository.existsByHandle(request.getHandle())) 
                throw new BadRequestException("Handle @" + request.getHandle() + " đã được sử dụng.");
        }
        
        // Validate Timezone
        if (request.getTimezone() != null && !request.getTimezone().isEmpty()) {
            try { java.time.ZoneId.of(request.getTimezone()); user.setTimezone(request.getTimezone()); } catch (Exception e) {}
        }
        
        // [MỚI] Map thủ công DateOfBirth và Gender để đảm bảo an toàn
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
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
        // Đổi thông tin để ẩn danh trước khi xóa mềm (nếu dùng soft delete) hoặc hard delete
        user.setEmail(user.getEmail() + "_deleted_" + timestamp);
        user.setHandle(user.getHandle() + "_deleted_" + timestamp);
        userRepository.save(user);
        
        // Xóa liên kết mạng xã hội
        List<SocialAccount> socialAccounts = socialAccountRepository.findAllByUserId(userId); // Cần đảm bảo repo có method này
        socialAccountRepository.deleteAll(socialAccounts);
        
        userRepository.deleteById(user.getId());
    }

    @Override
    public UserDataExport exportMyData(Long userId) {
        User user = getUserById(userId);

        var habits = habitRepository.findByUserIdAndArchivedFalse(userId).stream()
                .map(habitMapper::toResponse).collect(Collectors.toList());

        var checkins = checkinRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(checkinMapper::toResponse).collect(Collectors.toList());

        var friends = friendshipRepository.findAllAcceptedFriendsList(userId).stream()
                .map(f -> friendshipMapper.toResponse(f, userId)) 
                .collect(Collectors.toList());

        var badges = userBadgeRepository.findByUserIdOrderByEarnedAtDesc(userId).stream()
                .map(ub -> ub.getBadge().getName()).collect(Collectors.toList());

        return UserDataExport.builder()
                .profile(buildUserProfile(user, user))
                .habits(habits)
                .checkins(checkins)
                .friends(friends)
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

    @Override
    @Transactional(readOnly = true)
    public List<JourneyResponse> getUserRecaps(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        List<Journey> completedJourneys = journeyRepository.findCompletedJourneysByUserId(userId);
        return completedJourneys.stream()
                .map(journeyMapper::toResponse)
                .collect(Collectors.toList());
    }

    // --- CÁC METHOD MỚI CHO SETTINGS & SOCIAL ---

    @Override
    @Transactional // <--- ĐÃ CÓ: Sửa lỗi 500 Insert Read-only
    public UserSettings getNotificationSettings(Long userId) {
        return userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Nếu chưa có thì tạo mặc định
                    User user = getUserById(userId);
                    UserSettings settings = UserSettings.builder().user(user).build();
                    return userSettingsRepository.save(settings);
                });
    }

    @Override
    @Transactional // <--- ĐÃ CÓ
    public UserSettings updateNotificationSettings(Long userId, UpdateNotificationSettingsRequest request) {
        UserSettings settings = getNotificationSettings(userId);

        if (request.getEmailDailyReminder() != null) settings.setEmailDailyReminder(request.getEmailDailyReminder());
        if (request.getEmailUpdates() != null) settings.setEmailUpdates(request.getEmailUpdates());
        if (request.getPushFriendRequest() != null) settings.setPushFriendRequest(request.getPushFriendRequest());
        if (request.getPushNewComment() != null) settings.setPushNewComment(request.getPushNewComment());
        if (request.getPushJourneyInvite() != null) settings.setPushJourneyInvite(request.getPushJourneyInvite());
        if (request.getPushReaction() != null) settings.setPushReaction(request.getPushReaction());

        return userSettingsRepository.save(settings);
    }

    @Override
    @Transactional
    public void createDefaultSettings(User user) {
        if (userSettingsRepository.findByUserId(user.getId()).isEmpty()) {
            userSettingsRepository.save(UserSettings.builder().user(user).build());
        }
    }

    @Override
    public List<LinkedAccountResponse> getLinkedAccounts(Long userId) {
        // Cần đảm bảo SocialAccountRepository có method findAllByUserId
        // Nếu chưa có, hãy thêm vào Interface Repository: List<SocialAccount> findAllByUserId(Long userId);
        List<SocialAccount> accounts = socialAccountRepository.findAllByUserId(userId);
        return accounts.stream()
                .map(acc -> LinkedAccountResponse.builder()
                        .provider(acc.getProvider())
                        .email(acc.getEmail())
                        .avatarUrl(acc.getAvatarUrl())
                        .connected(true)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional // <--- ĐÃ CÓ
    public void unlinkSocialAccount(Long userId, String provider) {
        User user = getUserById(userId);
        
        // Kiểm tra an toàn: Không cho phép unlink nếu đây là phương thức đăng nhập duy nhất
        // VÀ user chưa thiết lập mật khẩu (authProvider != LOCAL).
        boolean hasPassword = "LOCAL".equals(user.getAuthProvider());
        long socialCount = socialAccountRepository.countByUserId(userId);

        if (!hasPassword && socialCount <= 1) {
            throw new BadRequestException("Bạn không thể hủy liên kết phương thức đăng nhập duy nhất. Vui lòng tạo mật khẩu trước.");
        }

        SocialAccount account = socialAccountRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa liên kết với " + provider));
        
        socialAccountRepository.delete(account);
    }

    // ---------------------------------------------

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserProfileResponse buildUserProfile(User targetUser, User viewer) {
        UserProfileResponse response = userMapper.toProfileResponse(targetUser);
        
        long friendCount = friendshipRepository.countByUserIdAndStatusAccepted(targetUser.getId());
        response.setFriendCount(friendCount); 

        response.setFollowerCount(0);
        response.setFollowingCount(0);
        response.setFollowedByCurrentUser(false);
        
        return response;
    }
}