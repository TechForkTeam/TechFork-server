package com.techfork.global.security.handler.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.global.exception.CommonErrorCode;
import com.techfork.global.response.BaseResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 권한 부족 예외 처리 핸들러
 * - 인증은 되었지만 접근 권한이 없는 경우 403 Forbidden 응답 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("Access denied - URI: {}, Message: {}", request.getRequestURI(), accessDeniedException.getMessage());

        ResponseEntity<BaseResponse<Void>> responseEntity = BaseResponse.of(CommonErrorCode.FORBIDDEN);

        response.setStatus(responseEntity.getStatusCode().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(responseEntity.getBody()));
    }
}