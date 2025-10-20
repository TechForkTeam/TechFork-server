package com.techfork.domain.post.batch;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostMetadataRepository;
import com.techfork.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PostMetadataReader implements ItemReader<Post> {

    private final PostRepository postRepository;
    private final PostMetadataRepository postMetadataRepository;

    private Iterator<Post> postIterator;

    @Override
    public Post read() {
        if (postIterator == null) {
            initializePostIterator();
        }

        if (postIterator.hasNext()) {
            return postIterator.next();
        }

        return null; // 모든 Post 처리 완료
    }

    private void initializePostIterator() {
        // 메타데이터가 없는 Post들만 조회
        List<Post> postsWithoutMetadata = postRepository.findAll().stream()
                .filter(post -> !postMetadataRepository.existsByPostId(post.getId()))
                .toList();

        this.postIterator = postsWithoutMetadata.iterator();
        log.info("메타데이터 추출 대상: {}개 게시글", postsWithoutMetadata.size());
    }
}
