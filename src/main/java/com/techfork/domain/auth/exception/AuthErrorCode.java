package com.techfork.domain.auth.exception;

import com.techfork.global.common.code.BaseCode;
import com.techfork.global.response.ReasonDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements BaseCode {
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_INVALID", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_EXPIRED", "만료된 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_INVALID_REFRESH", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "AUTH401_REFRESH_MISSING", "리프레시 토큰이 제공되지 않았습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH401_REFRESH_NOT_FOUND", "저장된 리프레시 토큰을 찾을 수 없습니다."),
    TOKEN_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH400_TYPE_MISMATCH", "토큰 타입이 일치하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH404_USER", "사용자를 찾을 수 없습니다.");

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
