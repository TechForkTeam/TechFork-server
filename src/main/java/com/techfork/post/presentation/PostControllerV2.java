package com.techfork.post.presentation;

import com.techfork.post.presentation.response.CompanyListResponse;
import com.techfork.post.presentation.response.PostListResponse;
import com.techfork.post.application.query.result.GetCompanyListResult;
import com.techfork.post.application.query.result.GetPostListResult;
import com.techfork.post.application.query.GetPostsByCompanyV2Query;
import com.techfork.post.application.query.GetRecentPostsV2Query;
import com.techfork.post.domain.enums.EPostSortType;
import com.techfork.post.application.query.PostQueryService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import com.techfork.global.security.oauth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Post V2", description = "게시글 API V2")
@Slf4j
@RestController
@RequestMapping("/api/v2/posts")
@RequiredArgsConstructor
public class PostControllerV2 {

    private final PostQueryService postQueryService;
    private final PostConverter postConverter;

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
        GetCompanyListResult result = postQueryService.getCompaniesV2();
        CompanyListResponse response = postConverter.toCompanyListResponseV2(result);
        return BaseResponse.of(SuccessCode.OK, response);
    }

    @Operation(
            summary = "기업별 게시글 조회 (V2)",
            description = """
                        여러 기업의 게시글을 무한 스크롤 방식으로 조회합니다.
                        companies 파라미터가 없으면 전체 게시글을 조회합니다.
                        초기에는 lastPublishedAt과 lastPostId를 빈 채로 호출하고,
                        페이징을 할 땐 lastPublishedAt과 lastPostId를 둘 다 동시에 보내주셔야 합니다.
                        페이징 관련 값은 응답으로 반환됩니다.
                        로그인 시 북마크 여부가 포함됩니다.
                        """
    )
    @GetMapping("/by-company")
    public ResponseEntity<BaseResponse<PostListResponse>> getPostsByCompany(
            @Parameter(description = "회사명 필터 (선택, 없으면 전체 조회)")
            @RequestParam(required = false) List<String> companies,

            @Parameter(description = "마지막 게시글 발행시간 (커서 1, 선택)")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @RequestParam(required = false) LocalDateTime lastPublishedAt,

            @Parameter(description = "마지막 게시글 ID (커서 2, 선택)")
            @RequestParam(required = false) Long lastPostId,

            @Parameter(description = "페이지 크기 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        GetPostsByCompanyV2Query query = new GetPostsByCompanyV2Query(companies, lastPublishedAt, lastPostId, size, userId);
        GetPostListResult result = postQueryService.getPostsByCompanyV2(query);
        PostListResponse response = postConverter.toPostListResponse(result);
        return BaseResponse.of(SuccessCode.OK, response);
    }

    @Operation(
            summary = "최근 게시글 조회 (V2)",
            description = """
                    최근 생성된 게시글을 무한 스크롤 방식으로 조회합니다.
                    sortBy로 정렬 기준을 선택할 수 있습니다.
                    - LATEST: publishedAt 기준 정렬, lastPublishedAt과 lastPostId 필요
                    - POPULAR: viewCount 기준 정렬, lastViewCount와 lastPostId 필요
                    초기 요청 시에는 커서 파라미터를 비워두고, 페이징 시 응답에서 받은 값을 모두 전달해주셔야 합니다.
                    로그인 시 북마크 여부가 포함됩니다.
                    """
    )
    @GetMapping("/recent")
    public ResponseEntity<BaseResponse<PostListResponse>> getRecentPosts(
            @Parameter(description = "정렬 기준 (LATEST: 최신순, POPULAR: 인기순, 기본값: LATEST)")
            @RequestParam(defaultValue = "LATEST") EPostSortType sortBy,

            @Parameter(description = "마지막 게시글 조회수 (커서, POPULAR 정렬 시 필요)")
            @RequestParam(required = false) Integer lastViewCount,

            @Parameter(description = "마지막 게시글 발행시간 (커서, LATEST 정렬 시 필요)")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @RequestParam(required = false) LocalDateTime lastPublishedAt,

            @Parameter(description = "마지막 게시글 ID (커서, 선택)")
            @RequestParam(required = false) Long lastPostId,

            @Parameter(description = "페이지 크기 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        GetRecentPostsV2Query query = new GetRecentPostsV2Query(sortBy, lastViewCount, lastPublishedAt, lastPostId, size, userId);
        GetPostListResult result = postQueryService.getRecentPostsV2(query);
        PostListResponse response = postConverter.toPostListResponse(result);
        return BaseResponse.of(SuccessCode.OK, response);
    }
}
