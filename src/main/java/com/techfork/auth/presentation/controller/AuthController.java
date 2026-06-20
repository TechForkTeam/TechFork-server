package com.techfork.auth.presentation.controller;

import com.techfork.auth.application.command.AuthCommandService;
import com.techfork.auth.application.command.input.LogoutCommand;
import com.techfork.auth.application.command.input.RefreshTokenCommand;
import com.techfork.auth.application.command.result.TokenRefreshResult;
import com.techfork.auth.presentation.annotation.AuthApi;
import com.techfork.auth.presentation.converter.AuthTokenConverter;
import com.techfork.auth.presentation.response.TokenRefreshResponse;
import com.techfork.auth.security.cookie.RefreshTokenCookieWriter;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AuthApi
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthCommandService authCommandService;
    private final AuthTokenConverter authTokenConverter;
    private final RefreshTokenCookieWriter refreshTokenCookieWriter;

    @Operation(
            summary = "토큰 갱신",
            description = "쿠키의 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다. 새로운 리프레시 토큰은 HttpOnly 쿠키로 설정됩니다."
    )
    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<TokenRefreshResponse>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        RefreshTokenCommand command = authTokenConverter.toRefreshTokenCommand(refreshToken);
        TokenRefreshResult result = authCommandService.refreshToken(command);
        refreshTokenCookieWriter.write(response, result.refreshToken(), result.refreshTokenExpiration());

        TokenRefreshResponse tokenResponse = authTokenConverter.toTokenRefreshResponse(result);
        return BaseResponse.of(SuccessCode.OK, tokenResponse);
    }

    @Operation(
            summary = "로그아웃",
            description = "쿠키의 리프레시 토큰을 사용하여 로그아웃합니다. Redis에서 토큰을 삭제하고 쿠키를 제거합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        LogoutCommand command = authTokenConverter.toLogoutCommand(refreshToken);
        authCommandService.logout(command);
        refreshTokenCookieWriter.delete(response);
        return BaseResponse.of(SuccessCode.OK);
    }
}
