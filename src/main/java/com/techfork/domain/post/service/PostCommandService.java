package com.techfork.domain.post.service;

import com.techfork.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCommandService {

    private final PostRepository postRepository;

    public void incrementViewCount(Long postId) {
        postRepository.incrementViewCount(postId);
    }
}
