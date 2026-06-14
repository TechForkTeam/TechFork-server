package com.techfork.auth.presentation.controller;

import com.techfork.auth.application.command.AuthCommandService;
import com.techfork.auth.application.command.input.GenerateDeveloperTokenCommand;
import com.techfork.auth.application.command.result.DeveloperTokenResult;
import com.techfork.auth.presentation.converter.DeveloperTokenConverter;
import com.techfork.auth.presentation.response.DeveloperTokenResponse;
import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class DeveloperTokenController {

    private final AuthCommandService authCommandService;
    private final DeveloperTokenConverter developerTokenConverter;

    @Operation(
            summary = "개발자 토큰 발급 (ADMIN 전용)",
            description = "프론트엔드 개발/테스트용 장수명 액세스 토큰(30일)을 발급합니다. ADMIN 권한이 있는 사용자만 접근할 수 있습니다."
    )
    @PostMapping("/developer-token")
    public ResponseEntity<BaseResponse<DeveloperTokenResponse>> generateDeveloperToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        GenerateDeveloperTokenCommand command = developerTokenConverter.toGenerateDeveloperTokenCommand(userPrincipal.getId());
        DeveloperTokenResult result = authCommandService.generateDeveloperToken(command);
        DeveloperTokenResponse response = developerTokenConverter.toDeveloperTokenResponse(result);
        return BaseResponse.of(SuccessCode.OK, response);
    }
}
