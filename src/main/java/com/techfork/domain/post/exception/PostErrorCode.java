package com.techfork.domain.post.exception;

import com.techfork.global.common.code.BaseCode;
import com.techfork.global.response.ReasonDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PostErrorCode implements BaseCode {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST404_1", "게시글을 찾을 수 없습니다.");

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
