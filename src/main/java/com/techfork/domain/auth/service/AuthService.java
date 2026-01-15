package com.techfork.domain.auth.service;

import com.techfork.domain.auth.dto.TokenRefreshResponse;
import com.techfork.domain.auth.exception.AuthErrorCode;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.Role;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import com.techfork.global.security.auth.service.RefreshTokenService;
import com.techfork.global.security.jwt.JwtDTO;
import com.techfork.global.security.jwt.JwtProperties;
import com.techfork.global.security.jwt.JwtUtil;
import com.techfork.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.techfork.global.security.jwt.JwtConstants.TOKEN_TYPE_REFRESH;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    @Value("${server.domain}")
    private String domain;

    @Transactional
    public TokenRefreshResponse refreshToken(String refreshToken, HttpServletResponse response) {
        validateRefreshTokenRequest(refreshToken);

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        validateRefreshTokenInRedis(userId, refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(AuthErrorCode.USER_NOT_FOUND));
        JwtDTO newTokens = jwtUtil.generateTokens(userId, user.getRole());

        saveAndSetRefreshToken(response, userId, newTokens.refreshToken());

        log.info("Token refreshed for userId: {}", userId);

        return TokenRefreshResponse.builder()
                .accessToken(newTokens.accessToken())
                .build();
    }

    private void validateRefreshTokenRequest(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new GeneralException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        jwtUtil.validateTokenType(refreshToken, TOKEN_TYPE_REFRESH);
    }

    private void validateRefreshTokenInRedis(Long userId, String refreshToken) {
        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            throw new GeneralException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
    }

    private void saveAndSetRefreshToken(HttpServletResponse response, Long userId, String refreshToken) {
        refreshTokenService.saveRefreshToken(
                userId,
                refreshToken,
                jwtProperties.getRefreshTokenExpiration()
        );

        CookieUtil.addRefreshTokenCookie(
                response,
                domain,
                refreshToken,
                jwtProperties.getRefreshTokenExpiration()
        );
    }
}
