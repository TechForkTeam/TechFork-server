package com.techfork.domain.auth.service;

import com.techfork.auth.application.AuthConverter;
import com.techfork.auth.application.AuthService;
import com.techfork.auth.application.dto.DeveloperTokenResponse;
import com.techfork.auth.application.dto.TokenRefreshResponse;
import com.techfork.auth.domain.exception.AuthErrorCode;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.exception.GeneralException;
import com.techfork.auth.security.service.RefreshTokenService;
import com.techfork.auth.security.service.UserAuthCacheService;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.techfork.auth.security.jwt.JwtConstants.TOKEN_TYPE_REFRESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthConverter authConverter;

    @Mock
    private UserAuthCacheService userAuthCacheService;

    @InjectMocks
    private AuthService authService;

    private String validRefreshToken;
    private String newAccessToken;
    private String newRefreshToken;
    private User user;
    private Long userId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "domain", "localhost");

        validRefreshToken = "valid.refresh.token";
        newAccessToken = "new.access.token";
        newRefreshToken = "new.refresh.token";
        userId = 1L;

        user = User.createSocialUser(SocialType.KAKAO, "socialId123", "test@example.com", null);
        ReflectionTestUtils.setField(user, "id", userId);
    }

    // ===== 토큰 갱신 테스트 =====

    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshToken_Success() {
        // Given
        JwtDTO newTokens = JwtDTO.of(newAccessToken, newRefreshToken);

        given(jwtUtil.isValidToken(validRefreshToken)).willReturn(true);
        given(jwtUtil.getUserIdFromToken(validRefreshToken)).willReturn(userId);
        given(refreshTokenService.validateRefreshToken(userId, validRefreshToken)).willReturn(true);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(jwtUtil.generateTokens(userId, Role.USER)).willReturn(newTokens);
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(900000L);
        given(jwtProperties.getAccessTokenExpiration()).willReturn(180000L);

        // When
        TokenRefreshResponse result = authService.refreshToken(validRefreshToken, response);

        // Then
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        verify(jwtUtil).isValidToken(validRefreshToken);
        verify(jwtUtil).validateTokenType(validRefreshToken, TOKEN_TYPE_REFRESH);
        verify(refreshTokenService).saveRefreshToken(eq(userId), eq(newRefreshToken), anyLong());
        verify(userAuthCacheService).put(eq(userId), eq(user), eq(180000L));
        verify(response).addHeader(eq("Set-Cookie"), anyString());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 리프레시 토큰이 null")
    void refreshToken_Fail_TokenIsNull() {
        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(null, response))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISSING);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 리프레시 토큰이 빈 문자열")
    void refreshToken_Fail_TokenIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> authService.refreshToken("", response))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISSING);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 토큰")
    void refreshToken_Fail_InvalidToken() {
        // Given
        given(jwtUtil.isValidToken(validRefreshToken)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(validRefreshToken, response))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - Redis에 저장된 토큰과 불일치하여 세션 무효화")
    void refreshToken_Fail_TokenMismatchAndSessionInvalidated() {
        // Given
        given(jwtUtil.isValidToken(validRefreshToken)).willReturn(true);
        given(jwtUtil.getUserIdFromToken(validRefreshToken)).willReturn(userId);
        given(refreshTokenService.validateRefreshToken(userId, validRefreshToken)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(validRefreshToken, response))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISMATCH);

        // 세션 무효화를 위해 Redis 토큰 삭제가 호출되었는지 검증
        verify(refreshTokenService).deleteRefreshToken(userId);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 사용자를 찾을 수 없음")
    void refreshToken_Fail_UserNotFound() {
        // Given
        given(jwtUtil.isValidToken(validRefreshToken)).willReturn(true);
        given(jwtUtil.getUserIdFromToken(validRefreshToken)).willReturn(userId);
        given(refreshTokenService.validateRefreshToken(userId, validRefreshToken)).willReturn(true);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(validRefreshToken, response))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND);
    }

    // ===== 로그아웃 테스트 =====

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // Given
        given(jwtUtil.isValidToken(validRefreshToken)).willReturn(true);
        given(jwtUtil.getUserIdFromToken(validRefreshToken)).willReturn(userId);

        // When
        authService.logout(validRefreshToken, response);

        // Then
        verify(jwtUtil).isValidToken(validRefreshToken);
        verify(jwtUtil).validateTokenType(validRefreshToken, TOKEN_TYPE_REFRESH);
        verify(refreshTokenService).deleteRefreshToken(userId);
        verify(response).addHeader(eq("Set-Cookie"), anyString());
    }

    @Test
    @DisplayName("로그아웃 실패 - 리프레시 토큰이 null")
    void logout_Fail_TokenIsNull() {
        // When & Then
        assertThatThrownBy(() -> authService.logout(null, response))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISSING);
    }

    @Test
    @DisplayName("로그아웃 실패 - 리프레시 토큰이 빈 문자열")
    void logout_Fail_TokenIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> authService.logout("", response))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISSING);
    }

    @Test
    @DisplayName("로그아웃 실패 - 유효하지 않은 토큰")
    void logout_Fail_InvalidToken() {
        // Given
        given(jwtUtil.isValidToken(validRefreshToken)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.logout(validRefreshToken, response))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    // ===== 개발자 토큰 발급 테스트 =====

    @Test
    @DisplayName("개발자 토큰 발급 성공 - ADMIN 권한으로 30일 만료 토큰 발급")
    void generateDeveloperToken_Success() {
        // Given
        User adminUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("adminSocialId")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();
        ReflectionTestUtils.setField(adminUser, "id", userId);

        String developerToken = "long.lived.access.token";
        DeveloperTokenResponse expectedResponse = DeveloperTokenResponse.builder()
                .developerToken(developerToken)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));
        given(jwtUtil.generateLongLivedAccessToken(userId, Role.ADMIN)).willReturn(developerToken);
        given(authConverter.toDeveloperTokenResponse(developerToken)).willReturn(expectedResponse);

        // When
        DeveloperTokenResponse result = authService.generateDeveloperToken(userId);

        // Then
        assertThat(result.developerToken()).isEqualTo(developerToken);
        verify(userRepository).findById(userId);
        verify(jwtUtil).generateLongLivedAccessToken(userId, Role.ADMIN);
        verify(authConverter).toDeveloperTokenResponse(developerToken);
    }

    @Test
    @DisplayName("개발자 토큰 발급 실패 - 사용자를 찾을 수 없음")
    void generateDeveloperToken_Fail_UserNotFound() {
        // Given
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.generateDeveloperToken(userId))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND);

        verify(userRepository).findById(userId);
        verify(jwtUtil, never()).generateLongLivedAccessToken(anyLong(), any(Role.class));
    }

    @Test
    @DisplayName("개발자 토큰 발급 실패 - ADMIN 권한이 아닌 사용자")
    void generateDeveloperToken_Fail_InsufficientPermissions() {
        // Given - 일반 사용자
        User normalUser = User.createSocialUser(SocialType.KAKAO, "userSocialId", "user@example.com", null);
        ReflectionTestUtils.setField(normalUser, "id", userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(normalUser));

        // When & Then
        assertThatThrownBy(() -> authService.generateDeveloperToken(userId))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.FORBIDDEN_INSUFFICIENT_PERMISSIONS);

        verify(userRepository).findById(userId);
        verify(jwtUtil, never()).generateLongLivedAccessToken(anyLong(), any(Role.class));
    }

}
