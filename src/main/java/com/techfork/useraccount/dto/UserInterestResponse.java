package com.techfork.useraccount.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UserInterestResponse(
        List<UserInterestDto> interests
) {
}
