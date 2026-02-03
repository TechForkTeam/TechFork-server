package com.techfork.domain.recommendation.setup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostDocumentRepository;
import com.techfork.domain.post.repository.PostRepository;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final String OUTPUT_DIR = "src/test/resources/fixtures/evaluation";
    private static final int POST_EXPORT_COUNT = 1200; // 전체 데이터 사용 (약 1100개)

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    @DisplayName("원격 DB에서 게시글 데이터 Export")
    @Transactional
    public void exportPostData() throws IOException {
        log.info("===== 게시글 데이터 Export 시작 =====");
        log.info("원격 DB: MySQL + Elasticsearch");

        // 출력 디렉토리 생성
        Path outputPath = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outputPath);
        log.info("출력 디렉토리: {}", outputPath.toAbsolutePath());

        // 1. 최신 게시글 export (MySQL)
        List<Post> posts = exportPosts();
        log.info("✓ 게시글 {} 개 export 완료", posts.size());

        // 2. PostDocument export (Elasticsearch, 임베딩 포함)
        Set<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toSet());
        List<PostDocument> postDocuments = exportPostDocuments(postIds);
        log.info("✓ PostDocument {} 개 export 완료 (임베딩 포함)", postDocuments.size());

        log.info("===== 게시글 데이터 Export 완료 =====");
        log.info("출력 위치: {}", outputPath.toAbsolutePath());
        log.info("\n생성된 파일:");
        log.info("  - posts.json ({} 개)", posts.size());
        log.info("  - post-documents.json ({} 개, titleEmbedding + summaryEmbedding 포함)", postDocuments.size());
    }

    private List<Post> exportPosts() throws IOException {
        // 최신 게시글 조회
        List<Post> posts = postRepository.findAll(
                PageRequest.of(0, POST_EXPORT_COUNT, Sort.by("publishedAt").descending())
        ).getContent();

        // DTO 변환 (순환 참조 방지)
        List<Map<String, Object>> postDtos = posts.stream()
                .map(this::convertPostToDto)
                .toList();

        writeJsonFile("posts.json", postDtos);
        return posts;
    }

    private List<PostDocument> exportPostDocuments(Set<Long> postIds) throws IOException {
        List<PostDocument> documents = new ArrayList<>();
        int notFoundCount = 0;

        for (Long postId : postIds) {
            Optional<PostDocument> docOpt = postDocumentRepository.findByPostId(postId);
            if (docOpt.isPresent()) {
                documents.add(docOpt.get());
            } else {
                notFoundCount++;
                log.warn("PostDocument not found for postId: {}", postId);
            }
        }

        if (notFoundCount > 0) {
            log.warn("총 {} 개 게시글의 PostDocument를 찾지 못했습니다.", notFoundCount);
        }

        // PostDocument는 이미 JSON 직렬화 가능하므로 그대로 저장
        writeJsonFile("post-documents.json", documents);

        // 임베딩 차원 검증
        if (!documents.isEmpty()) {
            PostDocument sample = documents.get(0);
            log.info("임베딩 차원 검증:");
            log.info("  - titleEmbedding: {} 차원",
                    sample.getTitleEmbedding() != null ? sample.getTitleEmbedding().size() : "null");
            log.info("  - summaryEmbedding: {} 차원",
                    sample.getSummaryEmbedding() != null ? sample.getSummaryEmbedding().size() : "null");

            // ContentChunk 검증
            if (sample.getContentChunks() != null && !sample.getContentChunks().isEmpty()) {
                log.info("  - contentChunks: {} 개",
                        sample.getContentChunks().size());
                log.info("  - chunk embedding: {} 차원",
                        sample.getContentChunks().get(0).getEmbedding() != null ?
                                sample.getContentChunks().get(0).getEmbedding().size() : "null");
            } else {
                log.info("  - contentChunks: 없음");
            }
        }

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

        // TechBlog 정보
        dto.put("techBlogId", post.getTechBlog().getId());
        dto.put("techBlogCompanyName", post.getTechBlog().getCompanyName());
        dto.put("techBlogUrl", post.getTechBlog().getBlogUrl());
        dto.put("techBlogRssUrl", post.getTechBlog().getRssUrl());

        return dto;
    }

    private void writeJsonFile(String filename, Object data) throws IOException {
        File outputFile = new File(OUTPUT_DIR, filename);
        objectMapper.writeValue(outputFile, data);
        log.debug("파일 작성: {}", outputFile.getAbsolutePath());
    }
}