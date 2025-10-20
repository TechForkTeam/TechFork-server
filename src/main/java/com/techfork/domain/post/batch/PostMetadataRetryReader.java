package com.techfork.domain.post.batch;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.entity.PostMetadata;
import com.techfork.domain.post.repository.PostMetadataRepository;
import com.techfork.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 메타데이터가 비어있는 게시글을 재처리하기 위한 Reader
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PostMetadataRetryReader implements ItemReader<Post> {

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
        // 1. 메타데이터는 있지만 내용이 비어있는 Post들 조회
        List<PostMetadata> emptyMetadata = postMetadataRepository.findEmptyMetadata();
        List<Post> postsWithEmptyMetadata = emptyMetadata.stream()
                .map(PostMetadata::getPost)
                .toList();

        // 2. 메타데이터가 없는 Post들 조회 (효율적인 쿼리)
        List<Post> postsWithoutMetadata = postMetadataRepository.findPostsWithoutMetadata();

        // 3. 두 리스트 합치기 (중복 제거)
        List<Post> allPosts = java.util.stream.Stream.concat(
                postsWithoutMetadata.stream(),
                postsWithEmptyMetadata.stream()
        ).distinct().toList();

        this.postIterator = allPosts.iterator();
        log.info("메타데이터 재처리 대상: {}개 게시글 (메타데이터 없음: {}, 비어있음: {})",
                allPosts.size(), postsWithoutMetadata.size(), postsWithEmptyMetadata.size());
    }
}
