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
import com.techfork.auth.security.token.RefreshTokenStore;
import com.techfork.auth.security.cache.UserAuthCacheStore;
import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.application.auth.UserAuthAccountService;
import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.enums.Role;
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
    private final RefreshTokenStore refreshTokenStore;
    private final UserAuthAccountService userAuthAccountService;
    private final JwtProperties jwtProperties;
    private final UserAuthCacheStore userAuthCacheStore;

    public TokenRefreshResult refreshToken(RefreshTokenCommand command) {
        String refreshToken = command.refreshToken();
        validateRefreshTokenRequest(refreshToken);

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        validateRefreshTokenInRedis(userId, refreshToken);

        UserAuthProfile userAuthProfile = userAuthAccountService.findAuthProfileById(userId)
                .orElseThrow(() -> new GeneralException(AuthErrorCode.USER_NOT_FOUND));

        JwtDTO newTokens = jwtUtil.generateTokens(userId, userAuthProfile.role());
        long expiration = jwtProperties.getRefreshTokenExpiration();
        saveRefreshToken(userId, newTokens.refreshToken(), expiration);

        userAuthCacheStore.put(userId, userAuthProfile, jwtProperties.getAccessTokenExpiration());

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
        UserAuthProfile userAuthProfile = userAuthAccountService.findAuthProfileById(userId)
                .orElseThrow(() -> new GeneralException(AuthErrorCode.USER_NOT_FOUND));

        if (userAuthProfile.role() != Role.ADMIN) {
            throw new GeneralException(AuthErrorCode.FORBIDDEN_INSUFFICIENT_PERMISSIONS);
        }

        String longLivedAccessToken = jwtUtil.generateLongLivedAccessToken(userId, userAuthProfile.role());

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
        if (!refreshTokenStore.validateRefreshToken(userId, refreshToken)) {
            refreshTokenStore.deleteRefreshToken(userId);
            log.warn("Refresh token mismatch detected for userId: {}. Session invalidated.", userId);
            throw new GeneralException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }
    }

    private void saveRefreshToken(Long userId, String refreshToken, long expiration) {
        refreshTokenStore.saveRefreshToken(userId, refreshToken, expiration);
    }

    private void deleteRefreshToken(Long userId) {
        refreshTokenStore.deleteRefreshToken(userId);
    }
}
