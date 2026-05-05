package com.techfork.domain.activity.readpost.service;

import com.techfork.domain.activity.readpost.dto.ReadPostRequest;
import com.techfork.domain.activity.readpost.entity.ReadPost;
import com.techfork.domain.activity.readpost.repository.ReadPostRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.exception.PostErrorCode;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReadPostCommandService {

    private final ReadPostRepository readPostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public void saveReadPost(Long userId, ReadPostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new GeneralException(PostErrorCode.POST_NOT_FOUND));

        boolean isFirstRead = !readPostRepository.existsByUserAndPost(user, post);
        if (isFirstRead) {
            post.incrementViewCount();
        }

        ReadPost readPost = ReadPost.create(
                user,
                post,
                request.readAt(),
                request.readDurationSeconds()
        );

        readPostRepository.save(readPost);
        log.info("Saved read post for user {} and post {} (viewCount incremented: {})",
                userId, request.postId(), isFirstRead);
    }
}
