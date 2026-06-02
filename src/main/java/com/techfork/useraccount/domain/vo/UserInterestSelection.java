package com.techfork.useraccount.domain.vo;

import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;

import java.util.List;

public record UserInterestSelection(
        EInterestCategory category,
        List<EInterestKeyword> keywords
) {
}
