package com.techfork.useraccount.application.query.result;

import lombok.Builder;

import java.util.List;

@Builder
public record GetUserInterestsResult(
        List<UserInterestResult> interests
) {
}
