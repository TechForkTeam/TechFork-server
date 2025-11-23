package com.techfork.domain.recommendation.controller;

import com.techfork.domain.recommendation.dto.RecommendationListResponse;
import com.techfork.domain.recommendation.service.RecommendationQueryService;
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

@Tag(name = "Recommendation", description = "추천 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationQueryService recommendationQueryService;

    @Operation(
            summary = "추천 게시글 조회",
            description = "사용자의 개인화된 추천 게시글 목록을 조회합니다. MMR 알고리즘으로 생성된 다양성 있는 추천을 제공합니다."
    )
    @GetMapping
    public ResponseEntity<BaseResponse<RecommendationListResponse>> getRecommendations() {
        // TODO: userId Auth 인증 기반으로 추출
        Long userId = 1L;

        RecommendationListResponse response = recommendationQueryService.getRecommendations(userId);
        return BaseResponse.of(SuccessCode.OK, response);
    }
}
