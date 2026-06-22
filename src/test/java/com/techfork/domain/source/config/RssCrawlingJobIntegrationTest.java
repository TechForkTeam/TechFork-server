package com.techfork.domain.source.config;

import com.techfork.post.application.batch.PostEmbeddingProcessor;
import com.techfork.post.infrastructure.batch.PostEmbeddingReader;
import com.techfork.post.infrastructure.batch.PostEmbeddingWriter;
import com.techfork.post.application.batch.PostSummaryProcessor;
import com.techfork.post.infrastructure.batch.PostSummaryReader;
import com.techfork.post.infrastructure.batch.PostSummaryWriter;
import com.techfork.post.domain.projection.ContentChunk;
import com.techfork.post.domain.projection.PostDocument;
import com.techfork.post.domain.Post;
import com.techfork.domain.source.batch.PostBatchWriter;
import com.techfork.domain.source.batch.RssFeedReader;
import com.techfork.domain.source.batch.RssToPostProcessor;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.post.fixture.PostFixture;
import com.techfork.domain.source.listener.RssCrawlingJobListener;
import com.techfork.global.common.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBatchTest
class RssCrawlingJobIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    @Qualifier("rssCrawlingJob")
    private Job rssCrawlingJob;

    @Autowired
    @Qualifier("summaryAndEmbeddingJob")
    private Job summaryAndEmbeddingJob;

    @MockitoBean
    private RssFeedReader rssFeedReader;

    @MockitoBean
    private RssToPostProcessor rssToPostProcessor;

    @MockitoBean
    private PostBatchWriter postBatchWriter;

    @MockitoBean
    private PostSummaryReader postSummaryReader;

    @MockitoBean
    private PostSummaryProcessor postSummaryProcessor;

    @MockitoBean
    private PostSummaryWriter postSummaryWriter;

    @MockitoBean
    private PostEmbeddingReader postEmbeddingReader;

    @MockitoBean
    private PostEmbeddingProcessor postEmbeddingProcessor;

    @MockitoBean
    private PostEmbeddingWriter postEmbeddingWriter;

    @MockitoBean
    private RssCrawlingJobListener rssCrawlingJobListener;

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
        awaitCompletion(execution);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        verify(rssFeedReader, times(2)).read();
        verify(rssToPostProcessor).process(item);

        ArgumentCaptor<Chunk<? extends Post>> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(postBatchWriter).write(chunkCaptor.capture());
        assertThat(chunkCaptor.getValue().getItems()).hasSize(1);
        assertThat(chunkCaptor.getValue().getItems().get(0)).isSameAs(post);
    }

    @Test
    @DisplayName("extractSummaryStep은 reader, async processor, writer wiring을 유지한다")
    void extractSummaryStep_WiresReaderAsyncProcessorAndWriter() throws Exception {
        Post post = PostFixture.createPost(11L, "summary-step", "원문", "평문", "TechFork", "요약 전", null);

        given(postSummaryReader.read()).willReturn(post).willReturn((Post) null);
        given(postSummaryProcessor.process(post)).willReturn(post);

        JobExecution execution = jobLauncherTestUtils.launchStep("extractSummaryStep");
        awaitCompletion(execution);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verify(postSummaryReader, times(2)).read();
        verify(postSummaryProcessor).process(post);

        ArgumentCaptor<Chunk<? extends Post>> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(postSummaryWriter).write(chunkCaptor.capture());
        assertThat(chunkCaptor.getValue().getItems()).hasSize(1);
        assertThat(chunkCaptor.getValue().getItems().get(0)).isSameAs(post);
    }

    @Test
    @DisplayName("embedAndIndexStep은 reader, async processor, writer wiring을 유지한다")
    void embedAndIndexStep_WiresReaderAsyncProcessorAndWriter() throws Exception {
        Post post = PostFixture.createPost(21L, "embed-step", "원문", "평문", "TechFork", "요약 완료", "짧은 요약");
        PostDocument postDocument = PostDocument.create(
                post,
                List.of(0.1f, 0.2f),
                List.of(0.3f, 0.4f),
                List.of(ContentChunk.create(0, "chunk", List.of(0.5f, 0.6f)))
        );

        given(postEmbeddingReader.read()).willReturn(post).willReturn((Post) null);
        given(postEmbeddingProcessor.process(post)).willReturn(postDocument);

        JobExecution execution = jobLauncherTestUtils.launchStep("embedAndIndexStep");
        awaitCompletion(execution);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verify(postEmbeddingReader, times(2)).read();
        verify(postEmbeddingProcessor).process(post);

        ArgumentCaptor<Chunk<? extends PostDocument>> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(postEmbeddingWriter).write(chunkCaptor.capture());
        assertThat(chunkCaptor.getValue().getItems()).hasSize(1);
        assertThat(chunkCaptor.getValue().getItems().get(0)).isSameAs(postDocument);
    }

    @Test
    @DisplayName("summaryAndEmbeddingJob은 summary 다음 embed step만 실행한다")
    void summaryAndEmbeddingJob_ExecutesOnlySummaryThenEmbedSteps() throws Exception {
        jobLauncherTestUtils.setJob(summaryAndEmbeddingJob);

        Post summaryPost = PostFixture.createPost(31L, "summary-job", "원문", "평문", "TechFork", "요약 전", null);
        Post embeddedPost = PostFixture.createPost(32L, "embed-job", "원문", "평문", "TechFork", "요약 완료", "짧은 요약");
        PostDocument postDocument = PostDocument.create(
                embeddedPost,
                List.of(0.1f),
                List.of(0.2f),
                List.of(ContentChunk.create(0, "chunk", List.of(0.3f)))
        );

        given(postSummaryReader.read()).willReturn(summaryPost).willReturn((Post) null);
        given(postSummaryProcessor.process(summaryPost)).willReturn(summaryPost);
        given(postEmbeddingReader.read()).willReturn(embeddedPost).willReturn((Post) null);
        given(postEmbeddingProcessor.process(embeddedPost)).willReturn(postDocument);

        JobExecution execution = jobLauncherTestUtils.launchJob();
        awaitCompletion(execution);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(sortedStepNames(execution))
                .containsExactly("extractSummaryStep", "embedAndIndexStep");
        verify(rssFeedReader, never()).read();
        verify(rssToPostProcessor, never()).process(any());
        verify(postBatchWriter, never()).write(any());
    }

    @Test
    @DisplayName("rssCrawlingJob은 fetch, summary, embed 순서로 실행된다")
    void rssCrawlingJob_ExecutesFetchSummaryEmbedInOrder() throws Exception {
        jobLauncherTestUtils.setJob(rssCrawlingJob);

        RssFeedItem item = new RssFeedItem(
                "순서 검증",
                "https://posts.example.com/order",
                "https://logo.example.com/logo.png",
                null,
                "본문",
                "평문",
                LocalDateTime.of(2026, 4, 13, 7, 0, 0),
                "카카오",
                41L
        );
        Post fetchedPost = PostFixture.createPost(41L, "fetch-job", "본문", "본문", "TechFork", null, null);
        Post summaryPost = PostFixture.createPost(42L, "summary-job", "본문", "본문", "TechFork", "요약 완료", "짧은 요약");
        PostDocument postDocument = PostDocument.create(
                summaryPost,
                List.of(0.1f),
                List.of(0.2f),
                List.of(ContentChunk.create(0, "chunk", List.of(0.3f)))
        );

        given(rssFeedReader.read()).willReturn(item).willReturn((RssFeedItem) null);
        given(rssToPostProcessor.process(item)).willReturn(fetchedPost);
        given(postSummaryReader.read()).willReturn(summaryPost).willReturn((Post) null);
        given(postSummaryProcessor.process(summaryPost)).willReturn(summaryPost);
        given(postEmbeddingReader.read()).willReturn(summaryPost).willReturn((Post) null);
        given(postEmbeddingProcessor.process(summaryPost)).willReturn(postDocument);

        JobExecution execution = jobLauncherTestUtils.launchJob();
        awaitCompletion(execution);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(sortedStepNames(execution))
                .containsExactly("fetchAndSaveRssStep", "extractSummaryStep", "embedAndIndexStep");
        verify(rssFeedReader, times(2)).read();
        verify(postSummaryReader, times(2)).read();
        verify(postEmbeddingReader, times(2)).read();
    }

    private List<String> sortedStepNames(JobExecution execution) {
        return execution.getStepExecutions().stream()
                .sorted(Comparator.comparing(StepExecution::getId))
                .map(StepExecution::getStepName)
                .toList();
    }

    private void awaitCompletion(JobExecution execution) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (execution.getStatus().isRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
    }
}
