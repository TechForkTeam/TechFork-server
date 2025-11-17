package com.techfork.domain.user.controller;

import com.techfork.domain.user.dto.InterestListResponse;
import com.techfork.domain.user.service.InterestQueryService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Onboarding", description = "온보딩 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final InterestQueryService interestQueryService;

    @Operation(
            summary = "관심사 목록 조회",
            description = "온보딩 시 선택 가능한 모든 관심사 카테고리와 키워드 목록을 조회합니다."
    )
    @GetMapping("/interests")
    public ResponseEntity<BaseResponse<InterestListResponse>> getInterests() {
        InterestListResponse response = interestQueryService.getAllInterests();
        return BaseResponse.of(SuccessCode.OK, response);
    }
}
