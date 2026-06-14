package com.techfork.auth.application.command;

import com.techfork.auth.application.command.input.KakaoLoginCommand;
import com.techfork.auth.application.command.result.KakaoLoginResult;
import com.techfork.auth.infrastructure.kakao.KakaoOAuthService;
import com.techfork.auth.infrastructure.kakao.response.KakaoUserInfoResponse;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.service.RefreshTokenService;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.enums.UserStatus;
import com.techfork.useraccount.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KakaoLoginCommandServiceTest {

    @Mock
    private KakaoOAuthService kakaoOAuthService;

    @Mock
    private UserRepository userRepository;

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
    @DisplayName("iOS 카카오 로그인 성공 - 신규 회원 가입")
    void login_Success_NewUser() {
        // Given
        String kakaoAccessToken = "kakao.access.token";
        String socialId = "12345";
        String email = "newuser@kakao.com";
        String profileImageUrl = "test.png";
        long refreshTokenExpiration = 900000L;

        KakaoUserInfoResponse.Profile profile = new KakaoUserInfoResponse.Profile("https://example.com/profile.jpg");
        KakaoUserInfoResponse.KakaoAccount kakaoAccount = new KakaoUserInfoResponse.KakaoAccount(email, profile);
        KakaoUserInfoResponse kakaoUserInfo = new KakaoUserInfoResponse(12345L, kakaoAccount);

        User newUser = User.createSocialUser(SocialType.KAKAO, socialId, email, profileImageUrl);
        ReflectionTestUtils.setField(newUser, "id", userId);

        JwtDTO tokens = JwtDTO.of(newAccessToken, newRefreshToken);

        given(kakaoOAuthService.getUserInfo(kakaoAccessToken)).willReturn(kakaoUserInfo);
        given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, socialId)).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(newUser);
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
        verify(userRepository).findBySocialTypeAndSocialId(SocialType.KAKAO, socialId);
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateTokens(userId, Role.USER);
        verify(refreshTokenService).saveRefreshToken(eq(userId), eq(newRefreshToken), anyLong());
    }

    @Test
    @DisplayName("iOS 카카오 로그인 성공 - 기존 회원 로그인")
    void login_Success_ExistingUser() {
        // Given
        String kakaoAccessToken = "kakao.access.token";
        String socialId = "12345";
        String email = "existinguser@kakao.com";
        String profileImageUrl = "test.png";
        long refreshTokenExpiration = 900000L;

        KakaoUserInfoResponse.Profile profile = new KakaoUserInfoResponse.Profile("https://example.com/profile.jpg");
        KakaoUserInfoResponse.KakaoAccount kakaoAccount = new KakaoUserInfoResponse.KakaoAccount(email, profile);
        KakaoUserInfoResponse kakaoUserInfo = new KakaoUserInfoResponse(12345L, kakaoAccount);

        User existingUser = User.createSocialUser(SocialType.KAKAO, socialId, email, profileImageUrl);
        ReflectionTestUtils.setField(existingUser, "id", userId);
        ReflectionTestUtils.setField(existingUser, "status", UserStatus.ACTIVE);

        JwtDTO tokens = JwtDTO.of(newAccessToken, newRefreshToken);

        given(kakaoOAuthService.getUserInfo(kakaoAccessToken)).willReturn(kakaoUserInfo);
        given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, socialId)).willReturn(Optional.of(existingUser));
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
        verify(userRepository).findBySocialTypeAndSocialId(SocialType.KAKAO, socialId);
        verify(userRepository, never()).save(any(User.class));
        verify(jwtUtil).generateTokens(userId, Role.USER);
        verify(refreshTokenService).saveRefreshToken(eq(userId), eq(newRefreshToken), anyLong());
    }
}
