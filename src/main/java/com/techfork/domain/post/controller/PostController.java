package com.techfork.domain.post.controller;

import com.techfork.domain.post.dto.CompanyListResponse;
import com.techfork.domain.post.dto.PostDetailDto;
import com.techfork.domain.post.dto.PostListResponse;
import com.techfork.domain.post.dto.PostSortType;
import com.techfork.domain.post.service.PostQueryService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Post", description = "게시글 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostQueryService postQueryService;

    @Operation(
            summary = "게시글이 있는 회사 목록 조회",
            description = "게시글이 존재하는 회사명 목록을 조회합니다. (필터링 칩용)"
    )
    @GetMapping("/companies")
    public ResponseEntity<BaseResponse<CompanyListResponse>> getCompanies() {
        CompanyListResponse response = postQueryService.getCompanies();
        return BaseResponse.of(SuccessCode.OK, response);
    }
}
