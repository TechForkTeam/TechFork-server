package com.techfork.auth.security.oauth;

import com.techfork.useraccount.application.auth.UserAuthAccountService;
import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.enums.UserStatus;
import java.time.Instant;
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

    @Mock
    private OidcSocialIdentityExtractor socialIdentityExtractor;

    private CustomOidcUserService customOidcUserService;

    @BeforeEach
    void setUp() {
        customOidcUserService = new CustomOidcUserService(userAuthAccountService, socialIdentityExtractor);
        customOidcUserService.setRetrieveUserInfo(request -> false);
    }

    @Nested
    @DisplayName("loadUser")
    class LoadUser {

        @Test
        @DisplayName("신규 Apple 인증 프로필을 UserPrincipal로 반환한다")
        void newAppleUser_ReturnsPrincipal() {
            UserAuthProfile userAuthProfile = new UserAuthProfile(1L, Role.USER, UserStatus.PENDING, EMAIL, false);
            givenSocialIdentity(SocialType.APPLE, SOCIAL_ID, EMAIL, PROFILE_IMAGE);
            given(userAuthAccountService.getOrCreateSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            )).willReturn(userAuthProfile);

            OidcUser oidcUser = customOidcUserService.loadUser(userRequest());

            assertPrincipal(oidcUser, 1L, Role.USER, UserStatus.PENDING, EMAIL);
            verifySocialIdentityExtracted();
            verify(userAuthAccountService).getOrCreateSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            );
        }

        @Test
        @DisplayName("Kakao 인증 식별자로 인증 프로필을 조회하고 UserPrincipal로 반환한다")
        void kakaoIdentity_ReturnsPrincipal() {
            String kakaoServiceUserId = "12345";
            String kakaoEmail = "kakao-user@example.com";
            String kakaoProfileImage = "https://cdn.example.com/kakao-user.png";
            UserAuthProfile userAuthProfile = new UserAuthProfile(4L, Role.USER, UserStatus.PENDING, kakaoEmail, false);
            givenSocialIdentity(SocialType.KAKAO, kakaoServiceUserId, kakaoEmail, kakaoProfileImage);
            given(userAuthAccountService.getOrCreateSocialAuthProfile(
                    SocialType.KAKAO,
                    kakaoServiceUserId,
                    kakaoEmail,
                    kakaoProfileImage
            )).willReturn(userAuthProfile);

            OidcUser oidcUser = customOidcUserService.loadUser(userRequest("kakao"));

            assertPrincipal(oidcUser, 4L, Role.USER, UserStatus.PENDING, kakaoEmail);
            verifySocialIdentityExtracted();
            verify(userAuthAccountService).getOrCreateSocialAuthProfile(
                    SocialType.KAKAO,
                    kakaoServiceUserId,
                    kakaoEmail,
                    kakaoProfileImage
            );
        }

        @Test
        @DisplayName("기존 Apple 인증 프로필을 UserPrincipal로 반환한다")
        void existingAppleUser_ReturnsPrincipal() {
            UserAuthProfile existingUserProfile = new UserAuthProfile(2L, Role.USER, UserStatus.ACTIVE, EMAIL, true);
            givenSocialIdentity(SocialType.APPLE, SOCIAL_ID, EMAIL, PROFILE_IMAGE);
            given(userAuthAccountService.getOrCreateSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            )).willReturn(existingUserProfile);

            OidcUser oidcUser = customOidcUserService.loadUser(userRequest());

            assertPrincipal(oidcUser, 2L, Role.USER, UserStatus.ACTIVE, EMAIL);
            verifySocialIdentityExtracted();
            verify(userAuthAccountService).getOrCreateSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            );
        }

        @Test
        @DisplayName("탈퇴 Apple 사용자의 재활성화된 PENDING 인증 프로필을 UserPrincipal로 반환한다")
        void withdrawnAppleUser_ReturnsReactivatedPendingPrincipal() {
            UserAuthProfile reactivatedUserProfile = new UserAuthProfile(3L, Role.USER, UserStatus.PENDING, EMAIL, false);
            givenSocialIdentity(SocialType.APPLE, SOCIAL_ID, EMAIL, PROFILE_IMAGE);
            given(userAuthAccountService.getOrCreateSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            )).willReturn(reactivatedUserProfile);

            OidcUser oidcUser = customOidcUserService.loadUser(userRequest());

            assertPrincipal(oidcUser, 3L, Role.USER, UserStatus.PENDING, EMAIL);
            verifySocialIdentityExtracted();
            verify(userAuthAccountService).getOrCreateSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            );
        }

        @Test
        @DisplayName("소셜 식별자 추출에 실패하면 인증 프로필을 조회하지 않는다")
        void socialIdentityExtractionFails_DoesNotRequestAuthProfile() {
            given(socialIdentityExtractor.extract(any(OidcUserRequest.class), any(OidcUser.class)))
                    .willThrow(new OAuth2AuthenticationException("socialId(sub) not found"));

            assertThatThrownBy(() -> customOidcUserService.loadUser(userRequest()))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .satisfies(exception -> assertThat(((OAuth2AuthenticationException) exception)
                            .getError()
                            .getErrorCode()).isEqualTo("socialId(sub) not found"));

            verify(userAuthAccountService, never()).getOrCreateSocialAuthProfile(any(), any(), any(), any());
        }

        @Test
        @DisplayName("이메일 추출에 실패하면 인증 프로필을 조회하지 않는다")
        void emailExtractionFails_DoesNotRequestAuthProfile() {
            given(socialIdentityExtractor.extract(any(OidcUserRequest.class), any(OidcUser.class)))
                    .willThrow(new OAuth2AuthenticationException("email not found"));

            assertThatThrownBy(() -> customOidcUserService.loadUser(userRequest()))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .satisfies(exception -> assertThat(((OAuth2AuthenticationException) exception)
                            .getError()
                            .getErrorCode()).isEqualTo("email not found"));

            verify(userAuthAccountService, never()).getOrCreateSocialAuthProfile(any(), any(), any(), any());
        }
    }

    private void givenSocialIdentity(SocialType socialType, String socialId, String email, String profileImage) {
        given(socialIdentityExtractor.extract(any(OidcUserRequest.class), any(OidcUser.class)))
                .willReturn(new OidcSocialIdentity(socialType, socialId, email, profileImage));
    }

    private void verifySocialIdentityExtracted() {
        verify(socialIdentityExtractor).extract(any(OidcUserRequest.class), any(OidcUser.class));
    }

    private void assertPrincipal(OidcUser oidcUser, Long id, Role role, UserStatus status, String email) {
        assertThat(oidcUser).isInstanceOf(UserPrincipal.class);
        UserPrincipal principal = (UserPrincipal) oidcUser;
        assertThat(principal.getId()).isEqualTo(id);
        assertThat(principal.getRole()).isEqualTo(role);
        assertThat(principal.getStatus()).isEqualTo(status);
        assertThat(principal.getEmail()).isEqualTo(email);
    }

    private OidcUserRequest userRequest() {
        return userRequest("apple");
    }

    private OidcUserRequest userRequest(String registrationId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(300);
        OidcIdToken idToken = new OidcIdToken("id-token", issuedAt, expiresAt, Map.of(
                "sub", "id-token-sub",
                "email", EMAIL,
                "picture", PROFILE_IMAGE
        ));
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
}
