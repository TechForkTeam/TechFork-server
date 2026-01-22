package com.techfork.domain.auth.exception;

import com.techfork.global.common.code.BaseCode;
import com.techfork.global.response.ReasonDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements BaseCode {
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_INVALID", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_EXPIRED", "만료된 토큰입니다."),
    MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_MALFORMED", "토큰 형식이 올바르지 않습니다."),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "AUTH401_SIGNATURE", "토큰 서명이 유효하지 않습니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_UNSUPPORTED", "지원하지 않는 토큰 형식입니다."),
    EMPTY_CLAIMS(HttpStatus.UNAUTHORIZED, "AUTH401_EMPTY_CLAIMS", "토큰 정보가 비어있습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_INVALID_REFRESH", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "AUTH401_REFRESH_MISSING", "리프레시 토큰이 제공되지 않았습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH401_REFRESH_MISMATCH", "리프레시 토큰이 일치하지 않습니다. 세션이 무효화되었습니다."),
    TOKEN_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH400_TYPE_MISMATCH", "토큰 타입이 일치하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH404_USER", "사용자를 찾을 수 없습니다."),
    FORBIDDEN_INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "AUTH403_FORBIDDEN", "권한이 부족합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}
