package com.techfork.domain.auth.service;

import com.techfork.auth.application.AuthConverter;
import com.techfork.auth.application.KakaoLoginService;
import com.techfork.auth.application.dto.KakaoLoginResponse;
import com.techfork.auth.infrastructure.kakao.KakaoOAuthService;
import com.techfork.auth.infrastructure.kakao.dto.KakaoUserInfoResponse;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.enums.UserStatus;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.auth.security.service.RefreshTokenService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KakaoLoginServiceTest {

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

    @Mock
    private AuthConverter authConverter;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private KakaoLoginService kakaoLoginService;

    private String newAccessToken;
    private String newRefreshToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kakaoLoginService, "domain", "localhost");

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

        KakaoUserInfoResponse.Profile profile = new KakaoUserInfoResponse.Profile("https://example.com/profile.jpg");
        KakaoUserInfoResponse.KakaoAccount kakaoAccount = new KakaoUserInfoResponse.KakaoAccount(email, profile);
        KakaoUserInfoResponse kakaoUserInfo = new KakaoUserInfoResponse(12345L, kakaoAccount);

        User newUser = User.createSocialUser(SocialType.KAKAO, socialId, email, profileImageUrl);
        ReflectionTestUtils.setField(newUser, "id", userId);

        JwtDTO tokens = JwtDTO.of(newAccessToken, newRefreshToken);
        KakaoLoginResponse expectedResponse = KakaoLoginResponse.builder()
                .accessToken(newAccessToken)
                .userId(userId)
                .isRegistered(false)
                .build();

        given(kakaoOAuthService.getUserInfo(kakaoAccessToken)).willReturn(kakaoUserInfo);
        given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, socialId)).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(newUser);
        given(jwtUtil.generateTokens(userId, Role.USER)).willReturn(tokens);
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(900000L);
        given(authConverter.toKakaoLoginResponse(newAccessToken, newUser)).willReturn(expectedResponse);

        // When
        KakaoLoginResponse result = kakaoLoginService.login(kakaoAccessToken, response);

        // Then
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.isRegistered()).isFalse();

        verify(kakaoOAuthService).getUserInfo(kakaoAccessToken);
        verify(userRepository).findBySocialTypeAndSocialId(SocialType.KAKAO, socialId);
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateTokens(userId, Role.USER);
        verify(refreshTokenService).saveRefreshToken(eq(userId), eq(newRefreshToken), anyLong());
        verify(response).addHeader(eq("Set-Cookie"), anyString());
        verify(authConverter).toKakaoLoginResponse(newAccessToken, newUser);
    }

    @Test
    @DisplayName("iOS 카카오 로그인 성공 - 기존 회원 로그인")
    void login_Success_ExistingUser() {
        // Given
        String kakaoAccessToken = "kakao.access.token";
        String socialId = "12345";
        String email = "existinguser@kakao.com";
        String profileImageUrl = "test.png";

        KakaoUserInfoResponse.Profile profile = new KakaoUserInfoResponse.Profile("https://example.com/profile.jpg");
        KakaoUserInfoResponse.KakaoAccount kakaoAccount = new KakaoUserInfoResponse.KakaoAccount(email, profile);
        KakaoUserInfoResponse kakaoUserInfo = new KakaoUserInfoResponse(12345L, kakaoAccount);

        User existingUser = User.createSocialUser(SocialType.KAKAO, socialId, email, profileImageUrl);
        ReflectionTestUtils.setField(existingUser, "id", userId);
        ReflectionTestUtils.setField(existingUser, "status", UserStatus.ACTIVE);

        JwtDTO tokens = JwtDTO.of(newAccessToken, newRefreshToken);
        KakaoLoginResponse expectedResponse = KakaoLoginResponse.builder()
                .accessToken(newAccessToken)
                .userId(userId)
                .isRegistered(true)
                .build();

        given(kakaoOAuthService.getUserInfo(kakaoAccessToken)).willReturn(kakaoUserInfo);
        given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, socialId)).willReturn(Optional.of(existingUser));
        given(jwtUtil.generateTokens(userId, Role.USER)).willReturn(tokens);
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(900000L);
        given(authConverter.toKakaoLoginResponse(newAccessToken, existingUser)).willReturn(expectedResponse);

        // When
        KakaoLoginResponse result = kakaoLoginService.login(kakaoAccessToken, response);

        // Then
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.isRegistered()).isTrue();

        verify(kakaoOAuthService).getUserInfo(kakaoAccessToken);
        verify(userRepository).findBySocialTypeAndSocialId(SocialType.KAKAO, socialId);
        verify(userRepository, never()).save(any(User.class));
        verify(jwtUtil).generateTokens(userId, Role.USER);
        verify(refreshTokenService).saveRefreshToken(eq(userId), eq(newRefreshToken), anyLong());
        verify(response).addHeader(eq("Set-Cookie"), anyString());
        verify(authConverter).toKakaoLoginResponse(newAccessToken, existingUser);
    }
}
