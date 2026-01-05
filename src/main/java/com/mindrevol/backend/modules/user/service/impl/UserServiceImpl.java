package com.mindrevol.backend.modules.user.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.service.SanitizationService;
import com.mindrevol.backend.modules.auth.entity.SocialAccount;
import com.mindrevol.backend.modules.auth.repository.SocialAccountRepository;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.mapper.JourneyMapper;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.storage.service.FileStorageService;
import com.mindrevol.backend.modules.user.dto.request.UpdateNotificationSettingsRequest;
import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.LinkedAccountResponse;
import com.mindrevol.backend.modules.user.dto.response.UserDataExport;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.Friendship;
import com.mindrevol.backend.modules.user.entity.FriendshipStatus;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserSettings;
import com.mindrevol.backend.modules.user.mapper.FriendshipMapper;
import com.mindrevol.backend.modules.user.mapper.UserMapper;
import com.mindrevol.backend.modules.user.repository.FriendshipRepository;
import com.mindrevol.backend.modules.user.repository.UserBlockRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.repository.UserSettingsRepository;
import com.mindrevol.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
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
    private final FriendshipRepository friendshipRepository;
    private final FriendshipMapper friendshipMapper;
    private final JourneyRepository journeyRepository;
    private final JourneyMapper journeyMapper;
    private final UserBlockRepository userBlockRepository;
    private final FileStorageService fileStorageService;
    
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
        return getPublicProfileCommon(targetUser, currentUserEmail);
    }

    @Override
    public UserProfileResponse getPublicProfileById(String userId, String currentUserEmail) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại."));
        return getPublicProfileCommon(targetUser, currentUserEmail);
    }

    private UserProfileResponse getPublicProfileCommon(User targetUser, String currentUserEmail) {
        User currentUser = null;
        if (currentUserEmail != null && !currentUserEmail.equals("anonymousUser")) {
            currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        }
        return buildUserProfile(targetUser, currentUser);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String currentEmail, UpdateProfileRequest request, MultipartFile file) {
        User user = getUserByEmail(currentEmail);
        
        // 1. Nếu có file ảnh gửi lên thì upload
        if (file != null && !file.isEmpty()) {
            try {
                // [FIX] Sử dụng DTO FileUploadResult trả về từ service
                FileStorageService.FileUploadResult uploadResult = fileStorageService.uploadFile(file, "avatars/" + user.getId());
                
                // Lấy URL từ kết quả upload
                user.setAvatarUrl(uploadResult.getUrl());
                
                // (Optional) Nếu sau này User có trường avatarFileId thì set ở đây:
                // user.setAvatarFileId(uploadResult.getFileId());
            } catch (Exception e) {
                log.error("Failed to upload avatar", e);
                throw new BadRequestException("Lỗi khi tải lên ảnh đại diện: " + e.getMessage());
            }
        }

        // 2. Xử lý các trường text
        if (request.getFullname() != null) request.setFullname(sanitizationService.sanitizeStrict(request.getFullname()));
        if (request.getBio() != null) request.setBio(sanitizationService.sanitizeRichText(request.getBio()));
        
        if (request.getHandle() != null && !request.getHandle().equals(user.getHandle())) {
            if (userRepository.existsByHandle(request.getHandle())) 
                throw new BadRequestException("Handle @" + request.getHandle() + " đã được sử dụng.");
        }
        
        if (request.getTimezone() != null && !request.getTimezone().isEmpty()) {
            try { java.time.ZoneId.of(request.getTimezone()); user.setTimezone(request.getTimezone()); } catch (Exception e) {}
        }
        
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
    public void updateFcmToken(String userId, String token) { 
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFcmToken(token);
        userRepository.save(user);
    }

    @Override
    public User getUserById(String id) { 
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public void deleteMyAccount(String userId) { 
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        long timestamp = System.currentTimeMillis();
        // Soft delete bằng cách đổi info tránh trùng lặp sau này
        user.setEmail(user.getEmail() + "_deleted_" + timestamp);
        user.setHandle(user.getHandle() + "_deleted_" + timestamp);
        userRepository.save(user);
        
        List<SocialAccount> socialAccounts = socialAccountRepository.findAllByUserId(userId);
        socialAccountRepository.deleteAll(socialAccounts);
        
        userRepository.deleteById(user.getId());
    }

    @Override
    public UserDataExport exportMyData(String userId) { 
        User user = getUserById(userId);

        // Chỉ export Checkin và Friend, bỏ Habit/Badge
        var checkins = checkinRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(checkinMapper::toResponse).collect(Collectors.toList());

        var friends = friendshipRepository.findAllAcceptedFriendsList(userId).stream()
                .map(f -> friendshipMapper.toResponse(f, userId)) 
                .collect(Collectors.toList());

        return UserDataExport.builder()
                .profile(buildUserProfile(user, user))
                .checkins(checkins)
                .friends(friends)
                .build();
    }

    @Override
    public List<UserSummaryResponse> searchUsers(String query, String currentUserId) {
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
    public List<JourneyResponse> getUserRecaps(String userId) { 
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        List<Journey> completedJourneys = journeyRepository.findCompletedJourneysByUserId(userId);
        return completedJourneys.stream()
                .map(journeyMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserSettings getNotificationSettings(String userId) {
        return userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = getUserById(userId);
                    UserSettings settings = UserSettings.builder().user(user).build();
                    return userSettingsRepository.save(settings);
                });
    }

    @Override
    @Transactional
    public UserSettings updateNotificationSettings(String userId, UpdateNotificationSettingsRequest request) {
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
    public List<LinkedAccountResponse> getLinkedAccounts(String userId) {
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
    @Transactional
    public void unlinkSocialAccount(String userId, String provider) {
        User user = getUserById(userId);
        
        boolean hasPassword = "LOCAL".equals(user.getAuthProvider());
        long socialCount = socialAccountRepository.countByUserId(userId);

        if (!hasPassword && socialCount <= 1) {
            throw new BadRequestException("Bạn không thể hủy liên kết phương thức đăng nhập duy nhất. Vui lòng tạo mật khẩu trước.");
        }

        SocialAccount account = socialAccountRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa liên kết với " + provider));
        
        socialAccountRepository.delete(account);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserProfileResponse buildUserProfile(User targetUser, User viewer) {
        UserProfileResponse response = userMapper.toProfileResponse(targetUser);
        
        long friendCount = friendshipRepository.countByUserIdAndStatusAccepted(targetUser.getId());
        response.setFriendCount(friendCount); 

        if (viewer != null && viewer.getId().equals(targetUser.getId())) {
            response.setMe(true);
            response.setFriendshipStatus(FriendshipStatus.NONE); 
            response.setBlockedByMe(false);
            response.setBlockedByThem(false);
        } else if (viewer != null) {
            response.setMe(false);
            boolean isBlockedByMe = userBlockRepository.existsByBlockerIdAndBlockedId(viewer.getId(), targetUser.getId());
            boolean isBlockedByThem = userBlockRepository.existsByBlockerIdAndBlockedId(targetUser.getId(), viewer.getId());
            response.setBlockedByMe(isBlockedByMe);
            response.setBlockedByThem(isBlockedByThem);
            Optional<Friendship> friendship = friendshipRepository.findByUsers(viewer.getId(), targetUser.getId());
            response.setFriendshipStatus(friendship.map(Friendship::getStatus).orElse(FriendshipStatus.NONE));
        } else {
            response.setMe(false);
            response.setFriendshipStatus(FriendshipStatus.NONE);
            response.setBlockedByMe(false);
            response.setBlockedByThem(false);
        }

        response.setFollowerCount(0); 
        response.setFollowingCount(0);
        response.setFollowedByCurrentUser(false);
        
        return response;
    }
}