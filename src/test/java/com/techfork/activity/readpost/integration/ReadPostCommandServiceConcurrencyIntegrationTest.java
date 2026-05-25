package com.techfork.activity.readpost.integration;

import com.techfork.activity.readpost.application.command.ReadPostCommandService;
import com.techfork.activity.readpost.application.command.SaveReadPostCommand;
import com.techfork.activity.readpost.infrastructure.FirstReadPostRepository;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.post.domain.Post;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.useraccount.entity.User;
import com.techfork.useraccount.enums.SocialType;
import com.techfork.useraccount.repository.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ReadPostCommandServiceConcurrencyIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ReadPostCommandService readPostCommandService;

    @Autowired
    private ReadPostRepository readPostRepository;

    @Autowired
    private FirstReadPostRepository firstReadPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = User.createSocialUser(SocialType.KAKAO, "concurrentUser", "concurrent@example.com", "profile.jpg");
        testUser = userRepository.save(testUser);

        TechBlog testBlog = TechBlog.builder()
                .companyName("테스트회사")
                .blogUrl("https://test.com")
                .rssUrl("https://test.com/rss")
                .build();
        testBlog = techBlogRepository.save(testBlog);

        testPost = Post.builder()
                .title("동시성 테스트 게시글")
                .fullContent("전체 내용")
                .plainContent("내용")
                .summary("요약")
                .shortSummary("짧은 요약")
                .company("테스트회사")
                .url("https://test.com/post/concurrency")
                .publishedAt(LocalDateTime.now().minusDays(1))
                .crawledAt(LocalDateTime.now())
                .techBlog(testBlog)
                .build();
        testPost = postRepository.save(testPost);
    }

    @AfterEach
    void tearDown() {
        firstReadPostRepository.deleteAll();
        readPostRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("동시에 같은 게시글 읽기 요청이 와도 조회수는 한 번만 증가한다")
    void saveReadPost_ConcurrentRequests_IncrementViewCountOnlyOnce() throws Exception {
        int requestCount = 10;
        LocalDateTime baseReadAt = LocalDateTime.of(2026, 5, 8, 16, 0);
        long initialViewCount = testPost.getViewCount();
        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);

        try {
            for (int i = 0; i < requestCount; i++) {
                final int index = i;
                executorService.submit(() -> {
                    readyLatch.countDown();
                    try {
                        assertThat(startLatch.await(5, TimeUnit.SECONDS)).isTrue();
                        readPostCommandService.saveReadPost(new SaveReadPostCommand(
                                testUser.getId(),
                                testPost.getId(),
                                baseReadAt.plusSeconds(index),
                                120 + index
                        ));
                    } catch (Throwable throwable) {
                        failures.add(throwable);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();
            assertThat(doneLatch.await(15, TimeUnit.SECONDS)).isTrue();
        } finally {
            executorService.shutdownNow();
            assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(failures).isEmpty();
        assertThat(readPostRepository.count()).isEqualTo(requestCount);
        assertThat(firstReadPostRepository.count()).isEqualTo(1);

        Post updatedPost = postRepository.findById(testPost.getId()).orElseThrow();
        assertThat(updatedPost.getViewCount()).isEqualTo(initialViewCount + 1);
    }
}
