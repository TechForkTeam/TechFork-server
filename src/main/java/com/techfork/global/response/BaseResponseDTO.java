package com.techfork.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.techfork.global.response.code.BaseCode;
import com.techfork.global.response.dto.ReasonDTO;
import lombok.Builder;
import org.springframework.http.ResponseEntity;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseResponseDTO<T>(
        boolean isSuccess,
        String code,
        String message,
        T data
) {
    public static <T> ResponseEntity<BaseResponseDTO<T>> success(BaseCode successCode) {
        ReasonDTO reason = successCode.getReason();
        BaseResponseDTO<T> body = BaseResponseDTO.<T>builder()
                .isSuccess(true)
                .code(reason.code())
                .message(reason.message())
                .build();

        return ResponseEntity
                .status(reason.httpStatus())
                .body(body);
    }

    public static <T> ResponseEntity<BaseResponseDTO<T>> success(BaseCode successCode, T data) {
        ReasonDTO reason = successCode.getReason();
        BaseResponseDTO<T> body = BaseResponseDTO.<T>builder()
                .isSuccess(true)
                .code(reason.code())
                .message(reason.message())
                .data(data)
                .build();

        return ResponseEntity
                .status(reason.httpStatus())
                .body(body);
    }

    public static <T> ResponseEntity<BaseResponseDTO<T>> error(BaseCode errorCode) {
        ReasonDTO errorReason = errorCode.getReason();
        BaseResponseDTO<T> body = BaseResponseDTO.<T>builder()
                .isSuccess(false)
                .code(errorReason.code())
                .message(errorReason.message())
                .build();

        return ResponseEntity
                .status(errorReason.httpStatus())
                .body(body);
    }
}
