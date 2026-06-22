package com.techfork.domain.recommendation.entity;

import com.techfork.domain.source.entity.TechBlog;
import com.techfork.post.domain.Post;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecommendationHistory 단위 테스트")
class RecommendationHistoryTest {

    @Test
    @DisplayName("현재 추천 게시글로부터 추천 이력을 생성한다")
    void fromRecommendedPost_CopiesRecommendationSnapshot() {
        User user = createUser();
        Post post = createPost();
        RecommendedPost recommendedPost = RecommendedPost.create(user, post, 0.91, 0.82, 3);

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

    @Test
    @DisplayName("추천 이력을 클릭 상태로 표시한다")
    void markAsisClicked_MarksHistoryAsClicked() {
        RecommendationHistory history = RecommendationHistory.fromRecommendedPost(
                RecommendedPost.create(createUser(), createPost(), 0.91, 0.82, 1)
        );

        LocalDateTime beforeClick = LocalDateTime.now();
        history.markAsisClicked();
        LocalDateTime afterClick = LocalDateTime.now();

        assertThat(history.getIsClicked()).isTrue();
        assertThat(history.getClickedAt()).isBetween(beforeClick, afterClick);
    }

    private User createUser() {
        return User.createSocialUser(
                SocialType.KAKAO,
                "recommendation-history-user",
                "history@example.com",
                "profile.jpg"
        );
    }

    private Post createPost() {
        TechBlog techBlog = TechBlog.create(
                "테스트회사",
                "https://history-test.com",
                "https://history-test.com/rss",
                "https://history-test.com/logo.png"
        );
        return Post.builder()
                .title("추천 이력 테스트 게시글")
                .fullContent("전체 내용")
                .plainContent("본문 내용")
                .company(techBlog.getCompanyName())
                .url("https://history-test.com/posts/1")
                .publishedAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .crawledAt(LocalDateTime.of(2026, 6, 1, 11, 0))
                .techBlog(techBlog)
                .build();
    }
}
