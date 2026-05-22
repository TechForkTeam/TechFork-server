package com.techfork.post.application.query.lookup;

import com.techfork.post.domain.Post;
import com.techfork.post.domain.exception.PostErrorCode;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLookupService {

    private final PostRepository postRepository;

    public Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(PostErrorCode.POST_NOT_FOUND));
    }
}
