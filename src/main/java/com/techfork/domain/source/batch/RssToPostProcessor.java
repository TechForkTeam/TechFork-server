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
 * Source 컨텍스트의 현재 monolith 내부 handoff DTO({@link RssFeedItem})를
 * Post aggregate 생성 경계로 전달하는 processor.
 *
 * <p>현재 phase 에서는 Source → Post 동기 handoff 를 유지하며,
 * 별도 published language / 이벤트 handoff 전환은 후속 작업으로 남긴다.</p>
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
