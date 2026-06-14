package com.techfork.auth.presentation;

import com.techfork.auth.application.dto.KakaoLoginRequest;
import com.techfork.auth.application.dto.KakaoLoginResponse;
import com.techfork.auth.application.dto.TokenRefreshResponse;
import com.techfork.auth.application.AuthService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "카카오 로그인",
            description = "iOS 클라이언트에서 받은 카카오 액세스 토큰을 검증하고 자체 JWT 토큰을 발급합니다. 리프레시 토큰은 HttpOnly 쿠키로 설정됩니다."
    )
    @PostMapping("/kakao/login")
    public ResponseEntity<BaseResponse<KakaoLoginResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request,
            HttpServletResponse response
    ) {
        KakaoLoginResponse loginResponse = authService.kakaoLogin(request.accessToken(), response);
        return BaseResponse.of(SuccessCode.OK, loginResponse);
    }

    @Operation(
            summary = "토큰 갱신",
            description = "쿠키의 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다. 새로운 리프레시 토큰은 HttpOnly 쿠키로 설정됩니다."
    )
    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<TokenRefreshResponse>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        TokenRefreshResponse tokenResponse = authService.refreshToken(refreshToken, response);
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
        authService.logout(refreshToken, response);
        return BaseResponse.of(SuccessCode.OK);
    }
}
