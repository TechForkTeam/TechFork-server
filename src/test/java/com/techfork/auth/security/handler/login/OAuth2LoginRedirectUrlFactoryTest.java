package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2LoginRedirectUrlFactoryTest {

    private static final Long USER_ID = 1L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REDIRECT_URI = "http://localhost:5173/auth/callback?registered=%s&token=%s&email=%s";
    private static final String LOGIN_FAILURE_REDIRECT_URI = "http://localhost:5173/login?error=true";

    private OAuth2LoginRedirectUrlFactory redirectUrlFactory;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRedirectUri(REDIRECT_URI);
        jwtProperties.setLoginFailureRedirectUri(LOGIN_FAILURE_REDIRECT_URI);
        redirectUrlFactory = new OAuth2LoginRedirectUrlFactory(jwtProperties);
    }

    @Nested
    @DisplayName("createSuccessRedirectUrl")
    class CreateSuccessRedirectUrl {

        @Test
        @DisplayName("ACTIVE 사용자는 등록 완료 상태와 인코딩된 이메일로 성공 URL을 생성한다")
        void activeUser_ReturnsRegisteredTrueAndEncodedEmail() {
            UserPrincipal principal = principal(UserStatus.ACTIVE, "dev user@example.com");

            String redirectUrl = redirectUrlFactory.createSuccessRedirectUrl(principal, ACCESS_TOKEN);

            assertThat(redirectUrl).isEqualTo(String.format(
                    REDIRECT_URI,
                    true,
                    ACCESS_TOKEN,
                    UriUtils.encode("dev user@example.com", StandardCharsets.UTF_8)
            ));
        }

        @Test
        @DisplayName("PENDING 사용자는 등록 미완료 상태로 성공 URL을 생성한다")
        void pendingUser_ReturnsRegisteredFalse() {
            UserPrincipal principal = principal(UserStatus.PENDING, "pending@example.com");

            String redirectUrl = redirectUrlFactory.createSuccessRedirectUrl(principal, ACCESS_TOKEN);

            assertThat(redirectUrl).isEqualTo(String.format(
                    REDIRECT_URI,
                    false,
                    ACCESS_TOKEN,
                    UriUtils.encode("pending@example.com", StandardCharsets.UTF_8)
            ));
        }

        @Test
        @DisplayName("이메일이 없으면 빈 이메일 파라미터로 성공 URL을 생성한다")
        void nullEmail_ReturnsEmptyEmailParameter() {
            UserPrincipal principal = principal(UserStatus.ACTIVE, null);

            String redirectUrl = redirectUrlFactory.createSuccessRedirectUrl(principal, ACCESS_TOKEN);

            assertThat(redirectUrl).isEqualTo(String.format(
                    REDIRECT_URI,
                    true,
                    ACCESS_TOKEN,
                    ""
            ));
        }
    }

    @Nested
    @DisplayName("createFailureRedirectUrl")
    class CreateFailureRedirectUrl {

        @Test
        @DisplayName("실패 리다이렉트 URI에 공개 에러 코드를 query parameter로 추가한다")
        void appendsPublicErrorCodeQueryParameter() {
            String redirectUrl = redirectUrlFactory.createFailureRedirectUrl();

            assertThat(redirectUrl).isEqualTo(LOGIN_FAILURE_REDIRECT_URI + "&errorCode=oauth_failed");
        }
    }

    private UserPrincipal principal(UserStatus status, String email) {
        return UserPrincipal.builder()
                .id(USER_ID)
                .role(Role.USER)
                .status(status)
                .email(email)
                .build();
    }
}
