//package com.mindrevol.backend.user.service;
//
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import java.util.Optional;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import com.mindrevol.backend.common.exception.BadRequestException;
//import com.mindrevol.backend.common.service.SanitizationService;
//import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
//import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
//import com.mindrevol.backend.modules.user.entity.User;
//import com.mindrevol.backend.modules.user.mapper.UserMapper;
//import com.mindrevol.backend.modules.user.repository.UserRepository;
//import com.mindrevol.backend.modules.user.service.impl.UserServiceImpl;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceImplTest {
//
//    @Mock private UserRepository userRepository;
//    @Mock private UserMapper userMapper;
//    @Mock private SanitizationService sanitizationService;
//
//    @InjectMocks
//    private UserServiceImpl userService;
//
//    private User user;
//    private final String EMAIL = "test@mindrevol.com";
//
//    @BeforeEach
//    void setUp() {
//        user = User.builder()
//                .email(EMAIL)
//                .handle("old_handle")
//                .fullname("Old Name")
//                .build();
//    }
//
//    @Test
//    void updateProfile_ShouldSanitizeAndSave() {
//        // Arrange
//        String dirtyName = "New <script>Name</script>"; // 1. Lưu giá trị gốc
//        String cleanName = "New Name";
//        String dirtyBio = "New <b>Bio</b>";
//
//        UpdateProfileRequest request = new UpdateProfileRequest();
//        request.setFullname(dirtyName);
//        request.setBio(dirtyBio);
//        request.setHandle("new_handle");
//
//        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
//        when(userRepository.existsByHandle("new_handle")).thenReturn(false);
//        
//        // Mock hành vi trả về giá trị sạch
//        when(sanitizationService.sanitizeStrict(dirtyName)).thenReturn(cleanName);
//        when(sanitizationService.sanitizeRichText(dirtyBio)).thenReturn(dirtyBio); // Giữ nguyên thẻ b
//        
//        when(userRepository.save(any(User.class))).thenReturn(user);
//        when(userMapper.toProfileResponse(any(User.class))).thenReturn(UserProfileResponse.builder().build());
//
//        // Act
//        userService.updateProfile(EMAIL, request);
//
//        // Assert
//        // 2. Verify với giá trị gốc (dirtyName), KHÔNG dùng request.getFullname() vì nó đã bị đổi
//        verify(sanitizationService).sanitizeStrict(dirtyName); 
//        verify(sanitizationService).sanitizeRichText(dirtyBio);
//        
//        verify(userMapper).updateUserFromRequest(eq(request), eq(user));
//        verify(userRepository).save(user);
//    }
//
//    @Test
//    void updateProfile_DuplicateHandle_ShouldThrowException() {
//        UpdateProfileRequest request = new UpdateProfileRequest();
//        request.setHandle("existing_handle");
//
//        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
//        // Giả lập handle này đã có người dùng
//        when(userRepository.existsByHandle("existing_handle")).thenReturn(true);
//
//        assertThrows(BadRequestException.class, () -> userService.updateProfile(EMAIL, request));
//    }
//}