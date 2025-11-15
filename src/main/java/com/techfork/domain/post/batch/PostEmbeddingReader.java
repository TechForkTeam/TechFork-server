package com.techfork.domain.post.batch;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/**
 * 요약이 완료되고 임베딩이 필요한 Post를 읽어오는 Reader
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostEmbeddingReader implements ItemReader<Post> {

    private final PostRepository postRepository;
    private Iterator<Post> postIterator;

    @Override
    public Post read() {
        if(postIterator == null) {
            Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "id"));
            Page<Post> posts = postRepository.findBySummaryIsNotNullAndEmbeddedAtIsNull(pageable);
            log.info("임베딩 대상 Post 개수: {}", posts.getContent().size());
            postIterator = posts.iterator();
        }

        return postIterator.hasNext() ? postIterator.next() : null;
    }
}
