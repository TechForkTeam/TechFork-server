package com.techfork.domain.useraccount.exception;

import com.techfork.global.common.code.BaseCode;
import com.techfork.global.response.ReasonDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserErrorCode implements BaseCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_1", "사용자를 찾을 수 없습니다."),
    INVALID_INTEREST_KEYWORD(HttpStatus.BAD_REQUEST, "USER400_1", "유효하지 않은 관심사 키워드입니다."),
    ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "USER400_2", "이미 탈퇴한 회원입니다.");

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
