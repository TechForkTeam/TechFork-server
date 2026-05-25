package com.techfork.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.post.domain.Post;
import com.techfork.post.domain.projection.PostDocument;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.domain.personalization.repository.PersonalizationProfileDocumentRepository;
import com.techfork.domain.search.config.GeneralSearchProperties;
import com.techfork.domain.search.dto.SearchResult;
import com.techfork.global.llm.EmbeddingClient;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchServiceImpl 단위 테스트")
class SearchServiceImplTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    @Test
    @DisplayName("일반 검색은 PostDocument projection을 후보로 사용하고 MySQL metadata를 별도로 조합한다")
    void searchGeneral_ComposesProjectionAndMetadata() throws IOException {
        GeneralSearchProperties properties = new GeneralSearchProperties();
        properties.setSearchSize(10);
        properties.setRRF_WINDOW_SIZE(10);
        SearchServiceImpl searchService = new SearchServiceImpl(
                elasticsearchClient,
                embeddingClient,
                properties,
                personalizationProfileDocumentRepository,
                postRepository,
                bookmarkRepository,
                Runnable::run,
                thumbnailOptimizer
        );

        PostDocument postDocument = postDocument(10L);
        SearchResponse<PostDocument> lexicalResponse = searchResponse(hit("10", 3.5, postDocument));
        SearchResponse<PostDocument> semanticResponse = searchResponse(hit("10", 7.0, postDocument));

        given(elasticsearchClient.search(searchRequestBuilder(), eq(PostDocument.class)))
                .willReturn(lexicalResponse, semanticResponse);
        given(embeddingClient.embed("spring batch")).willReturn(List.of(0.1f, 0.2f));
        Post metadataPost = mock(Post.class);
        given(metadataPost.getId()).willReturn(10L);
        given(metadataPost.getViewCount()).willReturn(321L);
        given(postRepository.findAllById(List.of(10L))).willReturn(List.of(metadataPost));
        given(thumbnailOptimizer.optimize("https://cdn.example.com/thumb.png"))
                .willReturn("https://cdn.example.com/thumb.optimized.png");

        List<SearchResult> results = searchService.searchGeneral("spring batch");

        assertThat(results).hasSize(1);
        SearchResult result = results.get(0);
        assertThat(result.getPostId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo("Spring Batch 정리");
        assertThat(result.getSummary()).isEqualTo("배치 처리 요약");
        assertThat(result.getShortSummary()).isEqualTo("짧은 요약");
        assertThat(result.getCompanyName()).isEqualTo("TechFork");
        assertThat(result.getThumbnailUrl()).isEqualTo("https://cdn.example.com/thumb.optimized.png");
        assertThat(result.getViewCount()).isEqualTo(321L);
        assertThat(result.getIsBookmarked()).isFalse();
        assertThat(result.getTitleVector()).isNull();
        assertThat(result.getSummaryVector()).isNull();

        verify(postRepository).findAllById(List.of(10L));
        verify(bookmarkRepository, never()).findBookmarkedPostIds(any(), any());
    }

    @SuppressWarnings("unchecked")
    private Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> searchRequestBuilder() {
        return (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any();
    }

    private SearchResponse<PostDocument> searchResponse(Hit<PostDocument> hit) {
        return SearchResponse.of(response -> response
                .took(1)
                .timedOut(false)
                .shards(shards -> shards
                        .total(1)
                        .successful(1)
                        .failed(0)
                )
                .hits(hits -> hits.hits(hit))
        );
    }

    private Hit<PostDocument> hit(String id, double score, PostDocument document) {
        return Hit.of(hit -> hit
                .id(id)
                .index("posts")
                .score(score)
                .source(document)
        );
    }

    private PostDocument postDocument(Long postId) {
        return PostDocument.builder()
                .id(String.valueOf(postId))
                .postId(postId)
                .title("Spring Batch 정리")
                .summary("배치 처리 요약")
                .shortSummary("짧은 요약")
                .company("TechFork")
                .url("https://posts.example.com/" + postId)
                .logoUrl("https://cdn.example.com/logo.png")
                .thumbnailUrl("https://cdn.example.com/thumb.png")
                .publishedAtString(LocalDateTime.of(2026, 5, 1, 10, 0).toString())
                .titleEmbedding(List.of(0.11f, 0.22f))
                .summaryEmbedding(List.of(0.33f, 0.44f))
                .contentChunks(List.of())
                .build();
    }
}
