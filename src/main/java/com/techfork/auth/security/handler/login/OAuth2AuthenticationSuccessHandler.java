package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.oauth.UserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2LoginTokenIssuer tokenIssuer;
    private final OAuth2LoginRefreshTokenWriter refreshTokenWriter;
    private final OAuth2LoginRedirectUrlFactory redirectUrlFactory;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        OAuth2LoginTokens tokens = tokenIssuer.issue(userPrincipal);
        refreshTokenWriter.write(userPrincipal.getId(), tokens, response);

        log.info("OAuth2 login success - userId: {}, role: {}, status: {}, email: {}",
                userPrincipal.getId(), userPrincipal.getRole(), userPrincipal.getStatus(), userPrincipal.getEmail());

        String targetUrl = redirectUrlFactory.createSuccessRedirectUrl(userPrincipal, tokens.accessToken());

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
