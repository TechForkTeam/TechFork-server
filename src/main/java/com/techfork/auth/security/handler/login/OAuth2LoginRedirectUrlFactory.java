package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.useraccount.domain.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2LoginRedirectUrlFactory {

    private final JwtProperties jwtProperties;

    public String createSuccessRedirectUrl(UserPrincipal userPrincipal, String accessToken) {
        boolean isRegistered = userPrincipal.getStatus() == UserStatus.ACTIVE;
        String email = userPrincipal.getEmail() != null ?
                UriUtils.encode(userPrincipal.getEmail(), StandardCharsets.UTF_8) : "";

        return String.format(jwtProperties.getRedirectUri(), isRegistered, accessToken, email);
    }

    public String createFailureRedirectUrl(AuthenticationException exception) {
        return UriComponentsBuilder.fromUriString(jwtProperties.getLoginFailureRedirectUri())
                .queryParam("error", exception.getLocalizedMessage())
                .build()
                .toUriString();
    }
}
