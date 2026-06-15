package com.techfork.auth.application.command;

import com.techfork.auth.application.command.input.KakaoLoginCommand;
import com.techfork.auth.application.command.result.KakaoLoginResult;
import com.techfork.auth.infrastructure.kakao.KakaoOAuthService;
import com.techfork.auth.infrastructure.kakao.response.KakaoUserInfoResponse;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.service.RefreshTokenService;
import com.techfork.useraccount.application.auth.UserAuthAccountService;
import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KakaoLoginCommandServiceTest {

    @Mock
    private KakaoOAuthService kakaoOAuthService;

    @Mock
    private UserAuthAccountService userAuthAccountService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private KakaoLoginCommandService kakaoLoginCommandService;

    private String newAccessToken;
    private String newRefreshToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        newAccessToken = "new.access.token";
        newRefreshToken = "new.refresh.token";
        userId = 1L;
    }

    @Test
    @DisplayName("iOS 카카오 로그인 성공 - REST id를 Kakao service user ID socialId로 사용해 신규 회원 가입")
    void login_Success_NewUser() {
        // Given
        String kakaoAccessToken = "kakao.access.token";
        Long kakaoRestUserId = 12345L;
        String kakaoServiceUserId = "12345";
        String email = "newuser@kakao.com";
        String profileImageUrl = "https://example.com/profile.jpg";
        long refreshTokenExpiration = 900000L;

        KakaoUserInfoResponse kakaoUserInfo = kakaoUserInfo(kakaoRestUserId, email, profileImageUrl);
        UserAuthProfile newUserProfile = new UserAuthProfile(userId, Role.USER, UserStatus.PENDING, email, false);
        JwtDTO tokens = JwtDTO.of(newAccessToken, newRefreshToken);

        given(kakaoOAuthService.getUserInfo(kakaoAccessToken)).willReturn(kakaoUserInfo);
        given(userAuthAccountService.getOrCreateSocialAuthProfile(
                SocialType.KAKAO,
                kakaoServiceUserId,
                email,
                profileImageUrl
        )).willReturn(newUserProfile);
        given(jwtUtil.generateTokens(userId, Role.USER)).willReturn(tokens);
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(refreshTokenExpiration);

        // When
        KakaoLoginResult result = kakaoLoginCommandService.login(new KakaoLoginCommand(kakaoAccessToken));

        // Then
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.refreshToken()).isEqualTo(newRefreshToken);
        assertThat(result.refreshTokenExpiration()).isEqualTo(refreshTokenExpiration);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.isRegistered()).isFalse();

        verify(kakaoOAuthService).getUserInfo(kakaoAccessToken);
        verify(userAuthAccountService).getOrCreateSocialAuthProfile(
                SocialType.KAKAO,
                kakaoServiceUserId,
                email,
                profileImageUrl
        );
        verify(jwtUtil).generateTokens(userId, Role.USER);
        verify(refreshTokenService).saveRefreshToken(eq(userId), eq(newRefreshToken), anyLong());
    }

    @Test
    @DisplayName("iOS 카카오 로그인 성공 - 기존 회원 로그인")
    void login_Success_ExistingUser() {
        // Given
        String kakaoAccessToken = "kakao.access.token";
        Long kakaoRestUserId = 12345L;
        String kakaoServiceUserId = "12345";
        String email = "existinguser@kakao.com";
        String profileImageUrl = "https://example.com/profile.jpg";
        long refreshTokenExpiration = 900000L;

        KakaoUserInfoResponse kakaoUserInfo = kakaoUserInfo(kakaoRestUserId, email, profileImageUrl);
        UserAuthProfile existingUserProfile = new UserAuthProfile(userId, Role.USER, UserStatus.ACTIVE, email, true);
        JwtDTO tokens = JwtDTO.of(newAccessToken, newRefreshToken);

        given(kakaoOAuthService.getUserInfo(kakaoAccessToken)).willReturn(kakaoUserInfo);
        given(userAuthAccountService.getOrCreateSocialAuthProfile(
                SocialType.KAKAO,
                kakaoServiceUserId,
                email,
                profileImageUrl
        )).willReturn(existingUserProfile);
        given(jwtUtil.generateTokens(userId, Role.USER)).willReturn(tokens);
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(refreshTokenExpiration);

        // When
        KakaoLoginResult result = kakaoLoginCommandService.login(new KakaoLoginCommand(kakaoAccessToken));

        // Then
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.refreshToken()).isEqualTo(newRefreshToken);
        assertThat(result.refreshTokenExpiration()).isEqualTo(refreshTokenExpiration);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.isRegistered()).isTrue();

        verify(kakaoOAuthService).getUserInfo(kakaoAccessToken);
        verify(userAuthAccountService).getOrCreateSocialAuthProfile(
                SocialType.KAKAO,
                kakaoServiceUserId,
                email,
                profileImageUrl
        );
        verify(jwtUtil).generateTokens(userId, Role.USER);
        verify(refreshTokenService).saveRefreshToken(eq(userId), eq(newRefreshToken), anyLong());
    }

    private KakaoUserInfoResponse kakaoUserInfo(Long kakaoRestUserId, String email, String profileImageUrl) {
        KakaoUserInfoResponse.Profile profile = new KakaoUserInfoResponse.Profile(profileImageUrl);
        KakaoUserInfoResponse.KakaoAccount kakaoAccount = new KakaoUserInfoResponse.KakaoAccount(email, profile);
        return new KakaoUserInfoResponse(kakaoRestUserId, kakaoAccount);
    }
}
