package com.techfork.auth.security.handler.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.auth.domain.exception.AuthErrorCode;
import com.techfork.global.common.code.BaseCode;
import com.techfork.auth.security.AuthSecurityConstants;
import com.techfork.global.exception.CommonErrorCode;
import com.techfork.global.exception.GeneralException;
import com.techfork.global.response.BaseResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        Exception jwtException = (Exception) request.getAttribute(AuthSecurityConstants.JWT_EXCEPTION_ATTRIBUTE);
        BaseCode errorCode = determineErrorCode(jwtException);

        if (jwtException != null) {
            log.warn("JWT authentication failed - Exception: {}, ErrorCode: {}",
                    jwtException.getClass().getSimpleName(),
                    errorCode.getReason().code());
        } else {
            log.warn("Authentication required - ErrorCode: {}", errorCode.getReason().code());
        }

        ResponseEntity<BaseResponse<Void>> responseEntity = BaseResponse.of(errorCode);

        response.setStatus(responseEntity.getStatusCode().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(responseEntity.getBody()));
    }

    private BaseCode determineErrorCode(Exception exception) {
        if (exception == null) {
            return CommonErrorCode.UNAUTHORIZED;
        } else if (exception instanceof GeneralException generalException) {
            return generalException.getCode();
        } else if (exception instanceof ExpiredJwtException) {
            return AuthErrorCode.EXPIRED_TOKEN;
        } else if (exception instanceof MalformedJwtException) {
            return AuthErrorCode.MALFORMED_TOKEN;
        } else if (exception instanceof SignatureException) {
            return AuthErrorCode.INVALID_SIGNATURE;
        } else if (exception instanceof UnsupportedJwtException) {
            return AuthErrorCode.UNSUPPORTED_TOKEN;
        } else if (exception instanceof IllegalArgumentException) {
            return AuthErrorCode.EMPTY_CLAIMS;
        }
        return CommonErrorCode.UNAUTHORIZED;
    }
}
