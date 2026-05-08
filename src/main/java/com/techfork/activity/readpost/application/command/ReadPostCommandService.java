package com.techfork.activity.readpost.application.command;

import com.techfork.activity.readpost.domain.ReadPost;
import com.techfork.activity.readpost.domain.ReadPostFirstReadPolicy;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.service.PostCommandService;
import com.techfork.domain.post.service.PostLookupService;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.service.UserLookupService;
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
    private final PostLookupService postLookupService;
    private final PostCommandService postCommandService;
    private final UserLookupService userLookupService;
    private final ReadPostFirstReadPolicy readPostFirstReadPolicy;

    public void saveReadPost(SaveReadPostCommand command) {
        User user = userLookupService.getUserOrThrow(command.userId());
        Post post = postLookupService.getPostOrThrow(command.postId());

        boolean firstReadMarked = readPostFirstReadPolicy.markFirstRead(user, post, command.readAt());
        boolean viewCountIncremented = false;
        if (firstReadMarked) {
            viewCountIncremented = postCommandService.incrementViewCount(post.getId());
        }

        ReadPost readPost = ReadPost.create(
                user,
                post,
                command.readAt(),
                command.readDurationSeconds()
        );

        readPostRepository.save(readPost);
        log.info("Saved read post for user {} and post {} (firstReadMarked: {}, viewCount incremented: {})",
                command.userId(), command.postId(), firstReadMarked, viewCountIncremented);
    }
}
