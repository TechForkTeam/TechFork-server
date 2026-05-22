package com.techfork.post.infrastructure.batch;

import com.techfork.post.domain.Post;
import com.techfork.post.infrastructure.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * summary가 null이거나 빈 문자열인 Post들을 읽어오는 Reader
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PostSummaryReader implements ItemReader<Post> {

    private final PostRepository postRepository;
    private Iterator<Post> postIterator;

    @Override
    public Post read() {
        if (postIterator == null) {
            List<Post> posts = postRepository.findWithKeywordsBySummaryIsNull();
            log.info("요약이 없거나 비어있는 게시글 {}개 발견", posts.size());
            postIterator = posts.iterator();
        }

        return postIterator.hasNext() ? postIterator.next() : null;
    }
}
