package com.techfork.domain.recommendation.converter;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.recommendation.dto.RecommendedPostDto;
import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.enums.SocialType;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationConverterTest {

    @Test
    @DisplayName("추천 DTO 생성 시 썸네일 URL에 Cloudflare 최적화를 적용한다")
    void toRecommendedPostDto_OptimizesThumbnailUrl() {
        CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer = mock(CloudflareThirdPartyThumbnailOptimizer.class);
        RecommendationConverter converter = new RecommendationConverter(thumbnailOptimizer);

        TechBlog techBlog = TechBlog.create(
                "테스트 회사",
                "https://techfork.com",
                "https://techfork.com/rss",
                "https://techfork.com/logo.png"
        );

        Post post = Post.builder()
                .title("게시글")
                .shortSummary("요약")
                .company("테스트 회사")
                .url("https://techfork.com/posts/1")
                .thumbnailUrl("https://images.example.com/thumb.jpg")
                .publishedAt(LocalDateTime.now())
                .crawledAt(LocalDateTime.now())
                .techBlog(techBlog)
                .build();

        User user = User.createSocialUser(
                SocialType.KAKAO,
                "social-id",
                "test@example.com",
                "https://example.com/profile.png"
        );

        RecommendedPost recommendedPost = RecommendedPost.create(user, post, 0.9, 0.8, 1);

        when(thumbnailOptimizer.optimize("https://images.example.com/thumb.jpg"))
                .thenReturn("https://api.techfork.com/cdn-cgi/image/fit=scale-down,width=480,quality=75,format=auto/https://images.example.com/thumb.jpg");

        RecommendedPostDto result = converter.toRecommendedPostDto(recommendedPost);

        assertThat(result.thumbnailUrl())
                .isEqualTo("https://api.techfork.com/cdn-cgi/image/fit=scale-down,width=480,quality=75,format=auto/https://images.example.com/thumb.jpg");
    }
}
