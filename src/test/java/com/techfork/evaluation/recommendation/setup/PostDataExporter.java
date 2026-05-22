package com.techfork.evaluation.recommendation.setup;

import com.techfork.post.domain.projection.PostDocument;
import com.techfork.post.domain.Post;
import com.techfork.post.infrastructure.PostDocumentRepository;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.evaluation.recommendation.setup.components.FileExporter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 원격 DB에서 게시글 데이터만 JSON으로 export
 */
@Tag("evaluation-setup")
@Disabled("수동 실행용 - CI 제외")
@Slf4j
@SpringBootTest
@ActiveProfiles("local-tunnel")
class PostDataExporter {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostDocumentRepository postDocumentRepository;

    @Autowired
    private FileExporter fileExporter;

    private static final int POST_EXPORT_COUNT = 1200; // 전체 데이터 사용 (약 1100개)

    @Test
    @DisplayName("원격 DB에서 게시글 데이터 Export")
    @Transactional
    public void exportPostData() throws IOException {
        log.info("===== 게시글 데이터 Export 시작 =====");
        log.info("원격 DB: MySQL + Elasticsearch");

        fileExporter.ensureOutputDirectory();

        List<Post> posts = exportPosts();
        log.info("✓ 게시글 {} 개 export 완료", posts.size());

        Set<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toSet());
        List<PostDocument> postDocuments = exportPostDocuments(postIds);
        log.info("✓ PostDocument {} 개 export 완료 (임베딩 포함)", postDocuments.size());

        log.info("===== 게시글 데이터 Export 완료 =====");
        log.info("출력 위치: {}", fileExporter.getOutputDir());
        log.info("\n생성된 파일:");
        log.info("  - posts.json ({} 개)", posts.size());
        log.info("  - post-documents.json ({} 개, titleEmbedding + summaryEmbedding 포함)", postDocuments.size());
    }

    private List<Post> exportPosts() throws IOException {
        List<Post> posts = postRepository.findAll(
                PageRequest.of(0, POST_EXPORT_COUNT, Sort.by("publishedAt").descending())
        ).getContent();

        List<Map<String, Object>> postDtos = posts.stream()
                .map(this::convertPostToDto)
                .toList();

        fileExporter.writeJsonFile("posts.json", postDtos);
        return posts;
    }

    private List<PostDocument> exportPostDocuments(Set<Long> postIds) throws IOException {
        List<PostDocument> documents = new ArrayList<>();

        for (Long postId : postIds) {
            Optional<PostDocument> docOpt = postDocumentRepository.findByPostId(postId);
            if (docOpt.isPresent()) {
                documents.add(docOpt.get());
            } else {
                log.warn("PostDocument not found for postId: {}", postId);
            }
        }

        fileExporter.writeJsonFile("post-documents.json", documents);

        return documents;
    }

    private Map<String, Object> convertPostToDto(Post post) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", post.getId());
        dto.put("title", post.getTitle());
        dto.put("url", post.getUrl());
        dto.put("summary", post.getSummary());
        dto.put("shortSummary", post.getShortSummary());
        dto.put("company", post.getCompany());
        dto.put("logoUrl", post.getLogoUrl());
        dto.put("thumbnailUrl", post.getThumbnailUrl());
        dto.put("publishedAt", post.getPublishedAt() != null ? post.getPublishedAt().toString() : null);
        dto.put("viewCount", post.getViewCount());

        dto.put("techBlogId", post.getTechBlog().getId());
        dto.put("techBlogCompanyName", post.getTechBlog().getCompanyName());
        dto.put("techBlogUrl", post.getTechBlog().getBlogUrl());
        dto.put("techBlogRssUrl", post.getTechBlog().getRssUrl());

        return dto;
    }
}
