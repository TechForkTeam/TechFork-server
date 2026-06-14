package com.techfork.auth.presentation;

import com.techfork.auth.application.command.input.KakaoLoginCommand;
import com.techfork.auth.application.command.result.KakaoLoginResult;
import com.techfork.auth.presentation.request.KakaoLoginRequest;
import com.techfork.auth.presentation.response.KakaoLoginResponse;
import org.springframework.stereotype.Component;

@Component
public class KakaoLoginConverter {

    public KakaoLoginCommand toKakaoLoginCommand(KakaoLoginRequest request) {
        return new KakaoLoginCommand(request.accessToken());
    }

    public KakaoLoginResponse toKakaoLoginResponse(KakaoLoginResult result) {
        return KakaoLoginResponse.builder()
                .accessToken(result.accessToken())
                .userId(result.userId())
                .isRegistered(result.isRegistered())
                .build();
    }
}
