package com.techfork.domain.source.batch;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Post를 배치로 저장하는 Writer
 * Processor에서 중복 체크가 완료되므로 여기서는 단순 저장만 수행
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PostBatchWriter implements ItemWriter<Post> {

    private final PostRepository postRepository;

    @Override
    public void write(Chunk<? extends Post> chunk) {
        if (chunk.isEmpty()) {
            return;
        }

        postRepository.saveAll(chunk.getItems());
        log.info("{}개 게시글 저장 완료", chunk.size());
    }
}
