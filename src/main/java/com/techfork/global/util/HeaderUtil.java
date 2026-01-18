package com.techfork.global.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * HTTP 헤더 처리 유틸리티
 */
public final class HeaderUtil {

    private HeaderUtil() {}

    /**
     * HTTP 요청 헤더에서 특정 헤더값을 추출하고 prefix를 제거
     *
     * @param request HTTP 요청
     * @param headerName 헤더 이름 (예: "Authorization")
     * @param prefix 제거할 접두사 (예: "Bearer ")
     * @return prefix가 제거된 헤더값, 헤더가 없거나 prefix가 없으면 Optional.empty()
     */
    public static Optional<String> refineHeader(HttpServletRequest request, String headerName, String prefix) {
        String headerValue = request.getHeader(headerName);

        if (!StringUtils.hasText(headerValue)) {
            return Optional.empty();
        }

        if (!headerValue.startsWith(prefix)) {
            return Optional.empty();
        }

        return Optional.of(headerValue.substring(prefix.length()));
    }
}