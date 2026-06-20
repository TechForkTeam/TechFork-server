package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2LoginRedirectUrlFactoryTest {

    private static final Long USER_ID = 1L;
    private static final String REDIRECT_URI = "http://localhost:5173/auth/callback?legacy=ignored";
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

            String redirectUrl = redirectUrlFactory.createSuccessRedirectUrl(principal);
            Map<String, String> queryParams = queryParams(redirectUrl);

            assertThat(redirectUrl).startsWith("http://localhost:5173/auth/callback?");
            assertThat(queryParams)
                    .containsEntry("registered", "true")
                    .containsEntry("email", "dev user@example.com")
                    .doesNotContainKey("token");
        }

        @Test
        @DisplayName("PENDING 사용자는 등록 미완료 상태로 성공 URL을 생성한다")
        void pendingUser_ReturnsRegisteredFalse() {
            UserPrincipal principal = principal(UserStatus.PENDING, "pending@example.com");

            String redirectUrl = redirectUrlFactory.createSuccessRedirectUrl(principal);
            Map<String, String> queryParams = queryParams(redirectUrl);

            assertThat(queryParams)
                    .containsEntry("registered", "false")
                    .containsEntry("email", "pending@example.com")
                    .doesNotContainKey("token");
        }

        @Test
        @DisplayName("이메일이 없으면 빈 이메일 파라미터로 성공 URL을 생성한다")
        void nullEmail_ReturnsEmptyEmailParameter() {
            UserPrincipal principal = principal(UserStatus.ACTIVE, null);

            String redirectUrl = redirectUrlFactory.createSuccessRedirectUrl(principal);
            Map<String, String> queryParams = queryParams(redirectUrl);

            assertThat(queryParams)
                    .containsEntry("registered", "true")
                    .containsEntry("email", "")
                    .doesNotContainKey("token");
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

    private Map<String, String> queryParams(String redirectUrl) {
        String rawQuery = URI.create(redirectUrl).getRawQuery();
        Map<String, String> queryParams = new LinkedHashMap<>();

        Arrays.stream(rawQuery.split("&"))
                .map(parameter -> parameter.split("=", 2))
                .forEach(parameter -> queryParams.put(
                        decode(parameter[0]),
                        parameter.length > 1 ? decode(parameter[1]) : ""
                ));

        return queryParams;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
