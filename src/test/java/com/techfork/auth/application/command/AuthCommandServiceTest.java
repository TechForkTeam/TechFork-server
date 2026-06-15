package com.techfork.auth.application.command;

import com.techfork.auth.application.command.input.GenerateDeveloperTokenCommand;
import com.techfork.auth.application.command.input.LogoutCommand;
import com.techfork.auth.application.command.input.RefreshTokenCommand;
import com.techfork.auth.application.command.result.DeveloperTokenResult;
import com.techfork.auth.application.command.result.TokenRefreshResult;
import com.techfork.auth.domain.exception.AuthErrorCode;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.token.RefreshTokenStore;
import com.techfork.auth.security.service.UserAuthCacheService;
import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.application.auth.UserAuthAccountService;
import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.UserStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.techfork.auth.security.jwt.JwtConstants.TOKEN_TYPE_REFRESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private UserAuthAccountService userAuthAccountService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserAuthCacheService userAuthCacheService;

    @InjectMocks
    private AuthCommandService authCommandService;

    private String validRefreshToken;
    private String newAccessToken;
    private String newRefreshToken;
    private UserAuthProfile userAuthProfile;
    private Long userId;

    @BeforeEach
    void setUp() {
        validRefreshToken = "valid.refresh.token";
        newAccessToken = "new.access.token";
        newRefreshToken = "new.refresh.token";
        userId = 1L;
        userAuthProfile = new UserAuthProfile(userId, Role.USER, UserStatus.PENDING, "test@example.com", false);
    }

    // ===== 토큰 갱신 테스트 =====

    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshToken_Success() {
        // Given
        JwtDTO newTokens = JwtDTO.of(newAccessToken, newRefreshToken);

        given(jwtUtil.isValidToken(validRefreshToken)).willReturn(true);
        given(jwtUtil.getUserIdFromToken(validRefreshToken)).willReturn(userId);
        given(refreshTokenStore.validateRefreshToken(userId, validRefreshToken)).willReturn(true);
        given(userAuthAccountService.findAuthProfileById(userId)).willReturn(Optional.of(userAuthProfile));
        given(jwtUtil.generateTokens(userId, Role.USER)).willReturn(newTokens);
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(900000L);
        given(jwtProperties.getAccessTokenExpiration()).willReturn(180000L);

        // When
        TokenRefreshResult result = authCommandService.refreshToken(new RefreshTokenCommand(validRefreshToken));

        // Then
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.refreshToken()).isEqualTo(newRefreshToken);
        assertThat(result.refreshTokenExpiration()).isEqualTo(900000L);
        verify(jwtUtil).isValidToken(validRefreshToken);
        verify(jwtUtil).validateTokenType(validRefreshToken, TOKEN_TYPE_REFRESH);
        verify(refreshTokenStore).saveRefreshToken(eq(userId), eq(newRefreshToken), anyLong());
        verify(userAuthCacheService).put(eq(userId), eq(userAuthProfile), eq(180000L));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 리프레시 토큰이 null")
    void refreshToken_Fail_TokenIsNull() {
        // When & Then
        assertThatThrownBy(() -> authCommandService.refreshToken(new RefreshTokenCommand(null)))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISSING);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 리프레시 토큰이 빈 문자열")
    void refreshToken_Fail_TokenIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> authCommandService.refreshToken(new RefreshTokenCommand("")))
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
        assertThatThrownBy(() -> authCommandService.refreshToken(new RefreshTokenCommand(validRefreshToken)))
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
        given(refreshTokenStore.validateRefreshToken(userId, validRefreshToken)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authCommandService.refreshToken(new RefreshTokenCommand(validRefreshToken)))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISMATCH);

        // 세션 무효화를 위해 Redis 토큰 삭제가 호출되었는지 검증
        verify(refreshTokenStore).deleteRefreshToken(userId);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 사용자를 찾을 수 없음")
    void refreshToken_Fail_UserNotFound() {
        // Given
        given(jwtUtil.isValidToken(validRefreshToken)).willReturn(true);
        given(jwtUtil.getUserIdFromToken(validRefreshToken)).willReturn(userId);
        given(refreshTokenStore.validateRefreshToken(userId, validRefreshToken)).willReturn(true);
        given(userAuthAccountService.findAuthProfileById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authCommandService.refreshToken(new RefreshTokenCommand(validRefreshToken)))
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
        authCommandService.logout(new LogoutCommand(validRefreshToken));

        // Then
        verify(jwtUtil).isValidToken(validRefreshToken);
        verify(jwtUtil).validateTokenType(validRefreshToken, TOKEN_TYPE_REFRESH);
        verify(refreshTokenStore).deleteRefreshToken(userId);
    }

    @Test
    @DisplayName("로그아웃 실패 - 리프레시 토큰이 null")
    void logout_Fail_TokenIsNull() {
        // When & Then
        assertThatThrownBy(() -> authCommandService.logout(new LogoutCommand(null)))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISSING);
    }

    @Test
    @DisplayName("로그아웃 실패 - 리프레시 토큰이 빈 문자열")
    void logout_Fail_TokenIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> authCommandService.logout(new LogoutCommand("")))
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
        assertThatThrownBy(() -> authCommandService.logout(new LogoutCommand(validRefreshToken)))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    // ===== 개발자 토큰 발급 테스트 =====

    @Test
    @DisplayName("개발자 토큰 발급 성공 - ADMIN 권한으로 30일 만료 토큰 발급")
    void generateDeveloperToken_Success() {
        // Given
        UserAuthProfile adminAuthProfile = new UserAuthProfile(
                userId,
                Role.ADMIN,
                UserStatus.ACTIVE,
                "admin@example.com",
                true
        );
        String developerToken = "long.lived.access.token";

        given(userAuthAccountService.findAuthProfileById(userId)).willReturn(Optional.of(adminAuthProfile));
        given(jwtUtil.generateLongLivedAccessToken(userId, Role.ADMIN)).willReturn(developerToken);

        // When
        DeveloperTokenResult result = authCommandService.generateDeveloperToken(new GenerateDeveloperTokenCommand(userId));

        // Then
        assertThat(result.developerToken()).isEqualTo(developerToken);
        verify(userAuthAccountService).findAuthProfileById(userId);
        verify(jwtUtil).generateLongLivedAccessToken(userId, Role.ADMIN);
    }

    @Test
    @DisplayName("개발자 토큰 발급 실패 - 사용자를 찾을 수 없음")
    void generateDeveloperToken_Fail_UserNotFound() {
        // Given
        given(userAuthAccountService.findAuthProfileById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authCommandService.generateDeveloperToken(new GenerateDeveloperTokenCommand(userId)))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND);

        verify(userAuthAccountService).findAuthProfileById(userId);
        verify(jwtUtil, never()).generateLongLivedAccessToken(anyLong(), any(Role.class));
    }

    @Test
    @DisplayName("개발자 토큰 발급 실패 - ADMIN 권한이 아닌 사용자")
    void generateDeveloperToken_Fail_InsufficientPermissions() {
        // Given
        given(userAuthAccountService.findAuthProfileById(userId)).willReturn(Optional.of(userAuthProfile));

        // When & Then
        assertThatThrownBy(() -> authCommandService.generateDeveloperToken(new GenerateDeveloperTokenCommand(userId)))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.FORBIDDEN_INSUFFICIENT_PERMISSIONS);

        verify(userAuthAccountService).findAuthProfileById(userId);
        verify(jwtUtil, never()).generateLongLivedAccessToken(anyLong(), any(Role.class));
    }
}
