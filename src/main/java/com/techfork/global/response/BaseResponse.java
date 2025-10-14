package com.techfork.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.techfork.global.common.code.BaseCode;
import lombok.Builder;
import org.springframework.http.ResponseEntity;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseResponse<T>(
        boolean isSuccess,
        String code,
        String message,
        T data
) {
    public static <T> ResponseEntity<BaseResponse<T>> of(BaseCode code) {
        return of(code, null);
    }

    public static <T> ResponseEntity<BaseResponse<T>> of(BaseCode code, T data) {
        ReasonDTO reason = code.getReason();
        boolean isSuccess = reason.httpStatus().is2xxSuccessful();

        BaseResponse<T> body = BaseResponse.<T>builder()
                .isSuccess(isSuccess)
                .code(reason.code())
                .message(reason.message())
                .data(data)
                .build();

        return ResponseEntity
                .status(reason.httpStatus())
                .body(body);
    }

    public static ResponseEntity<Object> ofObject(BaseCode code, Object data) {
        ReasonDTO reason = code.getReason();
        boolean isSuccess = reason.httpStatus().is2xxSuccessful();

        BaseResponse<Object> body = BaseResponse.builder()
                .isSuccess(isSuccess)
                .code(reason.code())
                .message(reason.message())
                .data(data)
                .build();

        return ResponseEntity
                .status(reason.httpStatus())
                .body(body);
    }
}
