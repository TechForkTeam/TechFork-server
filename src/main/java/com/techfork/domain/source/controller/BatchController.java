package com.techfork.domain.source.controller;

import com.techfork.global.common.code.SuccessCode;
import com.techfork.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Batch", description = "배치 작업 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job rssCrawlingJob;

    @Operation(summary = "RSS 크롤링 실행", description = "모든 테크 블로그의 RSS를 크롤링하여 DB에 저장합니다.")
    @PostMapping("/crawl-rss")
    public ResponseEntity<BaseResponse<String>> crawlRss() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("requestTime", LocalDateTime.now().toString())
                    .toJobParameters();

            jobLauncher.run(rssCrawlingJob, jobParameters);

            log.info("RSS 크롤링 Job 실행 완료");
            return BaseResponse.of(SuccessCode.OK, "RSS 크롤링이 성공적으로 시작되었습니다.");

        } catch (Exception e) {
            log.error("RSS 크롤링 Job 실행 실패", e);
            return BaseResponse.of(SuccessCode.OK, "RSS 크롤링 실행 중 오류 발생: " + e.getMessage());
        }
    }
}
