package com.techfork.auth.security.filter;

import com.techfork.auth.domain.exception.AuthErrorCode;
import com.techfork.auth.security.AuthSecurityConstants;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.auth.security.cache.UserAuthCacheStore;
import com.techfork.auth.security.util.HeaderUtil;
import com.techfork.global.constant.MdcKey;
import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.application.auth.UserAuthAccountService;
import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.enums.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import static com.techfork.auth.security.jwt.JwtConstants.TOKEN_TYPE_ACCESS;

/**
 * JWT 인증 필터
 * - Authorization 헤더에서 Bearer 토큰을 추출하여 인증 처리
 * - 유효한 액세스 토큰인 경우 SecurityContext에 인증 정보 설정
 * - 요청당 한 번만 실행되는 OncePerRequestFilter 상속
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserAuthAccountService userAuthAccountService;
    private final UserAuthCacheStore userAuthCacheStore;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = HeaderUtil.refineHeader(request, AuthSecurityConstants.AUTHORIZATION_HEADER, AuthSecurityConstants.BEARER_PREFIX)
                    .orElse(null);

            if (jwt != null) {
                jwtUtil.validateToken(jwt);
                jwtUtil.validateTokenType(jwt, TOKEN_TYPE_ACCESS);

                Long userId = jwtUtil.getUserIdFromToken(jwt);
                UserPrincipal userPrincipal = userAuthCacheStore.get(userId);

                if (userPrincipal == null) {
                    UserAuthProfile userAuthProfile = userAuthAccountService.findAuthProfileById(userId)
                            .orElseThrow(() -> new GeneralException(AuthErrorCode.USER_NOT_FOUND));

                    userPrincipal = UserPrincipal.from(userAuthProfile);

                    if (userPrincipal.getStatus() == UserStatus.WITHDRAWN) {
                        throw new GeneralException(AuthErrorCode.WITHDRAWN_USER);
                    }

                    userAuthCacheStore.put(userId, userAuthProfile, jwtProperties.getAccessTokenExpiration());
                } else if (userPrincipal.getStatus() == UserStatus.WITHDRAWN) {
                    throw new GeneralException(AuthErrorCode.WITHDRAWN_USER);
                }

                UsernamePasswordAuthenticationToken authentication = createAuthentication(userPrincipal, request);

                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);

                MDC.put(MdcKey.USER_ID, String.valueOf(userId));
                log.debug("Set authentication for user: {}", userId);
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            request.setAttribute(AuthSecurityConstants.JWT_EXCEPTION_ATTRIBUTE, e);
        }

        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken createAuthentication(UserPrincipal userPrincipal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }
}
