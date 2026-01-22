package com.techfork.domain.admin.controller;

import com.techfork.domain.auth.dto.DeveloperTokenResponse;
import com.techfork.domain.auth.service.AuthService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import com.techfork.global.security.oauth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin", description = "관리자 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    @Operation(
            summary = "개발자 토큰 발급 (ADMIN 전용)",
            description = "프론트엔드 개발/테스트용 장수명 액세스 토큰(30일)을 발급합니다. ADMIN 권한이 있는 사용자만 접근할 수 있습니다."
    )
    @PostMapping("/developer-token")
    public ResponseEntity<BaseResponse<DeveloperTokenResponse>> generateDeveloperToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        DeveloperTokenResponse response = authService.generateDeveloperToken(userPrincipal.getId());
        return BaseResponse.of(SuccessCode.OK, response);
    }
}