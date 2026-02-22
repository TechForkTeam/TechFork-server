package com.techfork.global.filter;

import com.techfork.global.constant.MdcKey;
import jakarta.servlet.FilterChain;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP 요청마다 MDC에 트레이싱 컨텍스트를 설정하는 필터.
 * 요청 종료 시 MDC를 정리하여 스레드 풀 재사용 시 누수를 방지한다.
 * <p>
 * MDC 필드:
 * - requestId: 요청 단위 고유 ID (UUID)
 * - userId: 인증된 사용자 ID (비인증 요청은 "anonymous")
 * - method: HTTP 메서드
 * - uri: 요청 URI
 * - clientIp: 클라이언트 IP (X-Forwarded-For 우선)
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            MDC.put(MdcKey.REQUEST_ID, UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            MDC.put(MdcKey.METHOD, request.getMethod());
            MDC.put(MdcKey.URI, request.getRequestURI());
            MDC.put(MdcKey.CLIENT_IP, resolveClientIp(request));
            MDC.put(MdcKey.USER_ID, "anonymous");

            filterChain.doFilter(request, response);

            log.info("{} {} {} {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    System.currentTimeMillis() - startTime);
        } finally {
            MDC.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isValid(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (isValid(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private boolean isValid(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }
}
