package com.techfork.domain.post.controller;

import com.techfork.domain.post.dto.CompanyListResponse;
import com.techfork.domain.post.service.PostQueryService;
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

@Tag(name = "Post V2", description = "게시글 API V2")
@Slf4j
@RestController
@RequestMapping("/api/v2/posts")
@RequiredArgsConstructor
public class PostControllerV2 {

    private final PostQueryService postQueryService;

    @Operation(
            summary = "게시글이 있는 회사 목록 조회 (V2)",
            description = """
                    게시글이 존재하는 회사 목록을 조회합니다.
                    - 최신 게시글 발행일 기준으로 정렬
                    - 오늘 발행된 게시글이 있는지 여부 포함
                    - 회사 로고 URL 포함
                    """
    )
    @GetMapping("/companies")
    public ResponseEntity<BaseResponse<CompanyListResponse>> getCompanies() {
        CompanyListResponse response = postQueryService.getCompaniesV2();
        return BaseResponse.of(SuccessCode.OK, response);
    }
}
