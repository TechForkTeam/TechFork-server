package com.techfork.domain.auth.controller;

import com.techfork.domain.auth.exception.AuthErrorCode;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.Role;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.global.security.auth.service.RefreshTokenService;
import com.techfork.global.security.jwt.JwtDTO;
import com.techfork.global.security.jwt.JwtProperties;
import com.techfork.global.security.jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 통합 테스트
 * - IntegrationTestBase 상속으로 테스트 컨테이너 공유
 * - @Tag("integration") 자동 적용
 * - 모든 레이어(Controller, Service, Repository) 통합 테스트
 * - MockMvc로 HTTP 요청/응답 테스트
 */
class AuthControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private User testUser;
    private String validRefreshToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);
        testUser = userRepository.save(testUser);
        userId = testUser.getId();

        // 리프레시 토큰 생성 및 Redis에 저장
        JwtDTO tokens = jwtUtil.generateTokens(userId, Role.USER);
        validRefreshToken = tokens.refreshToken();
        long expiration = jwtProperties.getRefreshTokenExpiration();
        refreshTokenService.saveRefreshToken(userId, validRefreshToken, expiration);
    }

    @AfterEach
    void tearDown() {
        // Redis 데이터 정리
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        // DB 데이터 정리
        userRepository.deleteAll();
    }

    // ===== 토큰 갱신 성공 테스트 =====

    @Test
    @DisplayName("토큰 갱신 성공 - 유효한 리프레시 토큰으로 새 액세스 토큰 발급")
    void refreshToken_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", validRefreshToken)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    // ===== 토큰 불일치 시 세션 무효화 테스트 =====

    @Test
    @DisplayName("토큰 갱신 실패 - Redis 토큰과 불일치하여 세션 무효화")
    void refreshToken_Fail_TokenMismatchAndSessionInvalidated() throws Exception {
        // Given: Redis에 다른 토큰 저장 (토큰 불일치 상황 시뮬레이션)
        String differentToken = "different.refresh.token";
        JwtDTO differentTokens = jwtUtil.generateTokens(userId, Role.USER);
        String requestToken = differentTokens.refreshToken();

        refreshTokenService.saveRefreshToken(userId, differentToken, jwtProperties.getRefreshTokenExpiration());

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", requestToken)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(AuthErrorCode.REFRESH_TOKEN_MISMATCH.getReason().code()))
                .andExpect(jsonPath("$.message").value(AuthErrorCode.REFRESH_TOKEN_MISMATCH.getReason().message()));

        // 세션이 무효화되었는지 검증 (Redis에서 토큰 삭제되었는지 확인)
        boolean isTokenValid = refreshTokenService.validateRefreshToken(userId, differentToken);
        assertThat(isTokenValid).isFalse();
    }

    @Test
    @DisplayName("토큰 갱신 실패 - Redis에 토큰이 없는 경우도 세션 무효화 처리")
    void refreshToken_Fail_NoTokenInRedis() throws Exception {
        // Given: Redis에서 토큰 삭제 (토큰 없는 상황 시뮬레이션)
        refreshTokenService.deleteRefreshToken(userId);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", validRefreshToken)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(AuthErrorCode.REFRESH_TOKEN_MISMATCH.getReason().code()));
    }

    // ===== 기타 실패 케이스 =====

    @Test
    @DisplayName("토큰 갱신 실패 - 리프레시 토큰이 없음")
    void refreshToken_Fail_TokenMissing() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(AuthErrorCode.REFRESH_TOKEN_MISSING.getReason().code()));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 토큰 형식")
    void refreshToken_Fail_InvalidTokenFormat() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "invalid.token.format")))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_REFRESH_TOKEN.getReason().code()));
    }

    // ===== 로그아웃 테스트 =====

    @Test
    @DisplayName("로그아웃 성공 - Redis 토큰 삭제 및 쿠키 제거")
    void logout_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refreshToken", validRefreshToken)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // Redis에서 토큰이 삭제되었는지 검증
        boolean isTokenValid = refreshTokenService.validateRefreshToken(userId, validRefreshToken);
        assertThat(isTokenValid).isFalse();
    }

    @Test
    @DisplayName("로그아웃 실패 - 리프레시 토큰이 없음")
    void logout_Fail_TokenMissing() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(AuthErrorCode.REFRESH_TOKEN_MISSING.getReason().code()));
    }
}