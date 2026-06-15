package com.techfork.auth.security.handler.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그인 실패 핸들러
 * - OAuth2 인증 실패 시 프론트엔드로 에러 정보와 함께 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final OAuth2LoginRedirectUrlFactory redirectUrlFactory;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        log.error("OAuth2 login failed - Error: {}", exception.getMessage(), exception);

        String targetUrl = redirectUrlFactory.createFailureRedirectUrl(exception);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
