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

            PostMetadata postMetadata = PostMetadata.create(
                    post,
                    metadata.getDifficultyLevel(),
                    metadata.getLanguagesAsString(),
                    metadata.getFrameworksAsString(),
                    metadata.getToolsAsString(),
                    metadata.getMainTopicsAsString()
            );
            metadataList.add(postMetadata);

            if (metadata.mainTopics() != null) {
                for (String topicName : metadata.mainTopics()) {
                    if (topicName == null || topicName.isBlank()) {
                        continue;
                    }

                    Keyword keyword = keywordRepository.findByName(topicName)
                            .orElseGet(() -> {
                                Keyword newKeyword = Keyword.create(topicName);
                                return keywordRepository.save(newKeyword);
                            });

                    postKeywords.add(PostKeyword.create(post, keyword));
                }
            }
        }

        postMetadataRepository.saveAll(metadataList);
        postKeywordRepository.saveAll(postKeywords);

        log.info("{}개 게시글 메타데이터 저장 완료", chunk.size());
    }
}
