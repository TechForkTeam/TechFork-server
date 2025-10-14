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
    public static <T> ResponseEntity<BaseResponseDTO<T>> of(BaseCode code) {
        return of(code, null);
    }

    public static <T> ResponseEntity<BaseResponseDTO<T>> of(BaseCode code, T data) {
        ReasonDTO reason = code.getReason();
        boolean isSuccess = reason.httpStatus().is2xxSuccessful();

        BaseResponseDTO<T> body = BaseResponseDTO.<T>builder()
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

        BaseResponseDTO<Object> body = BaseResponseDTO.builder()
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
