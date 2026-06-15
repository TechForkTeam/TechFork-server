package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.oauth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OAuth2LoginTokenIssuer {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public OAuth2LoginTokens issue(UserPrincipal userPrincipal) {
        JwtDTO tokens = jwtUtil.generateTokens(userPrincipal.getId(), userPrincipal.getRole());
        return new OAuth2LoginTokens(
                tokens.accessToken(),
                tokens.refreshToken(),
                jwtProperties.getRefreshTokenExpiration()
        );
    }
}
