//package com.mindrevol.backend.shared.util;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//import java.security.Key;
//import java.util.Date;
//import java.util.Set;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import com.mindrevol.backend.common.utils.JwtUtil;
//import com.mindrevol.backend.modules.user.entity.Role;
//import com.mindrevol.backend.modules.user.entity.User;
//
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.jsonwebtoken.security.Keys;
//
//class JwtUtilTest {
//
//    private JwtUtil jwtUtil;
//    // Key giả lập phải đủ dài (>= 64 ký tự) cho HS512
//    private final String SECRET_KEY = "day-la-mot-chuoi-bi-mat-rat-dai-va-an-toan-cho-test-env-only-phai-du-64-ky-tu-de-chay-hs512";
//    private final long EXPIRATION = 3600000; // 1 giờ
//
//    @BeforeEach
//    void setUp() {
//        jwtUtil = new JwtUtil();
//        // Inject giá trị cấu hình thủ công cho Unit Test
//        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET_KEY);
//        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpirationMs", EXPIRATION);
//        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpirationMs", EXPIRATION);
//    }
//
//    @Test
//    void generateAccessToken_ShouldContainCorrectClaims() {
//        // Arrange
//        User user = User.builder()
//                .email("test@example.com")
//                .roles(Set.of(Role.builder().name("USER").build()))
//                .build();
//        
//        // FIX: Set ID bằng setter thay vì builder
//        // Vì @Builder của Lombok ở class con không nhận field của class cha
//        user.setId(1L); 
//
//        // Act
//        String token = jwtUtil.generateAccessToken(user);
//
//        // Assert
//        assertNotNull(token);
//        String email = jwtUtil.getEmailFromToken(token);
//        assertEquals("test@example.com", email);
//        assertTrue(jwtUtil.validateToken(token));
//    }
//
//    @Test
//    void validateToken_ExpiredToken_ShouldThrowException() {
//        // Arrange: Tạo token hết hạn
//        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
//        String expiredToken = Jwts.builder()
//                .setSubject("test@example.com")
//                .setIssuedAt(new Date(System.currentTimeMillis() - 100000))
//                .setExpiration(new Date(System.currentTimeMillis() - 1000)) 
//                .signWith(key, SignatureAlgorithm.HS512)
//                .compact();
//
//        // Act & Assert
//        assertThrows(Exception.class, () -> jwtUtil.validateToken(expiredToken));
//    }
//
//    @Test
//    void validateToken_MalformedToken_ShouldThrowException() {
//        String invalidToken = "token.nay.bi.loi";
//        assertThrows(Exception.class, () -> jwtUtil.validateToken(invalidToken));
//    }
//}