package com.techfork.auth.infrastructure.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfoResponse(
        @JsonProperty("id")
        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
            @JsonProperty("email")
            String email,

            @JsonProperty("profile")
            Profile profile
    ) {}

    public record Profile(
            @JsonProperty("profile_image_url")
            String profileImageUrl
    ) {}
}