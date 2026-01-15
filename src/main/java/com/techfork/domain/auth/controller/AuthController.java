package com.techfork.domain.auth.controller;

import com.techfork.domain.auth.dto.TokenRefreshResponse;
import com.techfork.domain.auth.service.AuthService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
}
