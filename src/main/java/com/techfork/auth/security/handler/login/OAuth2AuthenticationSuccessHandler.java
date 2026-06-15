package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.service.RefreshTokenService;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.auth.security.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2LoginRedirectUrlFactory redirectUrlFactory;

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

        String targetUrl = redirectUrlFactory.createSuccessRedirectUrl(userPrincipal, tokens.accessToken());

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private void saveAndSetRefreshToken(HttpServletResponse response, Long userId, String refreshToken, long expiration) {
        refreshTokenService.saveRefreshToken(userId, refreshToken, expiration);
        CookieUtil.addRefreshTokenCookie(response, domain, refreshToken, expiration);
    }
}
