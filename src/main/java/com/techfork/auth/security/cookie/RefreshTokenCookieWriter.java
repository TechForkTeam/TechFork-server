package com.techfork.auth.security.cookie;

import com.techfork.auth.security.AuthSecurityConstants;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieWriter {

    public void write(HttpServletResponse response, String domain, String token, long maxAgeMillis) {
        ResponseCookie cookie = ResponseCookie.from(AuthSecurityConstants.REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .domain(domain)
                .maxAge(maxAgeMillis / 1000)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void delete(HttpServletResponse response, String domain) {
        ResponseCookie cookie = ResponseCookie.from(AuthSecurityConstants.REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .domain(domain)
                .maxAge(0)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }
}
