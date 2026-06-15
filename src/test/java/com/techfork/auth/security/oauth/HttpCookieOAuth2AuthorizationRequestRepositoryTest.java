package com.techfork.auth.security.oauth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Map;
import java.util.Set;

import static com.techfork.auth.security.oauth.HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    private final HttpCookieOAuth2AuthorizationRequestRepository repository =
            new HttpCookieOAuth2AuthorizationRequestRepository();

    @Nested
    @DisplayName("saveAuthorizationRequest")
    class SaveAuthorizationRequest {

        @Test
        @DisplayName("OAuth2 authorization request 저장 시 직렬화된 httpOnly 쿠키를 생성한다")
        void createsSerializedHttpOnlyCookie() {
            MockHttpServletResponse response = new MockHttpServletResponse();
            OAuth2AuthorizationRequest authorizationRequest = authorizationRequest();

            repository.saveAuthorizationRequest(
                    authorizationRequest,
                    new MockHttpServletRequest(),
                    response
            );

            Cookie cookie = response.getCookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            assertThat(cookie).isNotNull();
            assertThat(cookie.getValue()).isNotBlank();
            assertThat(cookie.getPath()).isEqualTo("/");
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.getMaxAge()).isEqualTo(180);
        }

        @Test
        @DisplayName("null request를 넘기면 기존 authorization request 쿠키를 삭제한다")
        void nullRequest_AddsDeleteCookieWhenCookieExists() {
            Cookie existingCookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, "serialized-request");
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(existingCookie);
            MockHttpServletResponse response = new MockHttpServletResponse();

            repository.saveAuthorizationRequest(null, request, response);

            Cookie deleteCookie = response.getCookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            assertThat(deleteCookie).isNotNull();
            assertThat(deleteCookie.getValue()).isEmpty();
            assertThat(deleteCookie.getPath()).isEqualTo("/");
            assertThat(deleteCookie.getMaxAge()).isZero();
        }
    }

    @Nested
    @DisplayName("loadAuthorizationRequest")
    class LoadAuthorizationRequest {

        @Test
        @DisplayName("OAuth2 authorization request 쿠키가 있으면 역직렬화해서 로드한다")
        void withCookie_ReturnsDeserializedAuthorizationRequest() {
            OAuth2AuthorizationRequest original = authorizationRequest();
            MockHttpServletResponse saveResponse = new MockHttpServletResponse();
            repository.saveAuthorizationRequest(original, new MockHttpServletRequest(), saveResponse);

            MockHttpServletRequest loadRequest = new MockHttpServletRequest();
            loadRequest.setCookies(saveResponse.getCookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME));

            OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(loadRequest);

            assertThat(loaded).isNotNull();
            assertThat(loaded.getAuthorizationUri()).isEqualTo(original.getAuthorizationUri());
            assertThat(loaded.getClientId()).isEqualTo(original.getClientId());
            assertThat(loaded.getRedirectUri()).isEqualTo(original.getRedirectUri());
            assertThat(loaded.getScopes()).isEqualTo(original.getScopes());
            assertThat(loaded.getState()).isEqualTo(original.getState());
            assertThat(loaded.getAdditionalParameters()).containsEntry("nonce", "nonce-123");
            assertThat(loaded.getAttributes()).containsEntry("registration_id", "apple");
        }

        @Test
        @DisplayName("OAuth2 authorization request 쿠키가 없으면 null을 반환한다")
        void withoutCookie_ReturnsNull() {
            OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(new MockHttpServletRequest());

            assertThat(loaded).isNull();
        }
    }

    @Nested
    @DisplayName("removeAuthorizationRequest")
    class RemoveAuthorizationRequest {

        @Test
        @DisplayName("기존 request를 반환하고 삭제 쿠키를 추가한다")
        void returnsExistingRequestAndAddsDeleteCookie() {
            OAuth2AuthorizationRequest original = authorizationRequest();
            MockHttpServletResponse saveResponse = new MockHttpServletResponse();
            repository.saveAuthorizationRequest(original, new MockHttpServletRequest(), saveResponse);

            MockHttpServletRequest removeRequest = new MockHttpServletRequest();
            removeRequest.setCookies(saveResponse.getCookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME));
            MockHttpServletResponse removeResponse = new MockHttpServletResponse();

            OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(removeRequest, removeResponse);

            assertThat(removed).isNotNull();
            assertThat(removed.getState()).isEqualTo(original.getState());
            Cookie deleteCookie = removeResponse.getCookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            assertThat(deleteCookie).isNotNull();
            assertThat(deleteCookie.getValue()).isEmpty();
            assertThat(deleteCookie.getPath()).isEqualTo("/");
            assertThat(deleteCookie.getMaxAge()).isZero();
        }
    }

    private OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://appleid.apple.com/auth/authorize")
                .clientId("techfork-client")
                .redirectUri("https://techfork.site/login/oauth2/code/apple")
                .scopes(Set.of("openid", "email"))
                .state("state-123")
                .additionalParameters(Map.of("nonce", "nonce-123"))
                .attributes(Map.of("registration_id", "apple"))
                .build();
    }
}
