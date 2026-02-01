package com.techfork.global.security.handler.login;

import com.techfork.domain.user.enums.UserStatus;
import com.techfork.global.security.auth.service.RefreshTokenService;
import com.techfork.global.security.jwt.JwtDTO;
import com.techfork.global.security.jwt.JwtProperties;
import com.techfork.global.security.jwt.JwtUtil;
import com.techfork.global.security.oauth.UserPrincipal;
import com.techfork.global.security.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    @Value("${server.domain}")
    private String domain;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        JwtDTO tokens = jwtUtil.generateTokens(userPrincipal.getId(), userPrincipal.getRole());
        long expiration = jwtProperties.getRefreshTokenExpiration();
        saveAndSetRefreshToken(response, userPrincipal.getId(), tokens.refreshToken(), expiration);

        log.info("OAuth2 login success - userId: {}, role: {}, status: {}, email: {}",
                userPrincipal.getId(), userPrincipal.getRole(), userPrincipal.getStatus(), userPrincipal.getEmail());

        // 온보딩 완료 여부에 따라 리다이렉트
        boolean isRegistered = userPrincipal.getStatus() == UserStatus.ACTIVE;
        String email = userPrincipal.getEmail() != null ?
                UriUtils.encode(userPrincipal.getEmail(), StandardCharsets.UTF_8) : "";

        String targetUrl = String.format(jwtProperties.getRedirectUri(),
                isRegistered, tokens.accessToken(), email);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private void saveAndSetRefreshToken(HttpServletResponse response, Long userId, String refreshToken, long expiration) {
        refreshTokenService.saveRefreshToken(userId, refreshToken, expiration);
        CookieUtil.addRefreshTokenCookie(response, domain, refreshToken, expiration);
    }
}

