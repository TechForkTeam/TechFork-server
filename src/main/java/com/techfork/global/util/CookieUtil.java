package com.techfork.global.util;

import com.techfork.global.constant.Constants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public final class CookieUtil {
    private CookieUtil() {}

    public static void addRefreshTokenCookie(HttpServletResponse response, String domain, String token, long maxAge) {
        Cookie cookie = new Cookie(Constants.REFRESH_TOKEN_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setDomain(domain);
        cookie.setMaxAge((int) (maxAge / 1000)); // milliseconds to seconds

        response.addCookie(cookie);
    }
}
