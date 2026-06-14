package com.techfork.auth.application.command;

import com.techfork.auth.application.command.input.GenerateDeveloperTokenCommand;
import com.techfork.auth.application.command.input.LogoutCommand;
import com.techfork.auth.application.command.input.RefreshTokenCommand;
import com.techfork.auth.application.command.result.DeveloperTokenResult;
import com.techfork.auth.application.command.result.TokenRefreshResult;
import com.techfork.auth.domain.exception.AuthErrorCode;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.service.RefreshTokenService;
import com.techfork.auth.security.service.UserAuthCacheService;
import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.techfork.auth.security.jwt.JwtConstants.TOKEN_TYPE_REFRESH;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;
    private final UserAuthCacheService userAuthCacheService;

    public TokenRefreshResult refreshToken(RefreshTokenCommand command) {
        String refreshToken = command.refreshToken();
        validateRefreshTokenRequest(refreshToken);

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        validateRefreshTokenInRedis(userId, refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(AuthErrorCode.USER_NOT_FOUND));

        JwtDTO newTokens = jwtUtil.generateTokens(userId, user.getRole());
        long expiration = jwtProperties.getRefreshTokenExpiration();
        saveRefreshToken(userId, newTokens.refreshToken(), expiration);

        userAuthCacheService.put(userId, user, jwtProperties.getAccessTokenExpiration());

        log.info("Token refreshed");

        return TokenRefreshResult.builder()
                .accessToken(newTokens.accessToken())
                .refreshToken(newTokens.refreshToken())
                .refreshTokenExpiration(expiration)
                .build();
    }

    public void logout(LogoutCommand command) {
        String refreshToken = command.refreshToken();
        validateRefreshTokenRequest(refreshToken);

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        deleteRefreshToken(userId);

        log.info("User logged out - userId: {}", userId);
    }

    public DeveloperTokenResult generateDeveloperToken(GenerateDeveloperTokenCommand command) {
        Long userId = command.userId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(AuthErrorCode.USER_NOT_FOUND));

        if (user.getRole() != Role.ADMIN) {
            throw new GeneralException(AuthErrorCode.FORBIDDEN_INSUFFICIENT_PERMISSIONS);
        }

        String longLivedAccessToken = jwtUtil.generateLongLivedAccessToken(userId, user.getRole());

        log.info("Developer token (long-lived access token) generated for admin userId: {}", userId);

        return DeveloperTokenResult.builder()
                .developerToken(longLivedAccessToken)
                .build();
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

    private void saveRefreshToken(Long userId, String refreshToken, long expiration) {
        refreshTokenService.saveRefreshToken(userId, refreshToken, expiration);
    }

    private void deleteRefreshToken(Long userId) {
        refreshTokenService.deleteRefreshToken(userId);
    }
}
