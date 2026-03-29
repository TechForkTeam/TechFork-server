package com.techfork.domain.source.config;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.source.batch.PostBatchWriter;
import com.techfork.domain.source.batch.RssFeedReader;
import com.techfork.domain.source.batch.RssToPostProcessor;
import com.techfork.domain.source.dto.RssFeedItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "classpath:org/springframework/batch/core/schema-h2.sql")
class RssCrawlingJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private Job rssCrawlingJob;

    @MockitoBean
    private RssFeedReader rssFeedReader;

    @MockitoBean
    private RssToPostProcessor rssToPostProcessor;

    @MockitoBean
    private PostBatchWriter postBatchWriter;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(rssCrawlingJob);
    }

    @AfterEach
    void tearDown() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("fetchAndSaveRssStep은 RSS item을 읽어 processor/writer로 전달한다")
    void fetchAndSaveRssStep_WiresReaderProcessorWriter() throws Exception {
        RssFeedItem item = new RssFeedItem(
                "테스트 제목",
                "https://posts.example.com/1",
                "https://logo.example.com/logo.png",
                null,
                "본문",
                "본문",
                java.time.LocalDateTime.now(),
                "카카오",
                1L
        );
        Post post = mock(Post.class);

        given(rssFeedReader.read()).willReturn(item, (RssFeedItem) null);
        given(rssToPostProcessor.process(item)).willReturn(post);

        JobExecution execution = jobLauncherTestUtils.launchStep("fetchAndSaveRssStep");

        long deadline = System.currentTimeMillis() + 5_000;
        while (execution.isRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        verify(rssFeedReader, times(2)).read();
        verify(rssToPostProcessor).process(item);

        ArgumentCaptor<Chunk<? extends Post>> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(postBatchWriter).write(chunkCaptor.capture());
        assertThat(chunkCaptor.getValue().getItems()).hasSize(1);
        assertThat(chunkCaptor.getValue().getItems().get(0)).isSameAs(post);
    }
}
