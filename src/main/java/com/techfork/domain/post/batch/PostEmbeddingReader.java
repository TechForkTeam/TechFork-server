package com.techfork.domain.post.batch;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * 요약이 완료되고 임베딩이 필요한 Post를 읽어오는 Reader
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PostEmbeddingReader implements ItemStreamReader<Post> {

    private final PostRepository postRepository;
    private Iterator<Post> postIterator;
    private boolean initialized;

    @Override
    public Post read() {
        if (!initialized) {
            List<Post> posts = postRepository.findReadyForEmbedding();
            log.info("임베딩 대상 Post 개수: {}", posts.size());
            postIterator = posts.iterator();
            initialized = true;
        }

        return postIterator.hasNext() ? postIterator.next() : null;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        resetState();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // no-op
    }

    @Override
    public void close() throws ItemStreamException {
        resetState();
    }

    private void resetState() {
        postIterator = null;
        initialized = false;
    }
}
