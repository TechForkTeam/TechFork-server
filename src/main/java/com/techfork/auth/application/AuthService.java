package com.techfork.auth.application;

import com.techfork.auth.application.dto.DeveloperTokenResponse;
import com.techfork.auth.application.dto.TokenRefreshResponse;
import com.techfork.auth.domain.exception.AuthErrorCode;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.exception.GeneralException;
import com.techfork.auth.security.service.RefreshTokenService;
import com.techfork.auth.security.service.UserAuthCacheService;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.techfork.auth.security.jwt.JwtConstants.TOKEN_TYPE_REFRESH;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;
    private final AuthConverter authConverter;
    private final UserAuthCacheService userAuthCacheService;

    @Value("${server.domain}")
    private String domain;

    public TokenRefreshResponse refreshToken(String refreshToken, HttpServletResponse response) {
        validateRefreshTokenRequest(refreshToken);

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        validateRefreshTokenInRedis(userId, refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(AuthErrorCode.USER_NOT_FOUND));

        JwtDTO newTokens = jwtUtil.generateTokens(userId, user.getRole());
        long expiration = jwtProperties.getRefreshTokenExpiration();
        saveAndSetRefreshToken(response, userId, newTokens.refreshToken(), expiration);

        userAuthCacheService.put(userId, user, jwtProperties.getAccessTokenExpiration());

        log.info("Token refreshed");

        return TokenRefreshResponse.builder()
                .accessToken(newTokens.accessToken())
                .build();
    }

    public void logout(String refreshToken, HttpServletResponse response) {
        validateRefreshTokenRequest(refreshToken);

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        deleteRefreshToken(response, userId);

        log.info("User logged out - userId: {}", userId);
    }

    public DeveloperTokenResponse generateDeveloperToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(AuthErrorCode.USER_NOT_FOUND));

        if (user.getRole() != Role.ADMIN) {
            throw new GeneralException(AuthErrorCode.FORBIDDEN_INSUFFICIENT_PERMISSIONS);
        }

        String longLivedAccessToken = jwtUtil.generateLongLivedAccessToken(userId, user.getRole());

        log.info("Developer token (long-lived access token) generated for admin userId: {}", userId);

        return authConverter.toDeveloperTokenResponse(longLivedAccessToken);
    }

    private void validateRefreshTokenRequest(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new GeneralException(AuthErrorCode.REFRESH_TOKEN_MISSING);
        }
        if (!jwtUtil.isValidToken(refreshToken)) {
            throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        jwtUtil.validateTokenType(refreshToken, TOKEN_TYPE_REFRESH);
    }

    private void validateRefreshTokenInRedis(Long userId, String refreshToken) {
        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            refreshTokenService.deleteRefreshToken(userId);
            log.warn("Refresh token mismatch detected for userId: {}. Session invalidated.", userId);
            throw new GeneralException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }
    }

    private void saveAndSetRefreshToken(HttpServletResponse response, Long userId, String refreshToken, long expiration) {
        refreshTokenService.saveRefreshToken(userId, refreshToken, expiration);
        CookieUtil.addRefreshTokenCookie(response, domain, refreshToken, expiration);
    }

    private void deleteRefreshToken(HttpServletResponse response, Long userId) {
        refreshTokenService.deleteRefreshToken(userId);
        CookieUtil.deleteRefreshTokenCookie(response, domain);
    }
}
