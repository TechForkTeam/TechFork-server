package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.useraccount.domain.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
class OAuth2LoginRedirectUrlFactory {

    private static final String PUBLIC_FAILURE_ERROR_CODE = "oauth_failed";

    private final JwtProperties jwtProperties;

    public String createSuccessRedirectUrl(UserPrincipal userPrincipal) {
        boolean isRegistered = userPrincipal.getStatus() == UserStatus.ACTIVE;
        String email = userPrincipal.getEmail() != null ? userPrincipal.getEmail() : "";

        return UriComponentsBuilder.fromUriString(jwtProperties.getLoginSuccessRedirectUri())
                .replaceQuery(null)
                .queryParam("registered", isRegistered)
                .queryParam("email", email)
                .build()
                .encode()
                .toUriString();
    }

    public String createFailureRedirectUrl() {
        return UriComponentsBuilder.fromUriString(jwtProperties.getLoginFailureRedirectUri())
                .queryParam("errorCode", PUBLIC_FAILURE_ERROR_CODE)
                .build()
                .toUriString();
    }
}
