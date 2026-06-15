package com.techfork.auth.security.oauth;

import com.techfork.useraccount.application.auth.UserAuthAccountService;
import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.enums.UserStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

    private static final String SOCIAL_ID = "apple-sub-123";
    private static final String EMAIL = "apple-user@example.com";
    private static final String PROFILE_IMAGE = "https://cdn.example.com/apple-user.png";

    @Mock
    private UserAuthAccountService userAuthAccountService;

    private CustomOidcUserService customOidcUserService;

    @BeforeEach
    void setUp() {
        customOidcUserService = new CustomOidcUserService(userAuthAccountService);
        customOidcUserService.setRetrieveUserInfo(request -> false);
    }

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("신규 Apple 인증 프로필을 UserPrincipal로 반환한다")
        void loadUser_NewAppleUser_ReturnsPrincipal() {
            UserAuthProfile userAuthProfile = new UserAuthProfile(1L, Role.USER, UserStatus.PENDING, EMAIL, false);
            given(userAuthAccountService.getOrCreateSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            )).willReturn(userAuthProfile);

            OidcUser oidcUser = customOidcUserService.loadUser(userRequest(claims(SOCIAL_ID, EMAIL, PROFILE_IMAGE)));

            assertThat(oidcUser).isInstanceOf(UserPrincipal.class);
            UserPrincipal principal = (UserPrincipal) oidcUser;
            assertThat(principal.getId()).isEqualTo(1L);
            assertThat(principal.getRole()).isEqualTo(Role.USER);
            assertThat(principal.getStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(principal.getEmail()).isEqualTo(EMAIL);
            verify(userAuthAccountService).getOrCreateSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            );
        }

        @Test
        @DisplayName("신규 Kakao OIDC 인증 프로필을 UserPrincipal로 반환한다")
        void loadUser_NewKakaoOidcUser_ReturnsPrincipal() {
            String kakaoSocialId = "kakao-sub-456";
            String kakaoEmail = "kakao-user@example.com";
            String kakaoProfileImage = "https://cdn.example.com/kakao-user.png";
            UserAuthProfile userAuthProfile = new UserAuthProfile(4L, Role.USER, UserStatus.PENDING, kakaoEmail, false);
            given(userAuthAccountService.getOrCreateSocialAuthProfile(
                    SocialType.KAKAO,
                    kakaoSocialId,
                    kakaoEmail,
                    kakaoProfileImage
            )).willReturn(userAuthProfile);

            OidcUser oidcUser = customOidcUserService.loadUser(
                    userRequest("kakao", claims(kakaoSocialId, kakaoEmail, kakaoProfileImage))
            );

            assertThat(oidcUser).isInstanceOf(UserPrincipal.class);
            UserPrincipal principal = (UserPrincipal) oidcUser;
            assertThat(principal.getId()).isEqualTo(4L);
            assertThat(principal.getRole()).isEqualTo(Role.USER);
            assertThat(principal.getStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(principal.getEmail()).isEqualTo(kakaoEmail);
            verify(userAuthAccountService).getOrCreateSocialAuthProfile(
                    SocialType.KAKAO,
                    kakaoSocialId,
                    kakaoEmail,
                    kakaoProfileImage
            );
        }

        @Test
        @DisplayName("기존 Apple 인증 프로필을 UserPrincipal로 반환한다")
        void loadUser_ExistingAppleUser_ReturnsPrincipal() {
            UserAuthProfile existingUserProfile = new UserAuthProfile(2L, Role.USER, UserStatus.ACTIVE, EMAIL, true);
            given(userAuthAccountService.getOrCreateSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            )).willReturn(existingUserProfile);

            OidcUser oidcUser = customOidcUserService.loadUser(userRequest(claims(SOCIAL_ID, EMAIL, PROFILE_IMAGE)));

            UserPrincipal principal = (UserPrincipal) oidcUser;
            assertThat(principal.getId()).isEqualTo(2L);
            assertThat(principal.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(principal.getEmail()).isEqualTo(EMAIL);
            verify(userAuthAccountService).getOrCreateSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            );
        }

        @Test
        @DisplayName("탈퇴 Apple 사용자의 재활성화된 PENDING 인증 프로필을 UserPrincipal로 반환한다")
        void loadUser_WithdrawnAppleUser_ReturnsReactivatedPendingPrincipal() {
            UserAuthProfile reactivatedUserProfile = new UserAuthProfile(3L, Role.USER, UserStatus.PENDING, EMAIL, false);
            given(userAuthAccountService.getOrCreateSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            )).willReturn(reactivatedUserProfile);

            OidcUser oidcUser = customOidcUserService.loadUser(userRequest(claims(SOCIAL_ID, EMAIL, PROFILE_IMAGE)));

            UserPrincipal principal = (UserPrincipal) oidcUser;
            assertThat(principal.getId()).isEqualTo(3L);
            assertThat(principal.getStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(principal.getEmail()).isEqualTo(EMAIL);
            verify(userAuthAccountService).getOrCreateSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            );
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {

        @Test
        @DisplayName("sub 클레임이 없으면 예외를 던진다")
        void loadUser_MissingSubject_ThrowsException() {
            Map<String, Object> claims = claims(null, EMAIL, PROFILE_IMAGE);

            assertThatThrownBy(() -> customOidcUserService.loadUser(userRequest(claims)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sub");

            verify(userAuthAccountService, never()).getOrCreateSocialAuthProfile(any(), any(), any(), any());
        }

        @Test
        @DisplayName("email 클레임이 없으면 예외를 던지고 인증 프로필을 조회하지 않는다")
        void loadUser_MissingEmail_ThrowsOAuth2AuthenticationException() {
            Map<String, Object> claims = claims(SOCIAL_ID, null, PROFILE_IMAGE);

            assertThatThrownBy(() -> customOidcUserService.loadUser(userRequest(claims)))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .satisfies(exception -> assertThat(((OAuth2AuthenticationException) exception)
                            .getError()
                            .getErrorCode()).isEqualTo("email not found"));

            verify(userAuthAccountService, never()).getOrCreateSocialAuthProfile(any(), any(), any(), any());
        }
    }

    private OidcUserRequest userRequest(Map<String, Object> claims) {
        return userRequest("apple", claims);
    }

    private OidcUserRequest userRequest(String registrationId, Map<String, Object> claims) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(300);
        OidcIdToken idToken = new OidcIdToken("id-token", issuedAt, expiresAt, claims);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                issuedAt,
                expiresAt,
                Set.of("openid")
        );

        return new OidcUserRequest(clientRegistration(registrationId), accessToken, idToken);
    }

    private ClientRegistration clientRegistration(String registrationId) {
        return ClientRegistration.withRegistrationId(registrationId)
                .clientId("techfork-client")
                .clientSecret("techfork-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://techfork.site/login/oauth2/code/" + registrationId)
                .scope("openid")
                .authorizationUri("https://auth.example.com/oauth/authorize")
                .tokenUri("https://auth.example.com/oauth/token")
                .jwkSetUri("https://auth.example.com/oauth/keys")
                .issuerUri("https://auth.example.com")
                .userNameAttributeName("sub")
                .clientName(registrationId)
                .build();
    }

    private Map<String, Object> claims(String subject, String email, String profileImage) {
        Map<String, Object> claims = new HashMap<>();
        if (subject != null) {
            claims.put("sub", subject);
        }
        if (email != null) {
            claims.put("email", email);
        }
        if (profileImage != null) {
            claims.put("picture", profileImage);
        }
        return claims;
    }
}
