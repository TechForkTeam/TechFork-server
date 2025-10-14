package com.techfork.global.response.dto;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public record ReasonDTO(
        String code,
        String message,
        HttpStatus httpStatus
) {
}
