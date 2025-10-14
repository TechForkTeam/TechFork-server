package com.techfork.global.response;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public record ReasonDTO(
        String code,
        String message,
        HttpStatus httpStatus
) {
}
