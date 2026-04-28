package com.techfork.domain.useraccount.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocialType {
    KAKAO("kakao"),
    APPLE("apple");

    private final String registrationId;

    public static SocialType fromRegistrationId(String registrationId) {
        for (SocialType type : values()) {
            if (type.registrationId.equals(registrationId)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown registrationId: " + registrationId);
    }
}