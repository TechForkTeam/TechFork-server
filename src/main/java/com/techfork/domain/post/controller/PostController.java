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

    @Operation(
            summary = "기업별 게시글 조회",
            description = "특정 기업의 게시글을 무한 스크롤 방식으로 조회합니다. company 파라미터가 없으면 전체 게시글을 조회합니다."
    )
    @GetMapping("/by-company")
    public ResponseEntity<BaseResponse<PostListResponse>> getPostsByCompany(
            @Parameter(description = "회사명 필터 (선택, 없으면 전체 조회)")
            @RequestParam(required = false) String company,
            @Parameter(description = "마지막 게시글 ID (커서, 선택)")
            @RequestParam(required = false) Long lastPostId,
            @Parameter(description = "페이지 크기 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        PostListResponse response = postQueryService.getPostsByCompany(company, lastPostId, size);
        return BaseResponse.of(SuccessCode.OK, response);
    }

    @Operation(
            summary = "최근 게시글 조회",
            description = "최근 생성된 게시글을 무한 스크롤 방식으로 조회합니다. sortBy로 정렬 기준을 선택할 수 있습니다."
    )
    @GetMapping("/recent")
    public ResponseEntity<BaseResponse<PostListResponse>> getRecentPosts(
            @Parameter(description = "정렬 기준 (LATEST: 최신순, POPULAR: 인기순, 기본값: LATEST)")
            @RequestParam(defaultValue = "LATEST") PostSortType sortBy,
            @Parameter(description = "마지막 게시글 ID (커서, 선택)")
            @RequestParam(required = false) Long lastPostId,
            @Parameter(description = "페이지 크기 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        PostListResponse response = postQueryService.getRecentPosts(sortBy, lastPostId, size);
        return BaseResponse.of(SuccessCode.OK, response);
    }

    @Operation(
            summary = "게시글 상세 조회",
            description = "특정 게시글의 상세 정보를 조회합니다."
    )
    @GetMapping("/{postId}")
    public ResponseEntity<BaseResponse<PostDetailDto>> getPostDetail(
            @Parameter(description = "게시글 ID")
            @PathVariable Long postId
    ) {
        PostDetailDto response = postQueryService.getPostDetail(postId);
        return BaseResponse.of(SuccessCode.OK, response);
    }
}
