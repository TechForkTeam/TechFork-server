package com.techfork.auth.presentation.controller;

import com.techfork.auth.application.command.KakaoLoginCommandService;
import com.techfork.auth.application.command.input.KakaoLoginCommand;
import com.techfork.auth.application.command.result.KakaoLoginResult;
import com.techfork.auth.presentation.annotation.AuthApi;
import com.techfork.auth.presentation.converter.KakaoLoginConverter;
import com.techfork.auth.presentation.request.KakaoLoginRequest;
import com.techfork.auth.presentation.response.KakaoLoginResponse;
import com.techfork.auth.security.cookie.RefreshTokenCookieWriter;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AuthApi
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/kakao")
@RequiredArgsConstructor
public class KakaoLoginController {

    private final KakaoLoginCommandService kakaoLoginCommandService;
    private final KakaoLoginConverter kakaoLoginConverter;
    private final RefreshTokenCookieWriter refreshTokenCookieWriter;

    @Operation(
            summary = "카카오 로그인",
            description = "iOS 클라이언트에서 받은 카카오 액세스 토큰을 검증하고 자체 JWT 토큰을 발급합니다. 리프레시 토큰은 HttpOnly 쿠키로 설정됩니다."
    )
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<KakaoLoginResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request,
            HttpServletResponse response
    ) {
        KakaoLoginCommand command = kakaoLoginConverter.toKakaoLoginCommand(request);
        KakaoLoginResult result = kakaoLoginCommandService.login(command);
        refreshTokenCookieWriter.write(response, result.refreshToken(), result.refreshTokenExpiration());

        KakaoLoginResponse loginResponse = kakaoLoginConverter.toKakaoLoginResponse(result);
        return BaseResponse.of(SuccessCode.OK, loginResponse);
    }
}
