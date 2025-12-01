package com.mindrevol.backend.modules.user.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.service.SanitizationService;
import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.mapper.UserMapper;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SanitizationService sanitizationService;

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

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + email));
    }
    
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
    }

    private UserProfileResponse buildUserProfile(User targetUser, User viewer) {
        UserProfileResponse response = userMapper.toProfileResponse(targetUser);
        
        response.setFollowerCount(0); 
        response.setFollowingCount(0);
        response.setFollowedByCurrentUser(false);

        return response;
    }
}