package com.techfork.domain.admin.controller;

import com.techfork.auth.application.dto.DeveloperTokenResponse;
import com.techfork.auth.application.AuthService;
import com.techfork.domain.source.service.CrawlingService;
import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import com.techfork.global.security.oauth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Admin", description = "관리자 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final JobLauncher jobLauncher;
    private final Job summaryAndEmbeddingJob;
    private final CrawlingService crawlingService;

    @Operation(
            summary = "개발자 토큰 발급 (ADMIN 전용)",
            description = "프론트엔드 개발/테스트용 장수명 액세스 토큰(30일)을 발급합니다. ADMIN 권한이 있는 사용자만 접근할 수 있습니다."
    )
    @PostMapping("/developer-token")
    public ResponseEntity<BaseResponse<DeveloperTokenResponse>> generateDeveloperToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        DeveloperTokenResponse response = authService.generateDeveloperToken(userPrincipal.getId());
        return BaseResponse.of(SuccessCode.OK, response);
    }

    @Operation(
            summary = "요약 추출 + 임베딩 생성 배치 실행 (ADMIN 전용)",
            description = "요약이 없는 게시글의 요약을 추출하고, 임베딩을 생성하여 Elasticsearch에 색인합니다. (크롤링 제외)"
    )
    @PostMapping("/batch/summary-and-embedding")
    public ResponseEntity<BaseResponse<Void>> runSummaryAndEmbeddingBatch() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(summaryAndEmbeddingJob, jobParameters);

            return BaseResponse.of(SuccessCode.OK);

        } catch (JobExecutionAlreadyRunningException | JobRestartException |
                 JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            log.error("배치 실행 실패", e);
            throw new RuntimeException("배치 실행 중 오류 발생: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "RSS 크롤링 실행", description = "모든 테크 블로그의 RSS를 크롤링하여 DB에 저장합니다.")
    @PostMapping("/batch/crawl-rss")
    public ResponseEntity<BaseResponse<String>> crawlRss() {
        crawlingService.executeCrawling();
        return BaseResponse.of(SuccessCode.OK, "RSS 크롤링이 성공적으로 시작되었습니다.");
    }
}