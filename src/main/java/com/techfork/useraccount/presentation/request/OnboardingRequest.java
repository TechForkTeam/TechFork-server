package com.techfork.useraccount.presentation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OnboardingRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2-20자여야 합니다.")
        String nickname,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Size(max = 100, message = "한줄소개는 100자 이하여야 합니다.")
        String description,

        @NotNull(message = "관심사 목록은 필수입니다.")
        @NotEmpty(message = "관심사를 최소 1개 이상 선택해주세요.")
        List<UserInterestRequest> interests
) {
}
