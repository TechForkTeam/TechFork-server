package com.techfork.domain.source.batch;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * RssFeedItem을 Post 엔티티로 변환하는 Processor
 * 중복 체크도 여기서 수행하여 이미 존재하는 URL은 null 반환
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class RssToPostProcessor implements ItemProcessor<RssFeedItem, Post> {

    private final PostRepository postRepository;
    private final TechBlogRepository techBlogRepository;

    @Override
    public Post process(RssFeedItem item) {
        if (postRepository.existsByUrl(item.url())) {
            return null; // null 반환 시 Writer에서 처리 안 함
        }

        TechBlog techBlog = techBlogRepository.getReferenceById(item.techBlogId());
        return Post.create(item, techBlog);
    }
}
