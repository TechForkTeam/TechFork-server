package com.techfork.domain.useraccount.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaveInterestRequest(
        @NotNull(message = "관심사 목록은 필수입니다.")
        @NotEmpty(message = "관심사를 최소 1개 이상 선택해주세요.")
        List<UserInterestDto> interests
) {
}
