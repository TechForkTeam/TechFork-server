package com.techfork.activity.readpost.application.command;

import com.techfork.activity.readpost.domain.ReadPost;
import com.techfork.activity.readpost.domain.ReadPostFirstReadPolicy;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
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
    private final ReadPostFirstReadPolicy readPostFirstReadPolicy;

    public void saveReadPost(SaveReadPostCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(command.postId())
                .orElseThrow(() -> new GeneralException(PostErrorCode.POST_NOT_FOUND));

        boolean isFirstRead = readPostFirstReadPolicy.isFirstRead(user, post);
        if (isFirstRead) {
            post.incrementViewCount();
        }

        ReadPost readPost = ReadPost.create(
                user,
                post,
                command.readAt(),
                command.readDurationSeconds()
        );

        readPostRepository.save(readPost);
        log.info("Saved read post for user {} and post {} (viewCount incremented: {})",
                command.userId(), command.postId(), isFirstRead);
    }
}
