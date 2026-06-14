package com.techfork.auth.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "개발자 토큰 발급 응답")
public record DeveloperTokenResponse(
        @Schema(description = "개발자 토큰 (만료 기간: 30일)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String developerToken
) {
}
