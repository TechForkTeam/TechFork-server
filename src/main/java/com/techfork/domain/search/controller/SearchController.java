package com.techfork.domain.search.controller;

import com.techfork.domain.search.dto.SearchResult;
import com.techfork.domain.search.service.SearchService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import com.techfork.global.security.oauth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "검색 API", description = "Elasticsearch 기반의 통합 검색 기능을 제공합니다.")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "테스트용 : 1단계 검색 - BM25")
    @GetMapping("/bm25")
    public BaseResponse<List<SearchResult>> searchBm25(
            @RequestParam @Parameter(description = "검색어", required = true) String query
    ) {
        List<SearchResult> results = searchService.searchOnlyBm25(query);
        return BaseResponse.of(SuccessCode.OK, results).getBody();
    }

    @Operation(summary = "테스트용 : 1단계 검색 - semantic")
    @GetMapping("/semantic")
    public BaseResponse<List<SearchResult>> searchSemantic(
            @RequestParam @Parameter(description = "검색어", required = true) String query
    ) {
        List<SearchResult> results = searchService.searchOnlySemantic(query);
        return BaseResponse.of(SuccessCode.OK, results).getBody();
    }


    @Operation(
            summary = "통합 검색",
            description = "검색어를 기반으로 하이브리드 검색(BM25 + k-NN)을 수행합니다. " +
                    "인증된 사용자의 경우 개인화 리랭킹이 적용되고, 비인증 사용자는 일반 검색 결과를 반환합니다."
    )
    @GetMapping
    public BaseResponse<List<SearchResult>> search(
            @RequestParam @Parameter(description = "검색어", required = true) String query,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<SearchResult> results;

        if (userPrincipal != null) {
            results = searchService.searchPersonalized(query, userPrincipal.getId());
        } else {
            results = searchService.searchGeneral(query);
        }

        return BaseResponse.of(SuccessCode.OK, results).getBody();
    }
}