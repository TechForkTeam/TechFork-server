package com.techfork.domain.auth.converter;

import com.techfork.domain.auth.dto.DeveloperTokenResponse;
import com.techfork.domain.auth.dto.KakaoLoginResponse;
import com.techfork.domain.useraccount.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AuthConverter {

    public DeveloperTokenResponse toDeveloperTokenResponse(String token){
        return DeveloperTokenResponse.builder()
                .developerToken(token)
                .build();
    }

    public KakaoLoginResponse toKakaoLoginResponse(String accessToken, User user) {
        return KakaoLoginResponse.builder()
                .accessToken(accessToken)
                .userId(user.getId())
                .isRegistered(user.isActive())
                .build();
    }
}
