package com.techfork.auth.presentation;

import com.techfork.auth.application.KakaoLoginService;
import com.techfork.auth.application.dto.KakaoLoginRequest;
import com.techfork.auth.application.dto.KakaoLoginResponse;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/kakao")
@RequiredArgsConstructor
public class KakaoLoginController {

    private final KakaoLoginService kakaoLoginService;

    @Operation(
            summary = "카카오 로그인",
            description = "iOS 클라이언트에서 받은 카카오 액세스 토큰을 검증하고 자체 JWT 토큰을 발급합니다. 리프레시 토큰은 HttpOnly 쿠키로 설정됩니다."
    )
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<KakaoLoginResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request,
            HttpServletResponse response
    ) {
        KakaoLoginResponse loginResponse = kakaoLoginService.login(request.accessToken(), response);
        return BaseResponse.of(SuccessCode.OK, loginResponse);
    }
}
