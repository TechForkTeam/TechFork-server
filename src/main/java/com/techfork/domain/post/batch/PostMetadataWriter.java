package com.techfork.domain.post.batch;

import com.techfork.domain.post.dto.ExtractedMetadata;
import com.techfork.domain.post.dto.PostWithMetadata;
import com.techfork.domain.post.entity.Keyword;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.entity.PostMetadata;
import com.techfork.domain.post.repository.KeywordRepository;
import com.techfork.domain.post.repository.PostKeywordRepository;
import com.techfork.domain.post.repository.PostMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 추출된 메타데이터를 저장하는 Writer
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PostMetadataWriter implements ItemWriter<PostWithMetadata> {

    private final PostMetadataRepository postMetadataRepository;
    private final KeywordRepository keywordRepository;
    private final PostKeywordRepository postKeywordRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends PostWithMetadata> chunk) {
        if (chunk.isEmpty()) {
            return;
        }

        List<PostMetadata> metadataList = new ArrayList<>();
        List<PostKeyword> postKeywords = new ArrayList<>();

        for (PostWithMetadata item : chunk.getItems()) {
            Post post = item.post();
            ExtractedMetadata metadata = item.metadata();

            log.debug("메타데이터 저장 - Post ID: {}, 난이도: {}, 주제: {}, 언어: {}, 프레임워크: {}, 도구: {}",
                    post.getId(),
                    metadata.getDifficultyLevel(),
                    metadata.getMainTopicsAsString(),
                    metadata.getLanguagesAsString(),
                    metadata.getFrameworksAsString(),
                    metadata.getToolsAsString());

            PostMetadata postMetadata = PostMetadata.create(
                    post,
                    metadata.getDifficultyLevel(),
                    metadata.getLanguagesAsString(),
                    metadata.getFrameworksAsString(),
                    metadata.getToolsAsString(),
                    metadata.getMainTopicsAsString()
            );
            metadataList.add(postMetadata);

            // techStack의 모든 항목을 키워드로 저장
            if (metadata.techStack() != null) {
                // languages를 키워드로 추가
                if (metadata.techStack().languages() != null) {
                    for (String language : metadata.techStack().languages()) {
                        addKeyword(post, language, postKeywords);
                    }
                }

                // frameworks를 키워드로 추가
                if (metadata.techStack().frameworks() != null) {
                    for (String framework : metadata.techStack().frameworks()) {
                        addKeyword(post, framework, postKeywords);
                    }
                }

                // tools를 키워드로 추가
                if (metadata.techStack().tools() != null) {
                    for (String tool : metadata.techStack().tools()) {
                        addKeyword(post, tool, postKeywords);
                    }
                }
            }
        }

        postMetadataRepository.saveAll(metadataList);
        postKeywordRepository.saveAll(postKeywords);

        log.info("{}개 게시글 메타데이터 저장 완료 (키워드 {}개)", chunk.size(), postKeywords.size());
    }

    /**
     * 키워드를 추가하는 헬퍼 메서드
     */
    private void addKeyword(Post post, String keywordName, List<PostKeyword> postKeywords) {
        if (keywordName == null || keywordName.isBlank()) {
            return;
        }

        Keyword keyword = keywordRepository.findByName(keywordName)
                .orElseGet(() -> {
                    Keyword newKeyword = Keyword.create(keywordName);
                    return keywordRepository.save(newKeyword);
                });

        postKeywords.add(PostKeyword.create(post, keyword));
    }
}
