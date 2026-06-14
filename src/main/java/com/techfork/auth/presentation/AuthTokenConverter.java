package com.techfork.auth.presentation;

import com.techfork.auth.application.command.LogoutCommand;
import com.techfork.auth.application.command.RefreshTokenCommand;
import com.techfork.auth.application.result.TokenRefreshResult;
import com.techfork.auth.presentation.response.TokenRefreshResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenConverter {

    public RefreshTokenCommand toRefreshTokenCommand(String refreshToken) {
        return new RefreshTokenCommand(refreshToken);
    }

    public LogoutCommand toLogoutCommand(String refreshToken) {
        return new LogoutCommand(refreshToken);
    }

    public TokenRefreshResponse toTokenRefreshResponse(TokenRefreshResult result) {
        return TokenRefreshResponse.builder()
                .accessToken(result.accessToken())
                .build();
    }
}
