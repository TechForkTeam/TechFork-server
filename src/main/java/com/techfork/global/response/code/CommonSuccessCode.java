package com.techfork.global.response.code;

import com.techfork.global.response.dto.ReasonDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum CommonSuccessCode implements BaseCode{
    OK(HttpStatus.OK, "COMMON200", "요청에 성공했습니다."),
    CREATED(HttpStatus.CREATED, "COMMON201", "요청이 성공적으로 처리되어 리소스가 생성되었습니다."),
    ACCEPTED(HttpStatus.ACCEPTED, "COMMON202", "요청이 접수되었습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "COMMON204", "요청이 성공적으로 처리되었으나 반환할 데이터가 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason(){
        return ReasonDTO.builder()
                .code(code)
                .message(message)
                .httpStatus(httpStatus)
                .build();
    }
}
