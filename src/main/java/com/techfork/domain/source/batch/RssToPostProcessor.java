package com.techfork.domain.source.batch;

import com.techfork.post.domain.Post;
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
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class RssToPostProcessor implements ItemProcessor<RssFeedItem, Post> {

    private final TechBlogRepository techBlogRepository;

    @Override
    public Post process(RssFeedItem item) {
        TechBlog techBlog = techBlogRepository.getReferenceById(item.techBlogId());
        return Post.create(item, techBlog);
    }
}
