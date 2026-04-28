package com.techfork.domain.useraccount.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "계정 프로필 수정 요청")
public record UpdateAccountProfileRequest(
        @Schema(description = "닉네임 (선택적)", example = "테크러버")
        String nickName,

        @Schema(description = "자기소개 (선택적)", example = "백엔드 개발자입니다.")
        String description
) {
}
