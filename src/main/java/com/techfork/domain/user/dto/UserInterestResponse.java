package com.techfork.domain.user.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UserInterestResponse(
        List<UserInterestDto> interests
) {
}
