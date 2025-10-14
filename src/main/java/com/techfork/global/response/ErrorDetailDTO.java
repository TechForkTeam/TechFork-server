package com.techfork.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetailDTO(
        List<FieldErrorDTO> fieldErrors,
        String detail
) {
    public static ErrorDetailDTO of(List<FieldErrorDTO> fieldErrors) {
        return ErrorDetailDTO.builder()
                .fieldErrors(fieldErrors)
                .build();
    }

    public static ErrorDetailDTO of(String detail) {
        return ErrorDetailDTO.builder()
                .detail(detail)
                .build();
    }

    @Builder
    public record FieldErrorDTO(
            String field,
            Object rejectedValue,
            String message
    ){
        public static FieldErrorDTO of(String field, Object rejectedValue, String message) {
            return FieldErrorDTO.builder()
                    .field(field)
                    .rejectedValue(rejectedValue)
                    .message(message)
                    .build();
        }
    }
}
