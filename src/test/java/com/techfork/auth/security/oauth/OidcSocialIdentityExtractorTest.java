package com.techfork.auth.security.oauth;

import com.techfork.useraccount.domain.enums.SocialType;
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

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OidcSocialIdentityExtractorTest {

    private static final String SOCIAL_ID = "apple-sub-123";
    private static final String EMAIL = "apple-user@example.com";
    private static final String PROFILE_IMAGE = "https://cdn.example.com/apple-user.png";

    @Mock
    private OidcUser oidcUser;

    private OidcSocialIdentityExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new OidcSocialIdentityExtractor();
    }

    @Nested
    @DisplayName("extract")
    class Extract {

        @Test
        @DisplayName("Apple OIDC user에서 소셜 인증 식별자를 추출한다")
        void appleUser_ReturnsAppleSocialIdentity() {
            givenOidcUserClaims(SOCIAL_ID, EMAIL, PROFILE_IMAGE);

            OidcSocialIdentity identity = extractor.extract(userRequest("apple"), oidcUser);

            assertThat(identity.socialType()).isEqualTo(SocialType.APPLE);
            assertThat(identity.socialId()).isEqualTo(SOCIAL_ID);
            assertThat(identity.email()).isEqualTo(EMAIL);
            assertThat(identity.profileImage()).isEqualTo(PROFILE_IMAGE);
        }

        @Test
        @DisplayName("Kakao OIDC sub를 REST id와 같은 service user ID socialId로 추출한다")
        void kakaoUser_ReturnsKakaoServiceUserId() {
            String kakaoServiceUserId = "12345";
            String kakaoEmail = "kakao-user@example.com";
            givenOidcUserClaims(kakaoServiceUserId, kakaoEmail, null);

            OidcSocialIdentity identity = extractor.extract(userRequest("kakao"), oidcUser);

            assertThat(identity.socialType()).isEqualTo(SocialType.KAKAO);
            assertThat(identity.socialId()).isEqualTo(kakaoServiceUserId);
            assertThat(identity.email()).isEqualTo(kakaoEmail);
            assertThat(identity.profileImage()).isNull();
        }

        @Test
        @DisplayName("sub 클레임이 없으면 예외를 던진다")
        void missingSubject_ThrowsOAuth2AuthenticationException() {
            givenOidcUserClaims(null, EMAIL, PROFILE_IMAGE);

            assertThatThrownBy(() -> extractor.extract(userRequest("apple"), oidcUser))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .satisfies(exception -> assertThat(((OAuth2AuthenticationException) exception)
                            .getError()
                            .getErrorCode()).isEqualTo("socialId(sub) not found"));
        }

        @Test
        @DisplayName("email 클레임이 없으면 예외를 던진다")
        void missingEmail_ThrowsOAuth2AuthenticationException() {
            givenOidcUserClaims(SOCIAL_ID, null, PROFILE_IMAGE);

            assertThatThrownBy(() -> extractor.extract(userRequest("apple"), oidcUser))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .satisfies(exception -> assertThat(((OAuth2AuthenticationException) exception)
                            .getError()
                            .getErrorCode()).isEqualTo("email not found"));
        }
    }

    private void givenOidcUserClaims(String subject, String email, String profileImage) {
        given(oidcUser.getAttribute("sub")).willReturn(subject);
        if (subject != null) {
            given(oidcUser.getAttribute("email")).willReturn(email);
        }
        if (subject != null && email != null) {
            given(oidcUser.getAttribute("picture")).willReturn(profileImage);
        }
    }

    private OidcUserRequest userRequest(String registrationId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(300);
        OidcIdToken idToken = new OidcIdToken("id-token", issuedAt, expiresAt, Map.of("sub", "id-token-sub"));
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
