package com.techfork.domain.source.batch;

import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("local")
class RssCrawlingJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job rssCrawlingJob;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    @Test
    @DisplayName("RSS 크롤링 Job이 정상적으로 실행되고 게시글이 저장된다")
    void rssCrawlingJob_shouldSavePostsSuccessfully() throws Exception {
        // given
        jobLauncherTestUtils.setJob(rssCrawlingJob);

        long techBlogCount = techBlogRepository.count();
        long beforePostCount = postRepository.count();

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(jobExecution.getStatus().isUnsuccessful()).isFalse();

        long afterPostCount = postRepository.count();
        log.info("저장된 Post 개수: {} -> {} (증가: {})",
                beforePostCount, afterPostCount, afterPostCount - beforePostCount);

        // 최소 1개 이상의 게시글이 수집되었는지 확인
        assertThat(afterPostCount).isGreaterThan(beforePostCount);
    }

    @Test
    @DisplayName("중복 URL은 저장되지 않는다")
    void rssCrawlingJob_shouldSkipDuplicateUrls() throws Exception {
        // given - 첫 번째 실행
        jobLauncherTestUtils.setJob(rssCrawlingJob);

        JobParameters firstRun = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncherTestUtils.launchJob(firstRun);
        long countAfterFirstRun = postRepository.count();
        log.info("첫 번째 실행 후 Post 개수: {}", countAfterFirstRun);

        // when - 두 번째 실행 (중복 데이터)
        Thread.sleep(100); // timestamp 중복 방지
        JobParameters secondRun = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution secondJobExecution = jobLauncherTestUtils.launchJob(secondRun);

        // then - 중복 체크로 인해 새로운 게시글은 추가되지 않음
        long countAfterSecondRun = postRepository.count();
        log.info("두 번째 실행 후 Post 개수: {}", countAfterSecondRun);

        assertThat(secondJobExecution.getStatus().isUnsuccessful()).isFalse();
        assertThat(countAfterSecondRun).isEqualTo(countAfterFirstRun);
    }
}
