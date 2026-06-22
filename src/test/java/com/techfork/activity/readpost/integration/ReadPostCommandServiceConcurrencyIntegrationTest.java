package com.techfork.activity.readpost.integration;

import com.techfork.activity.readpost.application.command.ReadPostCommandService;
import com.techfork.activity.readpost.application.command.SaveReadPostCommand;
import com.techfork.activity.readpost.infrastructure.FirstReadPostRepository;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.post.domain.Post;
import com.techfork.post.fixture.PostFixture;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.fixture.TechBlogFixture;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.fixture.UserFixture;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.common.MySqlRedisIntegrationTestBase;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ReadPostCommandServiceConcurrencyIntegrationTest extends MySqlRedisIntegrationTestBase {

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
        testUser = UserFixture.socialUser("concurrentUser", "concurrent@example.com");
        testUser = userRepository.save(testUser);

        TechBlog testBlog = TechBlogFixture.createTechBlog("테스트회사", "https://test.com");
        testBlog = techBlogRepository.save(testBlog);

        testPost = PostFixture.createPost(testBlog, "동시성 테스트 게시글", "전체 내용", "내용",
                "요약", "짧은 요약", "https://test.com/thumb.png",
                "https://test.com/post/concurrency", LocalDateTime.now().minusDays(1));
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

    @Nested
    @DisplayName("동시 읽은 게시글 저장")
    class SaveReadPostConcurrently {

        @Test
        @DisplayName("동시에 같은 게시글 읽기 요청이 와도 조회수는 한 번만 증가한다")
        void concurrentRequests_IncrementViewCountOnlyOnce() throws Exception {
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

}
