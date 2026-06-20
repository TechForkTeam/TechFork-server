package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.oauth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OAuth2LoginRefreshTokenIssuer {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public OAuth2LoginRefreshToken issue(UserPrincipal userPrincipal) {
        String refreshToken = jwtUtil.generateRefreshToken(userPrincipal.getId(), userPrincipal.getRole());
        return new OAuth2LoginRefreshToken(
                refreshToken,
                jwtProperties.getRefreshTokenExpiration()
        );
    }
}
