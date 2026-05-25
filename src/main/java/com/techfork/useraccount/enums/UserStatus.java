package com.techfork.useraccount.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    PENDING("대기", "온보딩 미완료"),
    ACTIVE("활성", "온보딩 완료"),
    WITHDRAWN("탈퇴", "회원 탈퇴 완료");

    private final String description;
    private final String detail;
}