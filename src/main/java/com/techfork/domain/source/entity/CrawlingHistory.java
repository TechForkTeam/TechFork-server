package com.techfork.domain.source.entity;

import com.techfork.domain.source.enums.ECrawlingStatus;
import com.techfork.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "crawling_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlingHistory extends BaseTimeEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ECrawlingStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Column(nullable = false)
    private Integer totalCount = 0;

    @Column(nullable = false)
    private Integer successCount = 0;

    @Column(nullable = false)
    private Integer failCount = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "job_execution_id")
    private Long jobExecutionId;

    @Builder(access = AccessLevel.PRIVATE)
    private CrawlingHistory(ECrawlingStatus status, LocalDateTime startedAt, Long jobExecutionId) {
        this.status = status;
        this.startedAt = startedAt;
        this.jobExecutionId = jobExecutionId;
    }

    public static CrawlingHistory createStarted(Long jobExecutionId) {
        return CrawlingHistory.builder()
                .status(ECrawlingStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .jobExecutionId(jobExecutionId)
                .build();
    }

    public void complete(Integer totalCount, Integer successCount, Integer failCount) {
        this.status = ECrawlingStatus.SUCCESS;
        this.endedAt = LocalDateTime.now();
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failCount = failCount;
    }

    public void fail(String errorMessage) {
        this.status = ECrawlingStatus.FAILED;
        this.endedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }
}
