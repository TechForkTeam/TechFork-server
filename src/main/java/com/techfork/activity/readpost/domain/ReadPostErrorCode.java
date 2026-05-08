package com.techfork.activity.readpost.domain;

import com.techfork.global.common.code.BaseCode;
import com.techfork.global.response.ReasonDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ReadPostErrorCode implements BaseCode {
    READ_POST_VIEW_COUNT_INCREMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "READ_POST500_1", "읽은 게시글 처리 중 조회수 증가에 실패했습니다.");

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
