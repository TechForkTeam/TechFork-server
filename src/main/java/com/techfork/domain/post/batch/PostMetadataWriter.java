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

        // 3단계: 기존 키워드 조회 및 새 키워드 생성
        List<Keyword> existingKeywords = keywordRepository.findAllByNameIn(uniqueKeywordNames);
        List<String> existingKeywordNames = existingKeywords.stream()
                .map(Keyword::getName)
                .toList();

        List<Keyword> newKeywords = uniqueKeywordNames.stream()
                .filter(name -> !existingKeywordNames.contains(name))
                .map(Keyword::create)
                .toList();

        // 새 키워드 일괄 저장
        if (!newKeywords.isEmpty()) {
            keywordRepository.saveAll(newKeywords);
        }

        // 4단계: 모든 키워드 맵 생성 (이름 -> Keyword 엔티티)
        List<Keyword> allKeywords = new ArrayList<>(existingKeywords);
        allKeywords.addAll(newKeywords);
        var keywordMap = allKeywords.stream()
                .collect(java.util.stream.Collectors.toMap(Keyword::getName, k -> k));

        // 5단계: PostKeyword 생성
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

        // 6단계: 메타데이터 및 PostKeyword 일괄 저장
        postMetadataRepository.saveAll(metadataList);
        postKeywordRepository.saveAll(postKeywords);

        log.info("{}개 게시글 메타데이터 저장 완료 (키워드 {}개)", chunk.size(), postKeywords.size());
    }
}
