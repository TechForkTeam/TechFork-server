package com.techfork.domain.recommendation.entity;

import com.techfork.post.domain.Post;
import com.techfork.useraccount.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.techfork.post.fixture.PostFixture.createPost;
import static com.techfork.domain.recommendation.fixture.RecommendedPostFixture.recommendedPost;
import static com.techfork.domain.source.fixture.TechBlogFixture.createTechBlog;
import static com.techfork.useraccount.fixture.UserFixture.socialUser;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecommendationHistory 단위 테스트")
class RecommendationHistoryTest {

    @Nested
    @DisplayName("fromRecommendedPost")
    class FromRecommendedPost {

        @Test
        @DisplayName("현재 추천 게시글로부터 추천 이력을 생성한다")
        void recommendedPost_CopiesRecommendationSnapshot() {
            User user = socialUser("recommendation-history-user", "history@example.com");
            Post post = createPost(createTechBlog("테스트회사", "https://history-test.com"), "추천 이력 테스트 게시글", "https://history-test.com/posts/1");
            RecommendedPost recommendedPost = recommendedPost(user, post, 0.91, 0.82, 3);

            RecommendationHistory history = RecommendationHistory.fromRecommendedPost(recommendedPost);

            assertThat(history.getUser()).isSameAs(user);
            assertThat(history.getPost()).isSameAs(post);
            assertThat(history.getSimilarityScore()).isEqualTo(0.91);
            assertThat(history.getMmrScore()).isEqualTo(0.82);
            assertThat(history.getRankOrder()).isEqualTo(3);
            assertThat(history.getRecommendedAt()).isEqualTo(recommendedPost.getRecommendedAt());
            assertThat(history.getIsClicked()).isFalse();
            assertThat(history.getClickedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("markAsClicked")
    class MarkAsClicked {

        @Test
        @DisplayName("추천 이력을 클릭 상태로 표시한다")
        void unclickedHistory_MarksAsClicked() {
            RecommendationHistory history = RecommendationHistory.fromRecommendedPost(
                    recommendedPost(
                            socialUser("recommendation-history-user", "history@example.com"),
                            createPost(createTechBlog("테스트회사", "https://history-test.com"), "추천 이력 테스트 게시글", "https://history-test.com/posts/1"),
                            0.91,
                            0.82,
                            1
                    )
            );

            LocalDateTime beforeClick = LocalDateTime.now();
            history.markAsClicked();
            LocalDateTime afterClick = LocalDateTime.now();

            assertThat(history.getIsClicked()).isTrue();
            assertThat(history.getClickedAt()).isBetween(beforeClick, afterClick);
        }
    }

}
