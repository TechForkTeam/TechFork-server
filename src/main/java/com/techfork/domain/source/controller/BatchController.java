package com.techfork.domain.source.controller;

import com.techfork.domain.source.service.CrawlingService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Batch", description = "배치 작업 API")
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {

    private final CrawlingService crawlingService;

    @Operation(summary = "RSS 크롤링 실행", description = "모든 테크 블로그의 RSS를 크롤링하여 DB에 저장합니다.")
    @PostMapping("/crawl-rss")
    public ResponseEntity<BaseResponse<String>> crawlRss() {
        crawlingService.executeCrawling();
        return BaseResponse.of(SuccessCode.OK, "RSS 크롤링이 성공적으로 시작되었습니다.");
    }
}
