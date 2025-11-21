package com.techfork.domain.search.controller;

import com.techfork.domain.search.dto.SearchResult;
import com.techfork.domain.search.service.SearchService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @Operation(summary = "1단계 검색(BM25 + 시맨틱)", description = "검색어를 기반으로 BM25 + k-NN 하이브리드 검색을 수행하고 합산하여 상위 결과를 반환합니다. (개인화 미적용)")
    @GetMapping("/general")
    public BaseResponse<List<SearchResult>> searchGeneral(
            @RequestParam @Parameter(description = "검색어", required = true) String query
    ) {
        List<SearchResult> results = searchService.searchGeneral(query);
        return BaseResponse.of(SuccessCode.OK, results).getBody();
    }

    @Operation(summary = "2단계 검색(1단계 검색 후보군 추출 -> 순위 조정", description = "검색어를 기반으로 1차 검색(BM25 + k-NN)을 수행한 후보군을 개인화 리랭킹 적용합니다.")
    @GetMapping("/personalized")
    public BaseResponse<List<SearchResult>> searchPersonalized(
            @RequestParam @Parameter(description = "검색어", required = true) String query,
            @RequestParam @Parameter(description = "사용자 ID", required = true) Long userId
    ) {
       List<SearchResult> results = searchService.searchPersonalized(query, userId) ;
       return BaseResponse.of(SuccessCode.OK, results).getBody();
    }
}