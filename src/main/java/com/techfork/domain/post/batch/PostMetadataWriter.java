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
import java.util.Map;

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
        List<String> allKeywordNames = new ArrayList<>();
        List<PostKeyword> postKeywords = new ArrayList<>();

        // 1단계: 모든 키워드 이름 수집
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

            // techStack의 모든 항목을 키워드로 수집
            if (metadata.techStack() != null) {
                if (metadata.techStack().languages() != null) {
                    allKeywordNames.addAll(metadata.techStack().languages());
                }
                if (metadata.techStack().frameworks() != null) {
                    allKeywordNames.addAll(metadata.techStack().frameworks());
                }
                if (metadata.techStack().tools() != null) {
                    allKeywordNames.addAll(metadata.techStack().tools());
                }
            }
        }

        // 2단계: 중복 제거 및 빈 값 제거
        List<String> uniqueKeywordNames = allKeywordNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();

        // 3단계: 키워드를 하나씩 조회 또는 생성 (동시성 안전)
        Map<String, Keyword> keywordMap = new java.util.HashMap<>();

        for (String keywordName : uniqueKeywordNames) {
            Keyword keyword = getOrCreateKeyword(keywordName);
            keywordMap.put(keywordName, keyword);
        }

        // 4단계: PostKeyword 생성
        for (PostWithMetadata item : chunk.getItems()) {
            Post post = item.post();
            ExtractedMetadata metadata = item.metadata();

            if (metadata.techStack() != null) {
                List<String> techStackNames = new ArrayList<>();
                if (metadata.techStack().languages() != null) {
                    techStackNames.addAll(metadata.techStack().languages());
                }
                if (metadata.techStack().frameworks() != null) {
                    techStackNames.addAll(metadata.techStack().frameworks());
                }
                if (metadata.techStack().tools() != null) {
                    techStackNames.addAll(metadata.techStack().tools());
                }

                for (String keywordName : techStackNames) {
                    if (keywordName != null && !keywordName.isBlank()) {
                        Keyword keyword = keywordMap.get(keywordName);
                        if (keyword != null) {
                            postKeywords.add(PostKeyword.create(post, keyword));
                        }
                    }
                }
            }
        }

        // 5단계: 메타데이터 및 PostKeyword 일괄 저장
        postMetadataRepository.saveAll(metadataList);
        postKeywordRepository.saveAll(postKeywords);

        log.info("{}개 게시글 메타데이터 저장 완료 (키워드 {}개)", chunk.size(), postKeywords.size());
    }

    /**
     * 키워드 조회 또는 생성 (동시성 안전)
     * - 먼저 조회 시도
     * - 없으면 생성 시도
     * - Duplicate 에러 발생 시 재조회
     */
    private Keyword getOrCreateKeyword(String keywordName) {
        return keywordRepository.findByName(keywordName)
                .orElseGet(() -> {
                    try {
                        return keywordRepository.save(Keyword.create(keywordName));
                    } catch (Exception e) {
                        // 동시 저장으로 인한 Duplicate 에러 → 재조회
                        log.debug("키워드 동시 저장 감지, 재조회: {}", keywordName);
                        return keywordRepository.findByName(keywordName)
                                .orElseThrow(() -> new RuntimeException("키워드 저장/조회 실패: " + keywordName));
                    }
                });
    }
}
