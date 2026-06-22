package com.techfork.auth.security.oauth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
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
        @DisplayName("OAuth2 authorization request 저장 시 보안 속성이 포함된 쿠키를 생성한다")
        void validRequest_CreatesSerializedSecureCookie() {
            MockHttpServletResponse response = new MockHttpServletResponse();
            OAuth2AuthorizationRequest authorizationRequest = authorizationRequest();

            repository.saveAuthorizationRequest(
                    authorizationRequest,
                    new MockHttpServletRequest(),
                    response
            );

            String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
            assertAuthorizationRequestCookie(setCookie, 180);
            assertThat(extractCookieValue(setCookie)).isNotBlank();
        }

        @Test
        @DisplayName("null request를 넘기면 기존 authorization request 쿠키를 삭제한다")
        void nullRequest_AddsDeleteCookieWhenCookieExists() {
            Cookie existingCookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, "serialized-request");
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(existingCookie);
            MockHttpServletResponse response = new MockHttpServletResponse();

            repository.saveAuthorizationRequest(null, request, response);

            String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
            assertDeleteCookie(setCookie);
        }

        @Test
        @DisplayName("null request를 넘겨도 기존 쿠키가 없으면 삭제 쿠키를 추가하지 않는다")
        void nullRequest_DoesNotAddDeleteCookieWhenCookieDoesNotExist() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            repository.saveAuthorizationRequest(null, request, response);

            assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
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
            loadRequest.setCookies(cookieFromSetCookieHeader(saveResponse));

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
        void withCookie_ReturnsExistingRequestAndAddsDeleteCookie() {
            OAuth2AuthorizationRequest original = authorizationRequest();
            MockHttpServletResponse saveResponse = new MockHttpServletResponse();
            repository.saveAuthorizationRequest(original, new MockHttpServletRequest(), saveResponse);

            MockHttpServletRequest removeRequest = new MockHttpServletRequest();
            removeRequest.setCookies(cookieFromSetCookieHeader(saveResponse));
            MockHttpServletResponse removeResponse = new MockHttpServletResponse();

            OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(removeRequest, removeResponse);

            assertThat(removed).isNotNull();
            assertThat(removed.getState()).isEqualTo(original.getState());
            String setCookie = removeResponse.getHeader(HttpHeaders.SET_COOKIE);
            assertDeleteCookie(setCookie);
        }

        @Test
        @DisplayName("기존 쿠키가 없으면 null을 반환하고 삭제 쿠키를 추가하지 않는다")
        void withoutCookie_ReturnsNullAndDoesNotAddDeleteCookie() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request, response);

            assertThat(removed).isNull();
            assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
        }
    }

    private void assertDeleteCookie(String setCookie) {
        assertAuthorizationRequestCookie(setCookie, 0);
        assertThat(extractCookieValue(setCookie)).isEmpty();
    }

    private void assertAuthorizationRequestCookie(String setCookie, int maxAge) {
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).startsWith(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME + "=");
        assertThat(setCookie).contains("Path=/");
        assertThat(setCookie).contains("Max-Age=" + maxAge);
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=None");
        assertThat(setCookie).doesNotContain("Domain=");
    }

    private Cookie cookieFromSetCookieHeader(MockHttpServletResponse response) {
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        return new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, extractCookieValue(setCookie));
    }

    private String extractCookieValue(String setCookie) {
        assertThat(setCookie).isNotNull();
        String nameValue = setCookie.split(";", 2)[0];
        assertThat(nameValue).startsWith(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME + "=");
        return nameValue.substring((OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME + "=").length());
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
