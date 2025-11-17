package com.techfork.domain.activity.exception;

import com.techfork.global.common.code.BaseCode;
import com.techfork.global.response.ReasonDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ActivityErrorCode implements BaseCode {
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "ACTIVITY404_1", "북마크를 찾을 수 없습니다."),
    BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "ACTIVITY409_1", "이미 북마크한 게시글입니다.");

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
