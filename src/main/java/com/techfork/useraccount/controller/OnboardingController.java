package com.techfork.useraccount.controller;

import com.techfork.useraccount.application.query.result.GetInterestListResult;
import com.techfork.useraccount.converter.InterestConverter;
import com.techfork.useraccount.converter.UserConverter;
import com.techfork.useraccount.dto.InterestListResponse;
import com.techfork.useraccount.dto.OnboardingRequest;
import com.techfork.useraccount.application.query.InterestQueryService;
import com.techfork.useraccount.application.command.UserCommandService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import com.techfork.global.security.oauth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Onboarding", description = "온보딩 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final InterestQueryService interestQueryService;
    private final UserCommandService userCommandService;
    private final InterestConverter interestConverter;
    private final UserConverter userConverter;

    @Operation(
            summary = "관심사 목록 조회",
            description = "온보딩 시 선택 가능한 모든 관심사 카테고리와 키워드 목록을 조회합니다."
    )
    @GetMapping("/interests")
    public ResponseEntity<BaseResponse<InterestListResponse>> getInterests() {
        GetInterestListResult result = interestQueryService.getAllInterests();
        InterestListResponse response = interestConverter.toInterestListResponse(result);
        return BaseResponse.of(SuccessCode.OK, response);
    }

    @Operation(
            summary = "내 정보 및 관심사 저장",
            description = "온보딩 시 사용자의 정보와 선택한 관심사를 저장합니다. 카테고리별로 세부 키워드를 선택할 수 있습니다."
    )
    @PostMapping("/complete")
    public ResponseEntity<BaseResponse<Void>> completeOnboarding(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OnboardingRequest request
    ) {
        userCommandService.completeOnboarding(
                userConverter.toCompleteOnboardingCommand(
                        userPrincipal.getId(),
                        request,
                        interestConverter.toUserInterestCommands(request.interests())
                )
        );
        return BaseResponse.of(SuccessCode.CREATED);
    }
}
