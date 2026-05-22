package com.techfork.domain.source.batch;

import com.techfork.post.domain.Post;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RssToPostProcessorTest {

    @Mock
    private TechBlogRepository techBlogRepository;

    private final RssFeedItem rssFeedItem = RssFeedItem.builder()
            .title("테스트 제목")
            .url("https://posts.example.com/1")
            .logoUrl("https://cdn.example.com/logo.png")
            .thumbnailUrl("https://cdn.example.com/thumb.png")
            .content("긴 본문")
            .plainContent("평문 본문")
            .publishedAt(LocalDateTime.of(2026, 4, 13, 5, 45, 0))
            .company("TechFork")
            .techBlogId(7L)
            .build();

    @Nested
    @DisplayName("process")
    class Process {

        @Test
        @DisplayName("tech blog 참조를 조회한 뒤 Post 엔티티로 변환한다")
        void loadsTechBlogReferenceAndCreatesPost() {
            RssToPostProcessor rssToPostProcessor = new RssToPostProcessor(techBlogRepository);
            TechBlog techBlog = TechBlog.create(
                    "TechFork",
                    "https://techfork.example.com",
                    "https://techfork.example.com/rss",
                    "https://cdn.example.com/logo.png"
            );
            LocalDateTime before = LocalDateTime.now();
            given(techBlogRepository.getReferenceById(7L)).willReturn(techBlog);

            Post post = rssToPostProcessor.process(rssFeedItem);

            LocalDateTime after = LocalDateTime.now();
            verify(techBlogRepository).getReferenceById(7L);
            assertThat(post.getTitle()).isEqualTo(rssFeedItem.title());
            assertThat(post.getFullContent()).isEqualTo(rssFeedItem.content());
            assertThat(post.getPlainContent()).isEqualTo(rssFeedItem.plainContent());
            assertThat(post.getCompany()).isEqualTo(rssFeedItem.company());
            assertThat(post.getUrl()).isEqualTo(rssFeedItem.url());
            assertThat(post.getLogoUrl()).isEqualTo(rssFeedItem.logoUrl());
            assertThat(post.getThumbnailUrl()).isEqualTo(rssFeedItem.thumbnailUrl());
            assertThat(post.getPublishedAt()).isEqualTo(rssFeedItem.publishedAt());
            assertThat(post.getTechBlog()).isSameAs(techBlog);
            assertThat(post.getCrawledAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("tech blog 조회 실패를 그대로 전파한다")
        void propagatesRepositoryFailure() {
            RssToPostProcessor rssToPostProcessor = new RssToPostProcessor(techBlogRepository);
            given(techBlogRepository.getReferenceById(7L))
                    .willThrow(new EntityNotFoundException("tech blog not found"));

            assertThatThrownBy(() -> rssToPostProcessor.process(rssFeedItem))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("tech blog not found");
        }
    }
}
