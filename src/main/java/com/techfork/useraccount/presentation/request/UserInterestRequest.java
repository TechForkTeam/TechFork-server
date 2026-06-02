package com.techfork.useraccount.presentation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.List;

@Builder
public record UserInterestRequest(
        @NotBlank(message = "관심사 카테고리는 필수입니다.")
        String category,
        List<@NotBlank(message = "관심사 키워드는 필수입니다.") String> keywords
) {
}
